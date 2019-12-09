package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.SignedTermsAndConditionsContract
import com.marketdata.states.SignedTermsAndConditionsState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateAndContract
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
class SignTandC(val tandcName: String, val issuer: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = SignedTermsAndConditionsContract.Commands.Issue()

        // TODO: throw correct exception
        val tAndCState = serviceHub.vaultService.getTandCState(tandcName, issuer)

        val signedTandC = SignedTermsAndConditionsState(
                tandcName,
                issuer,
                ourIdentity,
                LinearPointer(tAndCState.linearId, TermsAndConditionsState::class.java))

        txb.withItems(
                StateAndContract(signedTandC, SignedTermsAndConditionsContract.ID ),
                Command(cmd, signedTandC.participants.map { it.owningKey })
                )

        txb.verify(serviceHub)

        val ourSignedTx = serviceHub.signInitialTransaction(txb)

        val sessions = (signedTandC.participants - ourIdentity)
                .map { initiateFlow(it) }

        return subFlow(FinalityFlow(ourSignedTx, sessions))
    }
}

@InitiatedBy(SignTandC::class)
class SignTandCResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

