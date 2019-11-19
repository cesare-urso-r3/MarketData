package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.PermissionContract
import com.marketdata.contracts.UsageContract
import com.marketdata.schema.DataSetSchemaV1
import com.marketdata.schema.PermissionSchemaV1
import com.marketdata.schema.PermissionSchemaV1.PersistentPermission
import com.marketdata.schema.UsageSchemaV1
import com.marketdata.states.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.*
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UsageIssueInitiator(val dataSet: String, val provider: Party, val dataChargeOwner: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = UsageContract.Commands.Issue()

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
            val providerIdx = PersistentPermission::providerName.equal(provider.toString())
            val dataSetIdx  = PersistentPermission::dataSetName.equal(dataSet)
            val subscriberIdx = PersistentPermission::subscriberName.equal(ourIdentity.toString())
            val dataChargeOwnerIdx = PersistentPermission::dataChargeOwnerName.equal(dataChargeOwner.toString())

            val customCriteria1 = VaultCustomQueryCriteria(providerIdx)
            val customCriteria2 = VaultCustomQueryCriteria(dataSetIdx)
            val customCriteria3 = VaultCustomQueryCriteria(subscriberIdx)
            val customCriteria4 = VaultCustomQueryCriteria(dataChargeOwnerIdx)

            val criteria = VaultQueryCriteria(Vault.StateStatus.ALL)
                    .and(customCriteria1
                            .and(customCriteria2
                                    .and(customCriteria3
                                            .and(customCriteria4))))
            serviceHub.vaultService.queryBy(PermissionState::class.java,criteria)
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

        val state = UsageState(dataSet, LinearPointer(permState.linearId, PermissionState::class.java ),  null, provider, dataChargeOwner, ourIdentity)

        val usageresults = builder {
            val providerIdx = UsageSchemaV1.PersistentUsage::providerName.equal(provider.toString())
            val dataSetIdx  = UsageSchemaV1.PersistentUsage::dataSet.equal(dataSet)
            val subscriberIdx = UsageSchemaV1.PersistentUsage::subscriberName.equal(ourIdentity.toString())
            val redistributorIdx = UsageSchemaV1.PersistentUsage::subscriberName.equal(dataChargeOwner.toString())
            val dateIdx = UsageSchemaV1.PersistentUsage::date.equal(state.date)

            val customCriteria1 = VaultCustomQueryCriteria(providerIdx)
            val customCriteria2 = VaultCustomQueryCriteria(dataSetIdx)
            val customCriteria3 = VaultCustomQueryCriteria(subscriberIdx)
            val customCriteria4 = VaultCustomQueryCriteria(redistributorIdx)
            val customCriteria5 = VaultCustomQueryCriteria(dateIdx)

            val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
                    .and(customCriteria1
                            .and(customCriteria2
                                    .and(customCriteria3
                                            .and(customCriteria4
                                                    .and(customCriteria5)))))
            serviceHub.vaultService.queryBy(PermissionState::class.java,criteria)
        }

        val usageStates = usageresults.states
        when {
            usageStates.isNotEmpty() -> {
                throw IllegalArgumentException("USage already exists for this combination of date/dataSet/provider/subscriber/dataChargeOwner")
            }
        }

        txb.withItems(
                StateAndContract(state, UsageContract.ID ),
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

@InitiatedBy(UsageIssueInitiator::class)
class UsageIssueResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a usage transaction" using (output is UsageState)

                // TODO: what do we want to check here?
            }
        }
        val expectedTxId = subFlow(signedTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
    }
}
