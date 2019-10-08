package com.marketdata.contracts

import com.marketdata.states.PermissionState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction

class PermissionContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.PermissionContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Revoke : TypeOnlyCommandData(), Commands
        class ChangeDataChargeOwner : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        requireThat{
            val cmd = tx.commands.requireSingleCommand<Commands>()

            when (cmd.value) {
                is Commands.Issue -> {

                    "No inputs should be consumed when issuing Permission." using (tx.inputStates.isEmpty())

                    "Only one output state should be created when issuing Permission." using (tx.outputStates.size == 1)

                    val outputState = tx.outputStates.single() as PermissionState

                    "The subscriber and provider cannot be the same." using (
                            outputState.subscriber != outputState.provider)

                    "All parties involved must sign permission issue transaction." using (
                            cmd.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())
                }
                is Commands.Revoke -> {

                    "No outputs should be included when revoking permission." using (tx.outputStates.isEmpty())

                    "Only one input state should be present when revoking Permission." using (tx.inputStates.size == 1)
                    val inputState = tx.inputStates.single() as PermissionState

                    "All parties involved must sign permission issue transaction." using (
                            cmd.signers.toSet() == inputState.participants.map { it.owningKey }.toSet())
                }
                is Commands.ChangeDataChargeOwner -> {
                    "Only one output state should be present when changing data owner." using (tx.outputStates.size == 1)
                    "Only one input state should be present when changing data owner." using (tx.inputStates.size == 1)

                    val inputState  = tx.inputStates.single() as PermissionState
                    val outputState = tx.outputStates.single() as PermissionState

                    "Cannot change data owner to the existing owner" using (inputState.dataChargeOwner != outputState.dataChargeOwner)

                    val cpyInputState = inputState.copy(dataChargeOwner = outputState.dataChargeOwner)

                    "Only the data charge owner is permitted to change" using (outputState == cpyInputState)
                }
                else -> {
                    throw IllegalArgumentException("Unknown command: ${this.toString()}")
                }
            }
        }
    }
}