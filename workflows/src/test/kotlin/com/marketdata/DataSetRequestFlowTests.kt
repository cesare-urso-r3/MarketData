package com.marketdata

import com.marketdata.contracts.PermissionContract
import com.marketdata.flows.*
import com.marketdata.states.DataSetState
import com.marketdata.states.PermissionState
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DataSetRequestFlowTests {

    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.marketdata.contracts"),
                TestCordapp.findCordapp("com.marketdata.flows")
            ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
    )

    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b, c).forEach {
           // it.registerInitiatedFlow(DataSetBrowseResponder::class.java)
            it.registerInitiatedFlow(DataSetIssueResponder::class.java)
            it.registerInitiatedFlow(DataSetRequestResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    fun createDataSet (dataSet : String, provider : Party) {

        println("Creating DataSet: $dataSet (${provider.name})")
        val flow = DataSetIssueInitiator(dataSet, provider)
        val future = b.startFlow(flow)

        network.runNetwork()
        val stx = future.getOrThrow()
        listOf(b).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            assertEquals(stx.id, txHash)
        }
        println("Created successfully")
    }
    @Test
    fun flowRequestDataSet() {
        val provider = b.info.chooseIdentityAndCert().party

        createDataSet("LSE L1", provider)
        createDataSet("CME L2", provider)

        val flow2 = DataSetRequestInitiator(provider)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        future2.getOrThrow()
        b.services.vaultService.queryBy(DataSetState::class.java).states.forEach {
            println("B STATE")
            println(it.toString())
        }

        val states = a.services.vaultService.queryBy(DataSetState::class.java).states

        states.forEach {
            println("A STATE")
            println(it.toString())
        }

        assert(states.isNotEmpty())


    }
}