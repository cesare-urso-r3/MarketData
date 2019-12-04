package com.marketdata.contracts

import com.marketdata.states.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

class DistributionContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.DistributionContract"
    }

    interface Commands : CommandData {
        class Distribute() : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Distribute -> {
                "No inputs should be consumed when issuing data set." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as DistributionState

                "Sending party needs to sign" using (cmd.signers.toSet() == setOf(outputState.from.owningKey))

                //TODO: Is there any meaningful check on reference states?
            }
        }
    }
}