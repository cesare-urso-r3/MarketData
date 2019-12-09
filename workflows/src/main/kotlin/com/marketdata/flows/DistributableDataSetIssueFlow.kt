package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.DistributableDataSetContract
import com.marketdata.data.PricingParameter
import com.marketdata.states.DataSetState
import com.marketdata.states.DistributableDataSetState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class DistributableDataSetIssue(private val name: String,
                                private val dataSet: DataSetState,
                                private val tandc: TermsAndConditionsState,
                                private val pricing : PricingParameter) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = DistributableDataSetContract.Commands.Issue()

        val state = DistributableDataSetState(
                name,
                dataSet.provider,
                ourIdentity,
                LinearPointer(dataSet.linearId, DataSetState::class.java),
                listOf(pricing),
                LinearPointer(tandc.linearId, TermsAndConditionsState::class.java))

        txb.withItems(
                StateAndContract(state, DistributableDataSetContract.ID ),
                Command(cmd, state.participants.map { it.owningKey }),
                tandc.termsAndConditions
                )

        txb.verify(serviceHub)

        val ourSignedTx = serviceHub.signInitialTransaction(txb)
        return subFlow(FinalityFlow(ourSignedTx, emptyList()))
    }
}
