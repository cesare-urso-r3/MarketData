package com.marketdata

import com.marketdata.contracts.PermissionContract
import com.marketdata.flows.DataSetIssueInitiator
import com.marketdata.flows.DataSetRequestInitiator
import com.marketdata.flows.PermissionIssueInitiator
import com.marketdata.flows.PermissionIssueResponder
import com.marketdata.states.DataSetState
import com.marketdata.states.PermissionState
import net.corda.core.identity.Party
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

class PermissionIssueFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.marketdata.contracts"),
        TestCordapp.findCordapp("com.marketdata.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(PermissionIssueResponder::class.java)
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
        val dataChargeOwner = c.info.chooseIdentityAndCert().party


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

        val permFlow = PermissionIssueInitiator("LSE L1", provider, dataChargeOwner)
        val permFuture = a.startFlow(permFlow)


    }
//    @Test
//    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
//        val subscriber = a.info.chooseIdentityAndCert().party
//        val provider = b.info.chooseIdentityAndCert().party
//        val redistributor = b.info.chooseIdentityAndCert().party
//        val flow = PermissionIssueInitiator("test_set_1", provider, redistributor)
//        val future = a.startFlow(flow)
//        network.runNetwork()
//        // Return the unsigned(!) SignedTransaction object from the flow.
//        val ptx: SignedTransaction = future.getOrThrow()
//        // Print the transaction for debugging purposes.
//        println(ptx.tx)
//        // Check the transaction is well formed...
//        // No outputs, one input IOUState and a command with the right properties.
//        assert(ptx.tx.inputs.isEmpty())
//        assert(ptx.tx.outputs.single().data is PermissionState)
//        val command = ptx.tx.commands.single()
//        assert(command.value is PermissionContract.Commands.Issue)
//       // assert(command.signers.toSet() == setOf(subscriber, provider, redistributor))
//        ptx.verifySignaturesExcept(subscriber.owningKey,
//                network.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey)
//    }
//
//
//    @Test
//    fun flowRecordsTheSameTransactionInBothPartyVaults() {
//        val provider = b.info.chooseIdentityAndCert().party
//        val redistributor = c.info.chooseIdentityAndCert().party
//
//        val flow = PermissionIssueInitiator("test_set_1", provider, redistributor)
//        val future = a.startFlow(flow)
//
//        network.runNetwork()
//        val stx = future.getOrThrow()
//        println("Signed transaction hash: ${stx.id}")
//        listOf(a, b, c).map {
//            it.services.validatedTransactions.getTransaction(stx.id)
//        }.forEach {
//            val txHash = (it as SignedTransaction).id
//            println("$txHash == ${stx.id}")
//            assertEquals(stx.id, txHash)
//        }
//    }
//
//    @Test
//    fun cannotIssueTheSamePermissionTwice() {
//        val subscriber = a.info.chooseIdentityAndCert().party
//        val provider = b.info.chooseIdentityAndCert().party
//        val redistributor = b.info.chooseIdentityAndCert().party
//        val dataSet = "duplicate_test"
//        val flow = PermissionIssueInitiator(dataSet, provider, redistributor)
//        val future = a.startFlow(flow)
//        network.runNetwork()
//        a.services
//        // Return the unsigned(!) SignedTransaction object from the flow.
//        val ptx: SignedTransaction = future.getOrThrow()
//        // Print the transaction for debugging purposes.
//        println(ptx.tx)
//        // Check the transaction is well formed...
//        // No outputs, one input IOUState and a command with the right properties.
//        assert(ptx.tx.inputs.isEmpty())
//        assert(ptx.tx.outputs.single().data is PermissionState)
//        val command = ptx.tx.commands.single()
//        assert(command.value is PermissionContract.Commands.Issue)
//        //assert(command.signers.toSet() == setOf(subscriber, provider, redistributor))
//        ptx.verifySignaturesExcept(subscriber.owningKey,
//                network.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey)
//
//        val flow2 = PermissionIssueInitiator(dataSet, provider, redistributor)
//        val future2 = a.startFlow(flow2)
//        network.runNetwork()
//
//        assertFailsWith<IllegalArgumentException>(
//                "Permission already exists for this combination of dataSet/provider/subscriber/dataChargeOwner") {
//            future2.getOrThrow() }
//
//    }
}