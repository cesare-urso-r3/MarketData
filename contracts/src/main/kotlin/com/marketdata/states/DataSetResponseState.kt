package com.marketdata.states

// TODO: probably not necessary

import com.marketdata.contracts.DataSetContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party

@BelongsToContract(DataSetContract::class)
class DataSetResponseState(val requestor : Party,
                           val requestee : Party,
                           val dataSets : List<StateRef>,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    // TODO: should this be StatePointers rather than StateRef ??
    override val participants: List<Party> get() = listOf(requestor, requestee)
}