package com.marketdata.data

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class PricingParameter(val monthlyCostPerUser : Double) {
}