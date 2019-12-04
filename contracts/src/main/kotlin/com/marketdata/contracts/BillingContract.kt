package com.marketdata.contracts

import com.marketdata.states.BillingState
import com.marketdata.states.DataSetState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.LedgerTransaction

class BillingContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.BillingContract"
    }

    interface Commands : CommandData {
        class Create() : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Create -> {
                "No inputs should be consumed when issuing data set." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as BillingState

                "The from party must sign" using (
                        cmd.signers.toSet() == setOf(outputState.from.owningKey))

                // TODO: validate the data set is correctly named

            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}