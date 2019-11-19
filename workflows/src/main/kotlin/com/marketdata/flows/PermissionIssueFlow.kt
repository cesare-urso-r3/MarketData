package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.PermissionContract
import com.marketdata.schema.DataSetSchemaV1
import com.marketdata.schema.PermissionSchemaV1
import com.marketdata.states.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.spi.CalendarDataProvider

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class PermissionIssueInitiator(val dataSet: String, val provider: Party, val dataChargeOwner : Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{

        val dataSetResults = builder {
            val dataSetIdx = DataSetSchemaV1.PersistentDataSet::name.equal(dataSet)
            val providerIdx = DataSetSchemaV1.PersistentDataSet::providerName.equal(provider.toString())

            val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)
            val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(dataSetIdx)

            val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
                    .and(customCriteria1
                            .and(customCriteria2))

            serviceHub.vaultService.queryBy(DataSetState::class.java,criteria)
        }

        val dataSetStates = dataSetResults.states
        when {
            dataSetStates.isEmpty() -> {
                throw IllegalArgumentException("No data set exists for this combination of dataSet/provider")
            }
        }
        val dataSetState = dataSetStates.single().state.data

        val results = builder {
            val providerIdx = PermissionSchemaV1.PersistentPermission::providerName.equal(provider.toString())
            val dataSetIdx  = PermissionSchemaV1.PersistentPermission::dataSetName.equal(dataSet)
            val subscriberIdx = PermissionSchemaV1.PersistentPermission::subscriberName.equal(ourIdentity.toString())
            val dataChargeOwnerIdx = PermissionSchemaV1.PersistentPermission::dataChargeOwnerName.equal(dataChargeOwner.toString())

            val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)
            val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(dataSetIdx)
            val customCriteria3 = QueryCriteria.VaultCustomQueryCriteria(subscriberIdx)
            val customCriteria4 = QueryCriteria.VaultCustomQueryCriteria(dataChargeOwnerIdx)

            val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
                    .and(customCriteria1
                            .and(customCriteria2
                                    .and(customCriteria3
                                            .and(customCriteria4))))
            serviceHub.vaultService.queryBy(PermissionState::class.java,criteria)
        }

        val permStates = results.states
        when {
            permStates.isNotEmpty() -> {
                throw IllegalArgumentException("Permission already exists for this combination of dataSet/provider/subscriber/dataChargeOwner")
            }
        }

        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = PermissionContract.Commands.Issue()
        val state = PermissionState(dataSet, LinearPointer(dataSetState.linearId, DataSetState::class.java), provider, ourIdentity, dataChargeOwner)

        txb.withItems(
                StateAndContract(state, PermissionContract.ID ),
                Command(cmd, state.participants.map { it.owningKey })
        )

        txb.verify(serviceHub)
        val partialTx = serviceHub.signInitialTransaction(txb)

        val sessions = mutableListOf<FlowSession>()
        state.participants.filter { it != ourIdentity }.forEach { p -> sessions.add(initiateFlow(p)) }

        val signedTx = subFlow(CollectSignaturesFlow(partialTx, sessions))

        return subFlow(FinalityFlow(signedTx, sessions));
    }
}

@InitiatedBy(PermissionIssueInitiator::class)
class PermissionIssueResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a permission transaction" using (output is PermissionState)

                // TODO: check that we are a provider/ redistrubutor of a data set,
                // and that we are also are happy to grant permission
            }
        }
        val expectedTxId = subFlow(signedTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
    }
}
