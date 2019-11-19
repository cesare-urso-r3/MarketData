package com.marketdata.contracts

import com.marketdata.states.DataSetResponseState
import com.marketdata.states.DataSetState
import com.marketdata.states.UsageState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.LedgerTransaction
import java.util.jar.JarInputStream

class DataSetContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.DataSetContract"
    }

    interface Commands : CommandData {
        class Issue() : TypeOnlyCommandData(), Commands
        class Revoke : TypeOnlyCommandData(), Commands
        // TODO: class Revoke : TypeOnlyCommandData(), Commands
    }

    abstract class CommandWithAttachmentId(val attachmentId: AttachmentId) : CommandData {
        override fun equals(other: Any?) = other?.javaClass == javaClass
        override fun hashCode() = javaClass.name.hashCode()
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Issue -> {
                "No inputs should be consumed when issuing data set." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as DataSetState

                "The issuer must sign" using (
                        cmd.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( outputState.name.isNotEmpty() )

                // TODO: validate the data set is correctly named

            }
            is Commands.Revoke -> {

                "No outputs should be present when revoking a data set." using (tx.outputStates.isEmpty())

                val inputState = tx.inputStates.single() as DataSetState
                "Only one input state should be created when issuing data set." using (tx.inputStates.size == 1)

                "The issuer must sign" using (
                        cmd.signers.toSet() == inputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( inputState.name.isNotEmpty() )

            }
//            is Commands.List -> {
//                "No inputs should be consumed when browsing a data set." using (tx.inputStates.isEmpty())
//
//                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)
//
//                "Only one output state should be created when issuing data set." using (
//                        tx.outputStates.single() is DataSetResponseState)
//
//                val outputState = tx.outputStates.single() as DataSetResponseState
//
//                "All referenced states must be DataSetStates" using (
//                            tx.referenceStates.all { it is DataSetState }
//                        )
//
//            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}