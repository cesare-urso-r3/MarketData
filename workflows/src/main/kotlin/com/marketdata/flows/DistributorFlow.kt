package com.marketdata.flows

// *********
// * Flows *
// *********
//@InitiatingFlow
//@StartableByRPC
//class GetTandCInitiator(val provider: Party) : FlowLogic<Unit>() {
//    override val progressTracker = ProgressTracker()
//
//    @Suspendable
//    override fun call() {
//        val session = initiateFlow(provider)
//        session.send(ourIdentity.name)
//    }
//}
//
//@InitiatedBy(GetTandCInitiator::class)
//class GetTandCResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//
//        counterpartySession.receive<String>().unwrap{ it }
//
//        val tandc = serviceHub.vaultService.queryBy(TermsAndConditionsState::class.java).states.filter {
//            val state = it.state.data
//            state.issuer == ourIdentity && state.name == "REDISTRIBUTOR"
//        }
//
//        if ( tandc.size == 1 )
//        {
//            counterpartySession.send(tandc.first())
//        } else {
//            // TODO: throw
//        }
//
//    }
//}
//@InitiatingFlow
//@StartableByRPC
//class DataSetBrowseInitiator(val requestor : Party) : FlowLogic<SignedTransaction>() {
//    override val progressTracker = ProgressTracker()
//
//    @Suspendable
//    override fun call() : SignedTransaction {
//        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
//        val cmd = DataSetContract.Commands.List()
//
//        txb.addCommand(Command(cmd, listOf(requestor.owningKey, ourIdentity.owningKey)))
//
//        val permStates = serviceHub.vaultService.queryBy(DataSetState::class.java).states
//        val refStates = permStates.map { it.ref }
//
//        val outputState = DataSetResponseState(requestor, ourIdentity, refStates)
//
//        txb.addOutputState(outputState)
//
//        for (dataSet in permStates) {
//            txb.addReferenceState(dataSet.referenced())
//        }
//
//        txb.verify(serviceHub)
//        val partialTx = serviceHub.signInitialTransaction(txb)
//
//        val sessions = listOf(initiateFlow(requestor))
//        val signedTx = subFlow(CollectSignaturesFlow(partialTx, sessions))
//
//        return subFlow(FinalityFlow(signedTx, sessions))
//    }
//}
//
//@InitiatedBy(DataSetBrowseInitiator::class)
//class DataSetBrowseResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                //                val output = stx.tx.outputs.single().data
////                "This must be a usage transaction" using (output is DataSet)
////
////                // TODO: what do we want to check here?
//            }
//        }
//        val expectedTxId = subFlow(signedTransactionFlow).id
//
//        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId))
//    }
//}