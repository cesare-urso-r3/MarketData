package com.marketdata

import com.marketdata.flows.*
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.node.services.AttachmentId
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import com.marketdata.data.PricingParameter
import com.marketdata.states.SignedTermsAndConditionsState

class TandCFlowTests {

    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.marketdata.contracts"),
            TestCordapp.findCordapp("com.marketdata.flows")
    ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
    )

    private val a = network.createNode()
    private val b = network.createNode()
    private val utils = TestUtilities(network)

    private lateinit var tandCAttachment: AttachmentId
    private val tandCName = "DemoT&C.zip"
    private val tandCPath = "src/test/resources/DemoT&C.zip"

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(SignTandCResponder::class.java)
        }
    }

    @Before
    fun setup() {

        val attachmentInputStream = File(tandCPath).inputStream()
        a.transaction {
            tandCAttachment = a.services.attachments.importAttachment(attachmentInputStream, "user", tandCName)
        }

        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun issueTandC() {

        utils.creatTandC(tandCAttachment, tandCName, a)

        val states = a.services.vaultService.queryBy(TermsAndConditionsState::class.java).states
        assert(states.isNotEmpty())
    }

    @Test
    fun signTandC() {
        val dataSet = "LSE L1"
        val tandcState = utils.creatTandC(tandCAttachment, tandCName, a)
        val pricing = PricingParameter(99.toDouble())

        val dataSetState = utils.createDataSet(a, dataSet, tandcState, pricing)
        utils.distributeDataSet(a, b, dataSetState)


        val TandCA = a.services.vaultService.queryBy(TermsAndConditionsState::class.java).states
        val TandCB = b.services.vaultService.queryBy(TermsAndConditionsState::class.java).states

        println("A -> ${TandCA.first().state.data}")
        println("B -> ${TandCB.first().state.data}")

        val flow = SignTandC(tandCName, a.info.legalIdentities.first())
        val future = b.startFlow(flow)

        network.runNetwork()
        future.getOrThrow()

        //Thread.sleep(2000L)
        val states = b.services.vaultService.queryBy(SignedTermsAndConditionsState::class.java).states

        println(states)
        assert(states.isNotEmpty())

    }

}