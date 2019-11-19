package com.marketdata

import com.marketdata.flows.PermissionIssueInitiator
import com.marketdata.flows.PermissionIssueResponder
import com.marketdata.flows.UsageIssueInitiator
import com.marketdata.flows.UsageIssueResponder
import com.marketdata.states.PermissionState
import net.corda.core.node.NetworkParameters
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UsageIssueFlowTests {
    private val network = MockNetwork(
            MockNetworkParameters(
                    cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.marketdata.contracts"),
                        TestCordapp.findCordapp("com.marketdata.flows")
                    ),
                    networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
            )
    )

    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b, c).forEach {
            it.registerInitiatedFlow(PermissionIssueResponder::class.java)
            it.registerInitiatedFlow(UsageIssueResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val provider = b.info.chooseIdentityAndCert().party
        val redistributor = c.info.chooseIdentityAndCert().party

        val dataSet = "test_set_1"

        val flow = PermissionIssueInitiator(dataSet, provider, redistributor)
        val future = a.startFlow(flow)

        network.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b, c).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }

        val uflow = UsageIssueInitiator(dataSet,provider, redistributor)
        val ufuture = a.startFlow(uflow)

        network.runNetwork()
        val ustx = ufuture.getOrThrow()
        println("Signed transaction hash: ${ustx.id}")
        listOf(a, b, c).map {
            it.services.validatedTransactions.getTransaction(ustx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${ustx.id}")
            assertEquals(ustx.id, txHash)
        }

    }

    @Test
    fun flowFailsWithoutReferenceState() {
        val provider = b.info.chooseIdentityAndCert().party
        val redistributor = c.info.chooseIdentityAndCert().party

        val dataSet = "test_set_1"
        val dataSet2 = "test_set_2"

        val flow = PermissionIssueInitiator(dataSet, provider, redistributor)
        val future = a.startFlow(flow)

        network.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b, c).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }

        val uflow = UsageIssueInitiator(dataSet2,provider, redistributor)
        val ufuture = a.startFlow(uflow)

        network.runNetwork()
        assertFailsWith<IllegalArgumentException>(
                "No permission to access data set (test_set_2) from provider (O=Mock Company 2, L=London, C=GB") {
            ufuture.getOrThrow() }
    }
}