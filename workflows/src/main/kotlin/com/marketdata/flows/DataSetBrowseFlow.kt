package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.DataSetContract
import com.marketdata.states.DataSetResponseState
import com.marketdata.states.DataSetState
import com.marketdata.states.UsageState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********

@InitiatingFlow
@StartableByRPC
class DataSetBrowseInitiator(val requestor : Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction {
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = DataSetContract.Commands.List()

        txb.addCommand(Command(cmd, listOf(requestor.owningKey, ourIdentity.owningKey)))

        val permStates = serviceHub.vaultService.queryBy(DataSetState::class.java).states
        val refStates = permStates.map { it.ref }

        val outputState = DataSetResponseState(requestor, ourIdentity, refStates)

        txb.addOutputState(outputState)

        for (dataSet in permStates) {
            txb.addReferenceState(dataSet.referenced())
        }

        txb.verify(serviceHub)
        val partialTx = serviceHub.signInitialTransaction(txb)

        val sessions = listOf(initiateFlow(requestor))
        val signedTx = subFlow(CollectSignaturesFlow(partialTx, sessions))

        return subFlow(FinalityFlow(signedTx, sessions))
    }
}

@InitiatedBy(DataSetBrowseInitiator::class)
class DataSetBrowseResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be a usage transaction" using (output is DataSet)
//
//                // TODO: what do we want to check here?
            }
        }
        val expectedTxId = subFlow(signedTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
    }
}