package com.marketdata

import com.marketdata.data.PricingParameter
import com.marketdata.flows.*
import com.marketdata.states.DataSetState
import com.marketdata.states.DistributableDataSetState
import com.marketdata.states.PermissionRequestState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.Test
import kotlin.test.assertEquals

fun StartedMockNode.getDefaultId() : Party = this.info.legalIdentities.first()

class TestUtilities (val network : MockNetwork){

    fun creatTandC (attachmentId: AttachmentId, tandCName : String, node: StartedMockNode) : TermsAndConditionsState {

        println("Creating TandC: $tandCName ($attachmentId)")

        val flow = IssueTandC(attachmentId, tandCName)
        val future = node.startFlow(flow)

        network.runNetwork()
        val stx = future.getOrThrow()
        listOf(node).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            assertEquals(stx.id, txHash)
        }
        println("Created TandC successfully")
        assertEquals(stx.tx.outputStates.size, 1)
        val retState = stx.tx.outputStates.single()
        assert( retState is TermsAndConditionsState)
        return retState as TermsAndConditionsState
    }

    fun createDataSet (node: StartedMockNode,
                       dataSet : String,
                       tandcState : TermsAndConditionsState,
                       pricing : PricingParameter = PricingParameter(99.toDouble())) : DataSetState {

        println("Creating DataSet: $dataSet")

        val flow = DataSetIssue(dataSet, tandcState, pricing)
        val future = node.startFlow(flow)

        network.runNetwork()
        println("Dataset flow started")

        val stx = future.getOrThrow()
        listOf(node).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            assertEquals(stx.id, txHash)
        }
        println("Created successfully")
        network.runNetwork()

        val states = node.services.vaultService.queryBy(DataSetState::class.java).states
        assert(states.isNotEmpty())

        assertEquals(stx.tx.outputStates.size, 1)
        val retState = stx.tx.outputStates.single()
        assert( retState is DataSetState)
        return retState as DataSetState
    }

    fun distributeDataSet(fromNode: StartedMockNode, toNode: StartedMockNode, dataSetState : DataSetState) {

        val dataSet = dataSetState.name

        val flow = DataSetRequestInitiator(fromNode.info.legalIdentities.first(), dataSet)
        val future = toNode.startFlow(flow)

        network.runNetwork()

        future.getOrThrow()
    }

    fun createdistributableDataSet(node: StartedMockNode,
                                   dataSetName : String,
                                   dataSet : DataSetState,
                                   tandcState : TermsAndConditionsState,
                                   pricing : PricingParameter = PricingParameter(99.toDouble())) : DistributableDataSetState {

        val flow = DistributableDataSetIssue(dataSetName, dataSet, tandcState, pricing)
        val future = node.startFlow(flow)
        network.runNetwork()

        val stx = future.getOrThrow()

        val states = node.services.vaultService.queryBy(DistributableDataSetState::class.java).states
        assert(states.isNotEmpty())

        assertEquals(stx.tx.outputStates.size, 1)
        val retState = stx.tx.outputStates.single()
        assert( retState is DistributableDataSetState)
        return retState as DistributableDataSetState
    }

    fun distributeDistributableDataSet(fromNode: StartedMockNode,
                                       toNode: StartedMockNode,
                                       providerNode: StartedMockNode,
                                       distDataSetState : DistributableDataSetState) {

        val dataSet = distDataSetState.dataSetName

        val flow = DistributableDataSetRequestInitiator(fromNode.getDefaultId(), dataSet, providerNode.getDefaultId())
        val future = toNode.startFlow(flow)

        network.runNetwork()

        future.getOrThrow()
    }

    fun setUpDistributeDistributableDataSet(dataSetName: String,
                                            provider : StartedMockNode,
                                            redistributor: StartedMockNode,
                                            subscriber: StartedMockNode,
                                            tandCAttachment : AttachmentId,
                                            tandCName: String,
                                            rtandCAttachment : AttachmentId,
                                            rtandCName: String) {

        val tandcState = creatTandC(tandCAttachment, tandCName, provider)
        val pricing = PricingParameter(99.toDouble())

        val dataSetState = createDataSet(provider, dataSetName, tandcState, pricing)

        distributeDataSet(provider, redistributor, dataSetState)

        val rtandcState = creatTandC(rtandCAttachment, rtandCName, redistributor)
        val rpricing = PricingParameter(31.toDouble())
        val distDataSet = createdistributableDataSet(redistributor,dataSetName, dataSetState, rtandcState, rpricing)


        distributeDistributableDataSet(redistributor, subscriber, provider, distDataSet)

        val bStates = redistributor.services.vaultService.queryBy(DistributableDataSetState::class.java).states
        val cStates = subscriber.services.vaultService.queryBy(DistributableDataSetState::class.java).states

        assertEquals(cStates.map { it.ref }, bStates.map { it.ref })

        val criteria = AttachmentQueryCriteria.AttachmentsQueryCriteria(signersCondition = Builder.equal(listOf(redistributor.getDefaultId().owningKey)))
        val aAttach = provider.services.attachments.queryAttachments(criteria)
        val bAttach = redistributor.services.attachments.queryAttachments(criteria)

        assertEquals(aAttach.toSet(), bAttach.toSet())
    }

    fun issuePermission(dataSetName: String,
                        provider : StartedMockNode,
                        redistributor: StartedMockNode,
                        subscriber: StartedMockNode,
                        tandCAttachment : AttachmentId,
                        tandCName: String,
                        rtandCAttachment : AttachmentId,
                        rtandCName: String) {

        setUpDistributeDistributableDataSet(dataSetName, provider, redistributor, subscriber, tandCAttachment, tandCName, rtandCAttachment, rtandCName)

        val flowp = SignTandC(tandCName, provider.getDefaultId())
        val futurep = subscriber.startFlow(flowp)

        network.runNetwork()
        futurep.getOrThrow()

        val flowr = SignTandC(rtandCName, redistributor.getDefaultId())
        val futurer = subscriber.startFlow(flowr)

        network.runNetwork()
        futurer.getOrThrow()

        val flow = PermissionRequest(provider.getDefaultId(), redistributor.getDefaultId(), dataSetName)
        val future = subscriber.startFlow(flow)

        network.runNetwork()

        future.getOrThrow()

        val aStates = provider.services.vaultService.queryBy(PermissionRequestState::class.java).states
        val bStates = redistributor.services.vaultService.queryBy(PermissionRequestState::class.java).states
        val cStates = subscriber.services.vaultService.queryBy(PermissionRequestState::class.java).states
//
//        println("[STATE] A $aStates")
//        println("[STATE] B $bStates")
//        println("[STATE] C $cStates")

        assertEquals(bStates.map { it.ref }, aStates.map { it.ref })
        assertEquals(cStates.map { it.ref }, bStates.map { it.ref })
        assert(aStates.isNotEmpty())
    }

    fun <T> runFlowOnBehalfOf(onBehalfOf : StartedMockNode, flow : FlowLogic<T>) : T {

        val future = onBehalfOf.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

}