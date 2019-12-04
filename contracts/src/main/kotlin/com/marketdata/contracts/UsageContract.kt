package com.marketdata.contracts

import com.marketdata.states.DataSetState
import com.marketdata.states.UsageReceiptState
import com.marketdata.states.UsageState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

class UsageContract : Contract {
    companion object {
        const val ID = "com.marketdata.contracts.UsageContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class SendReceipt : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Issue -> {
                "No inputs should be consumed when issuing Permission." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing Permission." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as UsageState

                "The subscriber and provider cannot be the same." using (
                        outputState.subscriber != outputState.provider)

                "Only the subscriber is required to sign" using (
                        cmd.signers.toSet() == setOf(outputState.subscriber.owningKey))

                "The date cannot be empty" using ( outputState.date.isNotEmpty() )

                val dataSet = getDataSetFromUsageState(outputState, tx)

                try {
                    val paidUsageStateAndRef = outputState.paidUsageState?.resolve(tx)

                    if (paidUsageStateAndRef != null) {
                        val paidUsageStateDataSet = getDataSetFromUsageReceiptState(paidUsageStateAndRef.state.data, tx)
                        "Paid permission must be for the correct data set" using ( paidUsageStateDataSet == dataSet )
                    }

                    // TODO: any more validation?

                } catch (e: NoSuchElementException) {
                    // this is ok, it's an optional reference
                }
            }
            is Commands.SendReceipt -> {
                // TODO - basic validation
            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}

fun getDataSetFromUsageState(usage : UsageState, tx : LedgerTransaction) : DataSetState {

    val permissionState = usage.permissionState.resolve(tx).state.data
    val distributableDataSet = permissionState.distributableDataSet.resolve(tx).state.data
    return distributableDataSet.dataSet.resolve(tx).state.data

}

fun getDataSetFromUsageReceiptState(usage : UsageReceiptState, tx : LedgerTransaction) : DataSetState {

    return usage.dataSet.resolve(tx).state.data
}