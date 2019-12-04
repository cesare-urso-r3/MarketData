package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.SignedTermsAndConditionsContract
import com.marketdata.schema.TermsAndConditionsSchemaV1
import com.marketdata.states.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
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
class SignTandC(val tandcName: String, val issuer: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = SignedTermsAndConditionsContract.Commands.Issue()

        // TODO: throw correct exception
        val tAndCState = getTandCStateByNameAndParty(serviceHub, tandcName, issuer)

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

        return subFlow(FinalityFlow(ourSignedTx, sessions));
    }
}

@InitiatedBy(SignTandC::class)
class SignTandCResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

fun getTandCStateByNameAndParty(serviceHub : ServiceHub, name : String, party: Party) : TermsAndConditionsState {
    return builder {
        val tAndCIdx = TermsAndConditionsSchemaV1.PersistentTandC::name.equal(name)
        val issuerIdx = TermsAndConditionsSchemaV1.PersistentTandC::issuerName.equal(party.name.toString())

        val customCriteria1 = VaultCustomQueryCriteria(tAndCIdx)
        val customCriteria2 = VaultCustomQueryCriteria(issuerIdx)

        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2))

        serviceHub.vaultService.queryBy(TermsAndConditionsState::class.java,criteria)
    }.states.single().state.data
}
