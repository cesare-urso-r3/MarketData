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
class DistributableDataSetRequestInitiator(private val requestFrom : Party,
                                           private val dataSetName: String,
                                           private val provider: Party) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val counterpartySession = initiateFlow(requestFrom)
        counterpartySession.send(DataSetNameAndProvider(dataSetName, provider))
    }
}

@InitiatedBy(DistributableDataSetRequestInitiator::class)
class DistributableDataSetRequestResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val dataSetNameAndProvider = counterpartySession.receive<DataSetNameAndProvider>().unwrap{ it }
        subFlow(DistributableDataSetResponseInitiator(counterpartySession.counterparty, dataSetNameAndProvider))
    }
}

@InitiatingFlow
class DistributableDataSetResponseInitiator(val requestor : Party, val dataSetNameAndProvider: DataSetNameAndProvider) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // now get the state with that name
        val dataSetStateAndRef = serviceHub.vaultService.getDistributableDataSetStateAndRef(
                dataSetNameAndProvider.provider,
                ourIdentity,
                dataSetNameAndProvider.name
        )

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

@InitiatedBy(DistributableDataSetResponseInitiator::class)
class DistributableDataSetResponseResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

