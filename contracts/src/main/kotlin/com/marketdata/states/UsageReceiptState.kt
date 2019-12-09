package com.marketdata.states

import com.marketdata.contracts.UsageContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// TODO: need to think about dates and timezones

@BelongsToContract(UsageContract::class)
class UsageReceiptState(val dataSet : LinearPointer<DataSetState>,
                        val signedTandCs : LinearPointer<SignedTermsAndConditionsState>,
                        val dataSetName : String,
                        val provider: Party,
                        val redistributor : AbstractParty,
                        val subscriber: Party,
                        val userName: String,
                        val date : String,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState { // QueryableState
    override val participants: List<AbstractParty> get() = setOf(provider, subscriber, redistributor).toList()

    override fun toString(): String {
        return stateToString(
                mapOf(
                        "Subscriber" to subscriber.name.toString(),
                        "DataSet" to dataSetName,
                        "Provider" to provider.name.toString(),
                        "Redistributor" to redistributor.toString()
                )
        )
    }
}