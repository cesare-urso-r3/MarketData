package com.marketdata.contracts

import com.marketdata.states.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.LedgerTransaction
import java.util.jar.JarInputStream

class PermissionRequestContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.PermissionRequestContract"
    }

    interface Commands : CommandData {
        class Issue() : TypeOnlyCommandData(), Commands
        class Revoke : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Issue -> {
                "No inputs should be consumed when issuing data set." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as PermissionRequestState

//                println("Signed by: ${cmd.signers.map { it.toString() }}")
//                println("Provider: ${outputState.provider.owningKey}")
//                println("Subscriber: ${outputState.subscriber.owningKey}")
//                println("Redistributor: ${outputState.dataChargeOwner.owningKey}")

                "The subscriber must sign" using (
                        cmd.signers.toSet() == setOf(outputState.subscriber.owningKey))

                "The dataSet name cannot be empty" using ( outputState.dataSetName.isNotEmpty() )

                val distDataSet = outputState.distributableDataSet.resolve(tx).state.data
                val dataSet = distDataSet.dataSet.resolve(tx).state.data

                val distTandCs = distDataSet.termsAndConditions.resolve(tx).state.data
                val dataTandCs = dataSet.termsAndConditions.resolve(tx).state.data

                val signedDistTandCs = outputState.redistributorTandCs.resolve(tx).state.data
                val signedDataTandCs = outputState.providerTandCs.resolve(tx).state.data

                "The supplied redistributor terms and conditions are incorrect" using
                        (signedDistTandCs.name == distTandCs.name)
                "The supplied provider terms and conditions are incorrect" using
                        (signedDataTandCs.name == dataTandCs.name)

                "The redistributor T&Cs must be signed by the subscriber in this request" using
                        (signedDistTandCs.signer == outputState.subscriber)
                "The data T&Cs must be signed by the subscriber in this request" using
                        (signedDataTandCs.signer == outputState.subscriber)

                "The dataSet requested does not match the provided dataSet details" using
                        ( dataSet.name == outputState.dataSetName )

                //TODO: there may be other references to cross-reference for integrity
            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}