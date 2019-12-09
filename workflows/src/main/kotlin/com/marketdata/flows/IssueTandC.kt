package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.*
import com.marketdata.schema.DataSetSchemaV1
import com.marketdata.schema.PermissionSchemaV1
import com.marketdata.schema.PermissionSchemaV1.PersistentPermission
import com.marketdata.schema.TermsAndConditionsSchemaV1
import com.marketdata.schema.UsageSchemaV1
import com.marketdata.states.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.*
import net.corda.core.node.services.vault.builder
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