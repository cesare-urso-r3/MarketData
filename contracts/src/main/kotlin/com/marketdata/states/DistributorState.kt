package com.marketdata.states

import com.marketdata.contracts.DistributorContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class DistributorContractState {
    PROPOSED, SIGNED
}

@BelongsToContract(DistributorContract::class)
data class DistributorState(val provider: Party,
                       val redistributor: Party,
                       val termsAndConditions: LinearPointer<SignedTermsAndConditionsState>,
                       val contractState: DistributorContractState = DistributorContractState.PROPOSED,
                       override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState  { //QueryableState

    override val participants: List<Party> get() = listOf(provider, redistributor)

     fun withSignedStatus() : DistributorState {
        return this.copy(contractState = DistributorContractState.SIGNED, linearId = linearId)
    }
}