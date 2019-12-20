package com.marketdata.states

import com.marketdata.contracts.BillingContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(BillingContract::class)
data class BillingState(val startDate : String,
                        val endDate: String,
                        val from: Party,
                        val to : Party,
                        val amount : Double,
                        override val linearId: UniqueIdentifier = UniqueIdentifier())
    : LinearState {

    override val participants: List<Party>
        get() = listOf(from, to)
}