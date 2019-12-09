package com.marketdata.states

import net.corda.core.contracts.ContractState

// helper for easier logging of states and selected name/value outputs
fun ContractState.stateToString(elements : Map<String,String>) : String {
    return "[${this.javaClass.name}] ${elements}"
}