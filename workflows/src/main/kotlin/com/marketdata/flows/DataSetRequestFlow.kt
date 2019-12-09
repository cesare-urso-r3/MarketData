package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.DistributionContract
import com.marketdata.states.DistributionState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class DataSetRequestInitiator(private val requestFrom : Party,
                              private val dataSet: String) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        if (!serviceHub.vaultService.hasDataSet(dataSet, requestFrom)) {
            val counterpartySession = initiateFlow(requestFrom)
            counterpartySession.send(dataSet)
        }
    }
}

@InitiatedBy(DataSetRequestInitiator::class)
class DataSetRequestResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val dataSet = counterpartySession.receive<String>().unwrap{ it }
        subFlow(DataSetResponseInitiator(counterpartySession.counterparty, dataSet))
    }
}

@InitiatingFlow
class DataSetResponseInitiator(val requestor : Party, val dataSet: String) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {

        // now get the state with that name
        val dataSetStateAndRef = serviceHub.vaultService.getDataSetStateAndRef(dataSet, ourIdentity)

        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = DistributionContract.Commands.Distribute()

        txb.withItems(
                Command(cmd, listOf(ourIdentity.owningKey)),
                StateAndContract(DistributionState(ourIdentity, requestor), DistributionContract.ID),
                ReferencedStateAndRef(dataSetStateAndRef)
        )

        txb.verify(serviceHub)

        val ourSignedTx = serviceHub.signInitialTransaction(txb)

        subFlow(FinalityFlow(ourSignedTx, listOf(initiateFlow(requestor))))
    }
}

@InitiatedBy(DataSetResponseInitiator::class)
class DataSetResponseResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

