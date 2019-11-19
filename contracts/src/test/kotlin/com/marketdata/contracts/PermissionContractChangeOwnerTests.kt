package com.marketdata.contracts

// TODO: can be deleted
//
//import com.marketdata.ALICE
//import com.marketdata.BOB
//import com.marketdata.CHARLIE
//import com.marketdata.DAN
//import com.marketdata.MEGACORP
//import com.marketdata.MINICORP
//import com.marketdata.states.PermissionState
//import net.corda.core.contracts.TypeOnlyCommandData
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import org.junit.Test
//
//class PermissionContractChangeOwnerTests {
//    private val ledgerServices = MockServices()
//
//    class DummyCommand : TypeOnlyCommandData()
//
//    private val cmd = PermissionContract.Commands.ChangeDataChargeOwner()
//
//    @Test
//    fun vanilla2Party() {
//        ledgerServices.ledger {
//            val perm = PermissionState("data_set", ALICE.party, BOB.party)
//            transaction {
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey, DAN.publicKey), cmd)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun vanilla3Party() {
//        ledgerServices.ledger {
//            val perm = PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party)
//            transaction {
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey, CHARLIE.publicKey, DAN.publicKey), cmd)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun mustIncludeChangeDataChargeOwnerCommand() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey, DAN.publicKey), DummyCommand()) // Wrong type.
//                this.fails()
//            }
//            transaction {
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey, DAN.publicKey), cmd)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun singleInputAllowed() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this `fails with` "Only one input state should be present when changing data owner."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign1() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, DAN.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this `fails with` "Only one output state should be present when changing data owner."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign2() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(BOB.publicKey, DAN.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this `fails with` "All parties involved must sign permission issue transaction."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign3() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey, DAN.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this `fails with` "All parties involved must sign permission issue transaction."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign4() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party, ALICE.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey, DAN.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign5() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey, DAN.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign6() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey, CHARLIE.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.withNewDataChargeOwner(DAN.party))
//                this `fails with` "All parties involved must sign permission issue transaction."
//            }
//        }
//    }
//
//    @Test
//    fun dataOwnerMustChange() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm)
//                this `fails with` "Cannot change data owner to the existing owner"
//            }
//        }
//    }
//
//    @Test
//    fun dataDetailsCannotChange() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.copy(dataChargeOwner = DAN.party, dataset = "new_data_set"))
//                this `fails with` "Only the data charge owner is permitted to change"
//            }
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.copy(dataChargeOwner = DAN.party, provider = DAN.party))
//                this `fails with` "Only the data charge owner is permitted to change"
//            }
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm.copy(dataChargeOwner = DAN.party, subscriber = DAN.party))
//                this `fails with` "Only the data charge owner is permitted to change"
//            }
//        }
//    }
//}
