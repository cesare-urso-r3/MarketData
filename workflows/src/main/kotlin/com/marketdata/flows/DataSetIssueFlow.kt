package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.DataSetContract
import com.marketdata.contracts.PermissionContract
import com.marketdata.contracts.UsageContract
import com.marketdata.schema.PermissionSchemaV1
import com.marketdata.schema.PermissionSchemaV1.PersistentPermission
import com.marketdata.schema.UsageSchemaV1
import com.marketdata.states.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
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
class DataSetIssueInitiator(val name: String, val provider: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = DataSetContract.Commands.Issue()
        val state = DataSetState(name, provider)

        txb.withItems(
                StateAndContract(state, DataSetContract.ID ),
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

@InitiatedBy(DataSetIssueInitiator::class)
class DataSetIssueResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a usage transaction" using (output is DataSetState)

                // TODO: what do we want to check here?
            }
        }
        val expectedTxId = subFlow(signedTransactionFlow).id

        serviceHub.myInfo.legalIdentities

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
    }
}
