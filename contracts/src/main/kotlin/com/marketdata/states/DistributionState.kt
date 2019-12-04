package com.marketdata.states

import com.marketdata.contracts.DistributionContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party

@BelongsToContract(DistributionContract::class)
data class DistributionState(val from : Party,
                             val to: Party,
                             override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<Party> get() = listOf(from, to)
}