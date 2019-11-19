package com.marketdata

import com.marketdata.contracts.PermissionContract
import com.marketdata.states.PermissionState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class PermissionContractTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    @Test
    fun `dummy test`() {

    }
}