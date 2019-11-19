package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class DataSetRequestInitiator(val partyToQuery : Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val counterpartySession = initiateFlow(partyToQuery)
        counterpartySession.send(ourIdentity.name.toString())
    }
}

@InitiatedBy(DataSetRequestInitiator::class)
class DataSetRequestResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        counterpartySession.receive<String>().unwrap{ it }
        subFlow(DataSetBrowseInitiator(counterpartySession.counterparty))
    }
}
