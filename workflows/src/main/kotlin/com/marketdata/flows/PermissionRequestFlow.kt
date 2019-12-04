package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.PermissionRequestContract
import com.marketdata.states.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********

@StartableByRPC
@InitiatingFlow
class PermissionRequest(val provider : Party, val redistributor : Party, val dataSetName : String) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // now get the state with that name
        val distDataSetStateAndRef = serviceHub.vaultService.GetDistributableDataSet(
                provider,
                redistributor,
                dataSetName)

        val distDataSet = distDataSetStateAndRef.state.data
        val requiredRedistributorTandC = distDataSet.termsAndConditions.resolve(serviceHub).state.data
        val signedRedistTandC = serviceHub.vaultService.GetSignedTandCStateAndRef(requiredRedistributorTandC, ourIdentity)

        val dataSet = distDataSet.dataSet.resolve(serviceHub).state.data
        val requiredProviderTandC = dataSet.termsAndConditions.resolve(serviceHub).state.data
        val signedProviderTandC = serviceHub.vaultService.GetSignedTandCStateAndRef(requiredProviderTandC, ourIdentity)

        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = PermissionRequestContract.Commands.Issue()

        val state = PermissionRequestState(
                            LinearPointer(
                                    distDataSet.linearId,
                                    DistributableDataSetState::class.java),
                            LinearPointer(
                                    signedProviderTandC.state.data.linearId,
                                    SignedTermsAndConditionsState::class.java),
                            LinearPointer(
                                    signedRedistTandC.state.data.linearId,
                                    SignedTermsAndConditionsState::class.java),
                            dataSetName,
                            provider,
                            ourIdentity,
                            redistributor
                    )

        txb.withItems(
                Command(cmd, state.participants.map { it.owningKey }),
                StateAndContract(state, PermissionRequestContract.ID)
        )

        txb.verify(serviceHub)

        val ourSignedTx = serviceHub.signInitialTransaction(txb)

        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet()

        val signedTx = subFlow(CollectSignaturesFlow(ourSignedTx, sessions))
        subFlow(FinalityFlow(signedTx, sessions))
    }
}

@InitiatedBy(PermissionRequest::class)
class PermissionRequestResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be a permission transaction" using (output is PermissionRequestState)

                // TODO: check that we are a provider/ redistrubutor of a data set,
                // and that we are also are happy to grant permission
            }
        }
        val expectedTxId = subFlow(signedTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
    }
}