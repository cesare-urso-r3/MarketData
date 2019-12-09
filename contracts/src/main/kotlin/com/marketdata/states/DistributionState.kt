package com.marketdata.states

import com.marketdata.contracts.DistributionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(DistributionContract::class)
data class DistributionState(val from : Party,
                             val to: Party,
                             override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<Party> get() = listOf(from, to)
}