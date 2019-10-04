package com.marketdata.states

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.CHARLIE
import com.marketdata.MEGACORP
import com.marketdata.MINICORP
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.testing.node.MockServices
import org.junit.Test
import kotlin.test.assertEquals

class PermissionStateTests {
    private val ledgerServices = MockServices()

    // helper
    fun assertSortedListsAreEqual (listA : List<Any>, listB : List<Any>) {
        assertEquals(listA.map{it.toString()}.toTypedArray().sort(), listB.map{it.toString()}.toTypedArray().sort())
    }

    @Test
    fun isLinearState() {
        assert(LinearState::class.java.isAssignableFrom(PermissionState::class.java))
    }

    @Test
    fun hasLinearIdFieldOfCorrectType() {
        // Does the linearId field exist?
        PermissionState::class.java.getDeclaredField("linearId")
        // Is the linearId field of the correct type?
        assertEquals(PermissionState::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
    }

    @Test
    fun hasDataChargeOwnerFieldOfCorrectType() {
        // Does the amount field exist?
        PermissionState::class.java.getDeclaredField("dataChargeOwner")
        // Is the amount field of the correct type?
        assertEquals(PermissionState::class.java.getDeclaredField("dataChargeOwner").type, Party::class.java)
    }

    @Test
    fun hasSubscriberOwnerFieldOfCorrectType() {
        // Does the amount field exist?
        PermissionState::class.java.getDeclaredField("subscriber")
        // Is the amount field of the correct type?
        assertEquals(PermissionState::class.java.getDeclaredField("subscriber").type, Party::class.java)
    }
    @Test
    fun hasProviderOwnerFieldOfCorrectType() {
        // Does the amount field exist?
        PermissionState::class.java.getDeclaredField("provider")
        // Is the amount field of the correct type?
        assertEquals(PermissionState::class.java.getDeclaredField("provider").type, Party::class.java)
    }
    @Test
    fun hasDataSetFieldOfCorrectType() {
        // Does the amount field exist?
        PermissionState::class.java.getDeclaredField("dataset")
        // Is the amount field of the correct type?
        assertEquals(PermissionState::class.java.getDeclaredField("dataset").type, String::class.java)
    }

    @Test
    fun partiesAreParticipants() {
        val permissionState = PermissionState("dataset", ALICE.party, BOB.party, null)
        assertSortedListsAreEqual(permissionState.participants, listOf(ALICE.party, BOB.party))
    }

    @Test
    fun partiesAreParticipants1() {
        val permissionState = PermissionState("dataset", ALICE.party, BOB.party, CHARLIE.party)
        assertSortedListsAreEqual(permissionState.participants, listOf(ALICE.party, BOB.party, CHARLIE.party))
    }

    @Test
    fun partiesAreParticipants2() {
        val permissionState = PermissionState("dataset", ALICE.party, BOB.party, ALICE.party)
        assertSortedListsAreEqual(permissionState.participants, listOf(ALICE.party, BOB.party))
    }

    @Test
    fun checkIOUStateParameterOrdering() {
        val fields = PermissionState::class.java.declaredFields
        val datasetIdx = fields.indexOf(PermissionState::class.java.getDeclaredField("dataset"))
        val providerIdx = fields.indexOf(PermissionState::class.java.getDeclaredField("provider"))
        val subscriberIdx = fields.indexOf(PermissionState::class.java.getDeclaredField("subscriber"))
        val dataChargeOwnerIdx = fields.indexOf(PermissionState::class.java.getDeclaredField("dataChargeOwner"))
        val linearIdIdx = fields.indexOf(PermissionState::class.java.getDeclaredField("linearId"))

        assert(datasetIdx < providerIdx)
        assert(providerIdx < subscriberIdx)
        assert(subscriberIdx < dataChargeOwnerIdx)
        assert(dataChargeOwnerIdx < linearIdIdx)
    }

    @Test
    fun checkWithNewLenderHelperMethod1() {
        val permissionState = PermissionState("data_set", ALICE.party, BOB.party)
        assertEquals(null, permissionState.dataChargeOwner)
        assertEquals(MINICORP.party, permissionState.withNewDataChargeOwner(MINICORP.party).dataChargeOwner)
        assertEquals(MEGACORP.party, permissionState.withNewDataChargeOwner(MEGACORP.party).dataChargeOwner)
    }

    @Test
    fun checkWithNewLenderHelperMethod2() {
        val permissionState = PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party)
        assertEquals(CHARLIE.party, permissionState.dataChargeOwner)
        assertEquals(MINICORP.party, permissionState.withNewDataChargeOwner(MINICORP.party).dataChargeOwner)
        assertEquals(MEGACORP.party, permissionState.withNewDataChargeOwner(MEGACORP.party).dataChargeOwner)
    }
}