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
import com.marketdata.flows.*
import com.marketdata.states.DistributableDataSetState
import com.marketdata.states.PermissionRequestState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import net.corda.core.node.services.vault.ColumnPredicate
import net.corda.core.utilities.getOrThrow
import java.security.PublicKey
import kotlin.test.assertEquals

class PermissionTests {

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
    fun requestPermission() {

        val dataSet = "LSE L1"
        utils.setUpDistributeDistributableDataSet(dataSet, a, b, c, tandCAttachment, tandCName, rtandCAttachment, rtandCName)

        val flowp = SignTandC(tandCName, a.getDefaultId())
        val futurep = c.startFlow(flowp)

        network.runNetwork()
        futurep.getOrThrow()

        val flowr = SignTandC(rtandCName, b.getDefaultId())
        val futurer = c.startFlow(flowr)

        network.runNetwork()
        futurer.getOrThrow()

        val flow = PermissionRequest(a.getDefaultId(), b.getDefaultId(), dataSet)
        val future = c.startFlow(flow)

        network.runNetwork()

        future.getOrThrow()

        val aStates = a.services.vaultService.queryBy(PermissionRequestState::class.java).states
        val bStates = b.services.vaultService.queryBy(PermissionRequestState::class.java).states
        val cStates = c.services.vaultService.queryBy(PermissionRequestState::class.java).states

        println("[STATE] A $aStates")
        println("[STATE] B $bStates")
        println("[STATE] C $cStates")

        assertEquals(bStates.map { it.ref }, aStates.map { it.ref })
        assertEquals(cStates.map { it.ref }, bStates.map { it.ref })
        assert(aStates.isNotEmpty())
    }

}