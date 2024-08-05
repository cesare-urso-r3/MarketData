package com.marketdata.contracts


import com.marketdata.states.UsageReceiptState
import com.marketdata.states.UsageState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using

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
                "No inputs should be consumed when issuing Usage." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing Usage." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as UsageState

                "The subscriber and provider cannot be the same." using (
                        outputState.subscriber != outputState.provider)

                "Only the subscriber is required to sign" using (
                        cmd.signers.toSet() == setOf(outputState.subscriber.owningKey))

                "The date cannot be empty" using ( outputState.date.isNotEmpty() )

                "The username cannot be empty" using ( outputState.userName.isNotEmpty() )

                val permissionState = outputState.permissionState.resolveToState(tx)

                "The data set must match the data set on the permission state" using
                        (outputState.dataSetName == permissionState.dataSetName)
                "The provider must match the provider on the permission state" using
                        (outputState.provider == permissionState.provider)
                "The redistributor must match the provider on the permission state" using
                        (outputState.redistributor == permissionState.redistributor)
                "The subscriber must match the subscriber on the permission state" using
                        (outputState.subscriber == permissionState.subscriber)

                var paidUsageStateAndRef : StateAndRef<UsageReceiptState>? = null
                try {
                    paidUsageStateAndRef = outputState.paidUsageState?.resolve(tx)
                } catch (e: NoSuchElementException) {
                    // this is ok, it's an optional reference
                }

                // do this outside the try/catch as we do want to raise errors if anything here throws an exception
                if (paidUsageStateAndRef != null) {
                    val paidUsageStateDataSet = paidUsageStateAndRef.state.data.dataSet.resolveToState(tx)

                    val distributableDataSet = permissionState.distributableDataSet.resolveToState(tx)
                    val dataSet = distributableDataSet.dataSet.resolveToState(tx)
                    "Paid permission must be for the correct data set" using (
                            paidUsageStateDataSet.name == dataSet.name &&
                            paidUsageStateDataSet.provider == dataSet.provider
                            )

                    val paidUsageState = paidUsageStateAndRef.state.data
                    "Paid permission must be for the correct subscriber" using ( paidUsageState.subscriber == outputState.subscriber )
                    "Paid permission must be for the correct provider" using ( paidUsageState.provider == outputState.provider )
                    "Paid permission must be for the correct date" using ( paidUsageState.date == outputState.date )
                    "Paid permission must be for the correct user" using ( paidUsageState.userName == outputState.userName )
                    "Paid permission must be for the correct data set name" using ( paidUsageState.dataSetName == outputState.dataSetName )

                    val tandC = dataSet.termsAndConditions.resolveToState(tx)
                    val signedTandC = paidUsageState.signedTandCs.resolveToState(tx)
                    "Signed Terms and conditions must be for the correct T&C name" using ( signedTandC.name == tandC.name )
                    "Signed Terms and conditions must be for the correct T&C issuer"  using ( signedTandC.issuer == tandC.issuer )
                    "Signed Terms and conditions must be signed by the subscriber"  using ( signedTandC.signer == outputState.subscriber )
                }
            }
            is Commands.SendReceipt -> {
                "No inputs should be consumed when issuing receipt." using (tx.inputStates.isEmpty())

                "Only one output state should be created when issuing receipt." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as UsageReceiptState

                val dataSet = outputState.dataSet.resolveToState(tx)
                val dataTandCs = dataSet.termsAndConditions.resolveToState(tx)
                val signedTandCs = outputState.signedTandCs.resolveToState(tx)

                "The data set name must match that of the provided data set state" using (dataSet.name == outputState.dataSetName)
                "The provider must match that of the provided data set state" using (dataSet.provider == outputState.provider)
                "Incorrect signed T&Cs" using (dataTandCs == signedTandCs.termsAndConditions.resolveToState(tx))

                "All parties are required to sign" using (
                            cmd.signers.toSet() ==
                                listOf(outputState.provider, outputState.subscriber, outputState.redistributor)
                                        .map { it.owningKey }
                                        .toSet()
                        )
            }
            else -> {
                throw IllegalArgumentException("Unknown command: ${this.toString()}")
            }
        }
    }
}