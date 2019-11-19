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

class SignedTermsAndConditionsContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.SignedTermsAndConditionsContract"
    }

    interface Commands : CommandData {
        class Issue() : Commands
        class Revoke() :Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Issue -> {
                "No inputs should be consumed when issuing data set." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing data set." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as SignedTermsAndConditionsState

                "The issuer must sign" using (
                        cmd.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( outputState.name.isNotEmpty() )

                val tandc = outputState.termsAndConditions.resolve(tx).state.data

                "The specified name does not match the attached terms and conditions" using (tandc.name == outputState.name)

            }
            is Commands.Revoke -> {

                "No outputs should be present when revoking a T&C." using (tx.outputStates.isEmpty())

                val inputState = tx.inputStates.single() as SignedTermsAndConditionsState
                "Only one input state should be created when issuing data set." using (tx.inputStates.size == 1)

                "The issuer must sign" using (
                        cmd.signers.toSet() == inputState.participants.map { it.owningKey }.toSet())

                "The name cannot be empty" using ( inputState.name.isNotEmpty() )

            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}