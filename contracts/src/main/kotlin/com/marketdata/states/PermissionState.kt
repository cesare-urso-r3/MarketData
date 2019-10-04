package com.marketdata.states

import com.marketdata.contracts.PermissionContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(PermissionContract::class)
data class PermissionState(val dataset: String,
                           val provider: Party,
                           val subscriber : Party,
                           val dataChargeOwner: Party? = null,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState {
    override val participants: List<Party> get() = setOf(subscriber, provider, dataChargeOwner).filterNotNull().toList()

    fun withNewDataChargeOwner(newDataChargeOwner: Party): PermissionState {
        return this.copy(dataChargeOwner = newDataChargeOwner)
    }
}
