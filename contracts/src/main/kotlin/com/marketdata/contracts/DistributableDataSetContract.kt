package com.marketdata.contracts

import com.marketdata.states.DistributableDataSetState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

class DistributableDataSetContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.DistributableDataSetContract"
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

                val outputState = tx.outputStates.single() as DistributableDataSetState

                "The issuer must sign" using (
                        cmd.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( outputState.dataSetName.isNotEmpty() )

            }
            is Commands.Revoke -> {

                "No outputs should be present when revoking a data set." using (tx.outputStates.isEmpty())

                val inputState = tx.inputStates.single() as DistributableDataSetState
                "Only one input state should be created when issuing data set." using (tx.inputStates.size == 1)

                "The issuer must sign" using (
                        cmd.signers.toSet() == inputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( inputState.dataSetName.isNotEmpty() )

            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}