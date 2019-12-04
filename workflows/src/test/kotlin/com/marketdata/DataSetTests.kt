package com.marketdata

import com.marketdata.states.DataSetState
import net.corda.core.node.services.AttachmentId
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import com.marketdata.data.PricingParameter
import com.marketdata.flows.DistributableDataSetIssue
import com.marketdata.flows.SignTandCResponder
import com.marketdata.states.DistributableDataSetState
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import net.corda.core.node.services.vault.ColumnPredicate
import net.corda.core.utilities.getOrThrow
import java.security.PublicKey
import kotlin.test.assertEquals

class DataSetTests {

    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.marketdata.contracts"),
            TestCordapp.findCordapp("com.marketdata.flows")
    ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
    )

    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()
    private val utils = TestUtilities(network)

    private lateinit var tandCAttachment: AttachmentId
    private lateinit var rtandCAttachment: AttachmentId
    private val tandCName = "DemoT&C.zip"
    private val tandCPath = "src/test/resources/DemoT&C.zip"
    private val rtandCName = "RedistributorDemoT&C.zip"
    private val rtandCPath = "src/test/resources/RedistributorDemoT&C.zip"

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(SignTandCResponder::class.java)
        }
    }

    @Before
    fun setup() {

        val attachmentInputStream = File(tandCPath).inputStream()
        val rattachmentInputStream = File(rtandCPath).inputStream()
        a.transaction {
            tandCAttachment = a.services.attachments.importAttachment(attachmentInputStream, "user", tandCName)
        }
        b.transaction {
            rtandCAttachment = b.services.attachments.importAttachment(rattachmentInputStream, "user", rtandCName)
        }

        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun issueDataSet() {

        val dataSet = "LSE L1"
        val tandcState = utils.creatTandC(tandCAttachment, tandCName, a)
        val pricing = PricingParameter(99.toDouble())

        utils.createDataSet(a, dataSet, tandcState, pricing)

        val states = a.services.vaultService.queryBy(DataSetState::class.java).states
        assert(states.isNotEmpty())
    }

    @Test
    fun distributeDataSet() {

        val dataSet = "LSE L1"
        val tandcState = utils.creatTandC(tandCAttachment, tandCName, a)
        val pricing = PricingParameter(99.toDouble())

        val dataSetState = utils.createDataSet(a, dataSet, tandcState, pricing)

        utils.distributeDataSet(a, b, dataSetState)

        val aStates = a.services.vaultService.queryBy(DataSetState::class.java).states
        val bStates = b.services.vaultService.queryBy(DataSetState::class.java).states

        println("[STATE] A $aStates")
        println("[STATE] B $bStates")

        assertEquals(aStates.map { it.ref }, bStates.map { it.ref })
        val criteria = AttachmentQueryCriteria.AttachmentsQueryCriteria(signersCondition = Builder.equal(listOf(a.getDefaultId().owningKey)))
        val aAttach = a.services.attachments.queryAttachments(criteria)
        val bAttach = b.services.attachments.queryAttachments(criteria)

        assertEquals(aAttach.toSet(), bAttach.toSet())

        println("[ATTACHMENT] A $aAttach")
        println("[ATTACHMENT] B $bAttach")
    }

    @Test
    fun issueDistributableDataSet() {

        val dataSet = "LSE L1"
        val tandcState = utils.creatTandC(tandCAttachment, tandCName, a)
        val pricing = PricingParameter(99.toDouble())

        val dataSetState = utils.createDataSet(a, dataSet, tandcState, pricing)

        utils.distributeDataSet(a, b, dataSetState)

        val rtandcState = utils.creatTandC(rtandCAttachment, rtandCName, b)
        val rpricing = PricingParameter(31.toDouble())
        utils.createdistributableDataSet(b,dataSet, dataSetState, rtandcState, rpricing)
    }


    @Test
    fun distributeDistributableDataSet() {

        val dataSet = "LSE L1"
        val tandcState = utils.creatTandC(tandCAttachment, tandCName, a)
        val pricing = PricingParameter(99.toDouble())

        val dataSetState = utils.createDataSet(a, dataSet, tandcState, pricing)

        utils.distributeDataSet(a, b, dataSetState)

        val rtandcState = utils.creatTandC(rtandCAttachment, rtandCName, b)
        val rpricing = PricingParameter(31.toDouble())
        val distDataSet = utils.createdistributableDataSet(b,dataSet, dataSetState, rtandcState, rpricing)


        utils.distributeDistributableDataSet(b, c, a, distDataSet)

        val bStates = b.services.vaultService.queryBy(DistributableDataSetState::class.java).states
        val cStates = c.services.vaultService.queryBy(DistributableDataSetState::class.java).states

        println("[STATE] B $bStates")
        println("[STATE] C $cStates")

        assertEquals(cStates.map { it.ref }, bStates.map { it.ref })

        val criteria = AttachmentQueryCriteria.AttachmentsQueryCriteria(signersCondition = Builder.equal(listOf(b.getDefaultId().owningKey)))
        val aAttach = a.services.attachments.queryAttachments(criteria)
        val bAttach = b.services.attachments.queryAttachments(criteria)

        assertEquals(aAttach.toSet(), bAttach.toSet())

        println("[ATTACHMENT] A $aAttach")
        println("[ATTACHMENT] B $bAttach")
    }
}