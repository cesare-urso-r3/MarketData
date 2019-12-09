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
        class Revoke : TypeOnlyCommandData(), Commands // TODO: Implement
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Issue -> {
                "No inputs should be consumed when issuing data set." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as PermissionRequestState

                "All parties must sign" using (
                        cmd.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( outputState.dataSetName.isNotEmpty() )

                val distDataSet = outputState.distributableDataSet.resolveToState(tx)
                val dataSet = distDataSet.dataSet.resolveToState(tx)

                val distTandCs = distDataSet.termsAndConditions.resolveToState(tx)
                val dataTandCs = dataSet.termsAndConditions.resolveToState(tx)

                val signedDistTandCs = outputState.redistributorTandCs.resolveToState(tx)
                val signedDataTandCs = outputState.providerTandCs.resolveToState(tx)

                "The supplied redistributor terms and conditions must be issued by the redistributor" using
                        (distTandCs.issuer == outputState.redistributor)
                "The supplied data terms and conditions must be issued by the provider" using
                        (dataTandCs.issuer == outputState.provider)

                "The supplied signed redistributor terms and conditions name is incorrect" using
                        (signedDistTandCs.name == distTandCs.name)
                "The supplied signed redistributor terms and conditions issuer is incorrect" using
                        (signedDistTandCs.issuer == distTandCs.issuer)
                "The supplied signed provider terms and conditions name is incorrect" using
                        (signedDataTandCs.name == dataTandCs.name)
                "The supplied signed provider terms and conditions issuer is incorrect" using
                        (signedDataTandCs.issuer == dataTandCs.issuer)

                "The redistributor T&Cs must be signed by the subscriber in this request" using
                        (signedDistTandCs.signer == outputState.subscriber)
                "The data T&Cs must be signed by the subscriber in this request" using
                        (signedDataTandCs.signer == outputState.subscriber)

                "The dataSet requested does not match the provided dataSet details" using
                        ( dataSet.name == outputState.dataSetName )
            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}