package com.marketdata.contracts

import com.marketdata.states.DataSetResponseState
import com.marketdata.states.DataSetState
import com.marketdata.states.TermsAndConditionsState
import com.marketdata.states.UsageState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.LedgerTransaction
import java.util.jar.JarInputStream

class TermsAndConditionsContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.TermsAndConditionsContract"
    }

    interface Commands : CommandData {
        class Issue(attachmentId: AttachmentId) : CommandWithAttachmentId(attachmentId), Commands
        class Revoke() :Commands
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

                val outputState = tx.outputStates.single() as TermsAndConditionsState

                "The issuer must sign" using (
                        cmd.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())

                "The dataSet name cannot be empty" using ( outputState.name.isNotEmpty() )

                try {
                    tx.getAttachment(outputState.termsAndConditions)
                } catch (e: NoSuchElementException) {
                    throw IllegalArgumentException("Terms and Conditions must be present")
                }
            }
            is Commands.Revoke -> {

                "No outputs should be present when revoking a T&C." using (tx.outputStates.isEmpty())

                val inputState = tx.inputStates.single() as DataSetState
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