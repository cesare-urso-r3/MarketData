package com.marketdata.data

import net.corda.core.serialization.CordaSerializable

// class to encapsulate pricing
@CordaSerializable
data class PricingParameter(val monthlyCostPerUser : Double)