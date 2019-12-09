package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.UsageContract
import com.marketdata.schema.PermissionSchemaV1.PersistentPermission
import com.marketdata.states.*
import net.corda.confidential.IdentitySyncFlow
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UsageIssue(val dataSet: String, val provider: Party, val redistributor: Party, val userName : String, val receipt : UsageReceiptState? = null) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{

        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = UsageContract.Commands.Issue()

        val results = builder {
            val providerIdx = PersistentPermission::providerName.equal(provider.toString())
            val dataSetIdx  = PersistentPermission::dataSetName.equal(dataSet)
            val subscriberIdx = PersistentPermission::subscriberName.equal(ourIdentity.toString())
            val redistributorIdx = PersistentPermission::redistributorName.equal(redistributor.toString())

            val customCriteria1 = VaultCustomQueryCriteria(providerIdx)
            val customCriteria2 = VaultCustomQueryCriteria(dataSetIdx)
            val customCriteria3 = VaultCustomQueryCriteria(subscriberIdx)
            val customCriteria4 = VaultCustomQueryCriteria(redistributorIdx)

            val criteria = VaultQueryCriteria(Vault.StateStatus.ALL)
                    .and(customCriteria1
                            .and(customCriteria2
                                    .and(customCriteria3
                                            .and(customCriteria4))))
            serviceHub.vaultService.queryBy(PermissionRequestState::class.java,criteria)
        }
        val permStates = results.states
        when {
            permStates.isEmpty() -> {
                throw IllegalArgumentException("No permission to access data set ($dataSet) from provider ($provider)")
            }
            permStates.size > 1 -> {
                throw IllegalArgumentException("Error determining data set ($dataSet) from provider ($provider)")
            }
        }

        val permState = permStates.single().state.data

        val receiptPointer = if( receipt != null ) {
            LinearPointer(receipt.linearId, UsageReceiptState::class.java)
        } else {
            null
        }

        val state = UsageState(
                dataSet,
                LinearPointer(permState.linearId, PermissionRequestState::class.java),
                receiptPointer,
                provider,
                redistributor,
                ourIdentity,
                userName)

        txb.withItems(
                StateAndContract(state, UsageContract.ID ),
                Command(cmd, ourIdentity.owningKey)
                )

        txb.verify(serviceHub)
        val partialTx = serviceHub.signInitialTransaction(txb)

        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }

        return subFlow(FinalityFlow(partialTx, sessions))
    }
}

@InitiatedBy(UsageIssue::class)
class UsageIssueResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterpartySession))

        val usageState = stx.tx.outputsOfType(UsageState::class.java).single()
        if (ourIdentity == usageState.redistributor) {
            subFlow(UsageReceipt(usageState))
        }
    }
}

@InitiatingFlow
class UsageReceipt(val usageState: UsageState) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{

        // The only party who can issue the receipt is the redistributor. They do not want to be identified by other
        // parties who may see this receipt, so use a new identity.

        val permState = usageState.permissionState.resolve(serviceHub).state.data
        val distDataSet = permState.distributableDataSet.resolve(serviceHub).state.data
        val dataSet = distDataSet.dataSet.resolve(serviceHub).state.data
        val signedTandCs = permState.providerTandCs.resolve(serviceHub).state.data


        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = UsageContract.Commands.SendReceipt()

        // This isn't ideal, but we don't want to create two anonymous identities, so just
        // create one then share it with the other. We don't really need to get the remote parties
        // to issue anonymous identites so this can be optimised for sure.
        val txIdentities = subFlow(SwapIdentityHelper( usageState.subscriber ))
        val anonymousMe = txIdentities[ourIdentity]!!

        val recieptState = UsageReceiptState(
                LinearPointer(dataSet.linearId, DataSetState::class.java),
                LinearPointer(signedTandCs.linearId, SignedTermsAndConditionsState::class.java),
                usageState.dataSetName,
                usageState.provider,
                anonymousMe,
                usageState.subscriber,
                usageState.userName,
                usageState.date
        )

        txb.withItems(
                StateAndContract(recieptState, UsageContract.ID ),
                Command(cmd, recieptState.participants.map { it.owningKey })
        )

        txb.verify(serviceHub)
        val partialTx = serviceHub.signInitialTransaction(txb, anonymousMe.owningKey)

        val subscriberSession = initiateFlow(usageState.subscriber)
        val providerSession = initiateFlow(usageState.provider)

        // Sync up confidential identities in the transaction with the provider
        subFlow(IdentitySyncHelper(usageState.provider, partialTx.tx))

        val sessions = listOf(subscriberSession, providerSession)

        val signedTx = subFlow(CollectSignaturesFlow(partialTx, sessions, listOf(anonymousMe.owningKey)))
        return subFlow(FinalityFlow(signedTx, sessions))
    }
}

@InitiatedBy(UsageReceipt::class)
class UsageReceiptResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {

                // TODO - check
            }
        }
        val expectedTxId = subFlow(signedTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
    }
}

@InitiatingFlow
class SwapIdentityHelper(private val party: Party ) : FlowLogic<LinkedHashMap<Party, AnonymousParty>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : LinkedHashMap<Party, AnonymousParty> {
        return subFlow(SwapIdentitiesFlow(initiateFlow(party)))
    }
}

@InitiatedBy(SwapIdentityHelper::class)
class SwapIdentityHelperResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(SwapIdentitiesFlow(counterpartySession))
    }
}

@InitiatingFlow
class IdentitySyncHelper(private val party: Party , private val tx : WireTransaction) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(){
        subFlow(IdentitySyncFlow.Send(initiateFlow(party), tx))
    }
}

@InitiatedBy(IdentitySyncHelper::class)
class IdentitySyncHelperResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(IdentitySyncFlow.Receive(counterpartySession))
    }
}
