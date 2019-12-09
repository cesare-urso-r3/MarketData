package com.marketdata.contracts

import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.transactions.LedgerTransaction


fun <T : LinearState> LinearPointer<T>.resolveToState(tx: LedgerTransaction) : T {
    return this.resolve(tx).state.data
}