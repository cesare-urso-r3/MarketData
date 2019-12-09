package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.TermsAndConditionsContract
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class IssueTandC(val attachmentId: AttachmentId, val attachmentName : String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call() : SignedTransaction{
        val cmd = TermsAndConditionsContract.Commands.Issue(attachmentId)
        val tAndCState = TermsAndConditionsState(attachmentName, ourIdentity, attachmentId)

        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txb.withItems(
                StateAndContract(tAndCState, TermsAndConditionsContract.ID ),
                Command(cmd, listOf(ourIdentity.owningKey)),
                attachmentId
        )

        txb.verify(serviceHub)

        val ourSignedTx = serviceHub.signInitialTransaction(txb)

        return subFlow(FinalityFlow(ourSignedTx, emptyList()))
    }
}