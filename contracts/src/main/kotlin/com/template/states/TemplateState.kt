package com.template.states

import com.template.contracts.TemplateContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(TemplateContract::class)
data class TemplateState(val dataset: String,
                         val provider: Party,
                         val subscriber : Party,
                         val dataChargeOwner: Party? = null,
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState {
    override val participants: List<Party> get() = setOf(subscriber, provider, dataChargeOwner).filterNotNull().toList()

    fun withNewDataChargeOwner(newDataChargeOwner: Party): TemplateState {
        return this.copy(dataChargeOwner = newDataChargeOwner)
    }
}
