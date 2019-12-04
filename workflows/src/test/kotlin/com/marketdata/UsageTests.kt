package com.marketdata

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
import com.marketdata.states.*
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import net.corda.core.node.services.vault.ColumnPredicate
import net.corda.core.utilities.getOrThrow
import java.security.PublicKey
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAmount
import java.util.*
import kotlin.test.assertEquals

class UsageTests {

    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.marketdata.contracts"),
            TestCordapp.findCordapp("com.marketdata.flows")
    ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
    )

    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()
    private val d = network.createNode()
    private val utils = TestUtilities(network)

    private lateinit var tandCAttachment: AttachmentId
    private lateinit var rtandCAttachment: AttachmentId
    private lateinit var r2tandCAttachment: AttachmentId
    private val tandCName = "DemoT&C.zip"
    private val tandCPath = "src/test/resources/DemoT&C.zip"
    private val rtandCName = "RedistributorDemoT&C.zip"
    private val r2tandCName = "RedistributorDemoT&C2.zip"
    private val rtandCPath = "src/test/resources/RedistributorDemoT&C.zip"
    private val r2tandCPath = "src/test/resources/RedistributorDemoT&C2.zip"

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(SignTandCResponder::class.java)
        }
    }

    @Before
    fun setup() {

        val attachmentInputStream = File(tandCPath).inputStream()
        val rattachmentInputStream = File(rtandCPath).inputStream()
        val r2attachmentInputStream = File(r2tandCPath).inputStream()
        a.transaction {
            tandCAttachment = a.services.attachments.importAttachment(attachmentInputStream, "user", tandCName)
        }
        b.transaction {
            rtandCAttachment = b.services.attachments.importAttachment(rattachmentInputStream, "user", rtandCName)
        }
        d.transaction {
            r2tandCAttachment = d.services.attachments.importAttachment(r2attachmentInputStream, "user", r2tandCName)
        }

        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun issueUsage() {

        val dataSet = "LSE L1"
        utils.issuePermission(dataSet, a, b, c, tandCAttachment, tandCName, rtandCAttachment, rtandCName)


        val flow = UsageIssue(dataSet, a.getDefaultId(), b.getDefaultId(), "A_TEST_USER" )
        val future = c.startFlow(flow)

        network.runNetwork()

        future.getOrThrow()

        val aStates = a.services.vaultService.queryBy(UsageState::class.java).states
        val bStates = b.services.vaultService.queryBy(UsageState::class.java).states
        val cStates = c.services.vaultService.queryBy(UsageState::class.java).states

        println("[STATE] A $aStates")
        println("[STATE] B $bStates")
        println("[STATE] C $cStates")

        assertEquals(bStates.map { it.ref }, aStates.map { it.ref })
        assertEquals(cStates.map { it.ref }, bStates.map { it.ref })
        assert(aStates.isNotEmpty())

        val a1States = a.services.vaultService.queryBy(UsageReceiptState::class.java).states
        val b1States = b.services.vaultService.queryBy(UsageReceiptState::class.java).states
        val c1States = c.services.vaultService.queryBy(UsageReceiptState::class.java).states
        println("[STATE] A1 $a1States")
        println("[STATE] B1 $b1States")
        println("[STATE] C1 $c1States")

        assertEquals(b1States.map { it.ref }, a1States.map { it.ref })
        assertEquals(c1States.map { it.ref }, b1States.map { it.ref })
        assert(aStates.isNotEmpty())

//        val usageReceiptState = c1States.single().state.data
//
//        val flow2 = UsageIssue(dataSet, a.getDefaultId(), b.getDefaultId(), "A_TEST_USER", usageReceiptState )
//        val future2 = c.startFlow(flow2)
//
//        network.runNetwork()
//
//        future2.getOrThrow()


        for(i in 1..5) {
            val flowloop = UsageIssue(dataSet, a.getDefaultId(), b.getDefaultId(), "A_TEST_USER_$i" )
            val futureloop = c.startFlow(flowloop)
            network.runNetwork()
            futureloop.getOrThrow()
        }

        utils.issuePermission(dataSet, a, d, c, tandCAttachment, tandCName, r2tandCAttachment, r2tandCName)

        println("*** DOUBLE_BILL ***")
        utils.runFlowOnBehalfOf(c, UsageIssue(dataSet, a.getDefaultId(), d.getDefaultId(), "DOUBLE_BILL" ))
        c.services.vaultService
                .queryBy(UsageReceiptState::class.java).states.filter { it.state.data.userName == "DOUBLE_BILL" }
                .forEach { println("${it.state.data.userName} ") }

        val usageReceipt = c.services.vaultService
                .queryBy(UsageReceiptState::class.java).states.filter { it.state.data.userName == "DOUBLE_BILL" }.single().state.data

        utils.runFlowOnBehalfOf(c, UsageIssue(dataSet, a.getDefaultId(), b.getDefaultId(), "DOUBLE_BILL", usageReceipt ))


        val a2States = a.services.vaultService.queryBy(UsageState::class.java).states
        val b2States = b.services.vaultService.queryBy(UsageState::class.java).states
        val c2States = c.services.vaultService.queryBy(UsageState::class.java).states

        println("[STATE] A2 $a2States")
        println("[STATE] B2 $b2States")
        println("[STATE] C2 $c2States")

        assertEquals(b2States.map { it.ref }, a2States.map { it.ref })
        assertEquals(c2States.map { it.ref }, b2States.map { it.ref })
        assert(a2States.isNotEmpty())

        val billFlow = BillCreate(
                LocalDate.now().minusDays(1).toString(),
                LocalDate.now().plusDays(1).toString(),
                c.getDefaultId())

        val billFuture = b.startFlow(billFlow)
        network.runNetwork()
        billFuture.getOrThrow()

        val a3States = a.services.vaultService.queryBy(BillingState::class.java).states
        val b3States = b.services.vaultService.queryBy(BillingState::class.java).states
        val c3States = c.services.vaultService.queryBy(BillingState::class.java).states

        println("[STATE] A3 $a3States")
        println("[STATE] B3 $b3States")
        println("[STATE] C3 $c3States")

        assertEquals(b3States.map { it.ref }, c3States.map { it.ref })
        assert(c2States.isNotEmpty())



    }

}