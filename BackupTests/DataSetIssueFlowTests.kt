package com.marketdata

import com.marketdata.contracts.PermissionContract
import com.marketdata.flows.DataSetIssueInitiator
import com.marketdata.flows.DataSetIssueResponder
import com.marketdata.flows.PermissionIssueInitiator
import com.marketdata.flows.PermissionIssueResponder
import com.marketdata.states.PermissionState
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DataSetIssueFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.marketdata.contracts"),
        TestCordapp.findCordapp("com.marketdata.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b, c).forEach {
         //   it.registerInitiatedFlow(DataSetIssueResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun flowCreatesDataSet() {
        val dataSet = "LSE L1"
        val provider = b.info.chooseIdentityAndCert().party

        val flow = DataSetIssueInitiator(dataSet, provider)
        val future = b.startFlow(flow)

        network.runNetwork()
        val stx = future.getOrThrow()
        //println("Signed transaction hash: ${stx.id}")
        listOf(b).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }
}