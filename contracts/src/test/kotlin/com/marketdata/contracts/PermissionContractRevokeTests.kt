//TODO: Can be deleted

package com.marketdata.contracts
//
//import com.marketdata.ALICE
//import com.marketdata.BOB
//import com.marketdata.CHARLIE
//import com.marketdata.MEGACORP
//import com.marketdata.MINICORP
//import com.marketdata.states.PermissionState
//import net.corda.core.contracts.TypeOnlyCommandData
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import org.junit.Test
//
//class PermissionContractRevokeTests {
//    private val ledgerServices = MockServices()
//
//    class DummyCommand : TypeOnlyCommandData()
//
//    private val cmd = PermissionContract.Commands.Revoke()
//
//    @Test
//    fun vanilla2Party() {
//
//        ledgerServices.ledger {
//            transaction {
//                input(PermissionContract.ID, PermissionState("data_set", ALICE.party, BOB.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey), cmd)
//                this.verifies()
//            }
//            // explicit null
//            transaction {
//                input(PermissionContract.ID,  PermissionState("data_set", ALICE.party, BOB.party, null))
//                command(listOf(ALICE.publicKey, BOB.publicKey), cmd)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun vanilla3Party() {
//        ledgerServices.ledger {
//            transaction {
//                input(PermissionContract.ID, PermissionState("data_set", ALICE.party, BOB.party, CHARLIE.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey, CHARLIE.publicKey), cmd)
//                this.verifies()
//            }
//            transaction {
//                input(PermissionContract.ID, PermissionState("data_set", ALICE.party, BOB.party, ALICE.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey), cmd)
//                this.verifies()
//            }
//            transaction {
//                input(PermissionContract.ID, PermissionState("data_set", ALICE.party, BOB.party, BOB.party))
//                command(listOf(ALICE.publicKey, BOB.publicKey), cmd)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun mustIncludeRevokeCommand() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                input(PermissionContract.ID, perm)
//                command(listOf(ALICE.publicKey, BOB.publicKey), DummyCommand()) // Wrong type.
//                this.fails()
//            }
//            transaction {
//                input(PermissionContract.ID, perm)
//                command(listOf(ALICE.publicKey, BOB.publicKey), cmd)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun noOutputsAllowed() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm)
//                this `fails with` "No outputs should be included when revoking permission."
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
//                this `fails with` "Only one input state should be present when revoking Permission."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign1() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                this `fails with` "All parties involved must sign permission issue transaction."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign2() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
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
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
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
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
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
//                command(listOf(ALICE.publicKey, BOB.publicKey),cmd)
//                input(PermissionContract.ID, perm)
//                this.verifies()
//            }
//        }
//    }
//}
