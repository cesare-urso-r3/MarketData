package com.marketdata.contracts

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.CHARLIE
import com.marketdata.MEGACORP
import com.marketdata.MINICORP
import com.marketdata.states.*
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StatePointer
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.node.NotaryInfo
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.contracts.DummyContract
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File
import java.security.Permission

class DistributorTests {
    val DUMMY_NOTARY = TestIdentity(DUMMY_NOTARY_NAME, 20).party
    private val ledgerServices = MockServices(
            ALICE,
            networkParameters = testNetworkParameters(
                    minimumPlatformVersion = 4,
                    notaries = listOf(NotaryInfo(DUMMY_NOTARY, true))
            )
    )

    class DummyCommand : TypeOnlyCommandData()
    val attachmentFile = File("src/test/resources/DemoT&C.zip")


    @Test
    fun distributorIssue() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("DistributorTerms", BOB.party, attachmentId)
                val tandCpointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val signedDataTandCs =
                        SignedTermsAndConditionsState("StandardTerms", CHARLIE.party, tandCpointer)
                val signedDataTandCsPointer =
                        LinearPointer(signedDataTandCs.linearId, SignedTermsAndConditionsState::class.java)

                output(DistributorContract.ID, DistributorState(BOB.party, CHARLIE.party, signedDataTandCsPointer))

                reference(SignedTermsAndConditionsContract.ID, signedDataTandCs)
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(CHARLIE.publicKey, BOB.publicKey), DistributorContract.Commands.Issue()) // Correct type.

                this.verifies()
            }
        }
    }

//    fun permissionState() {
//
//        ledgerServices.ledger {
//            transaction {
//                val dsState = DataSetState("data_set", BOB.party).linearId
//                output(PermissionContract.ID, PermissionState("data_set", dsState, BOB.party, ALICE.party, CHARLIE.party))
//                command(listOf(BOB.publicKey), DataSetContract.Commands.Issue()) // Correct type.
//                reference(DataSetContract.ID, dsState)
//                this.verifies()
//            }
//        }
//    }


//    @Test
//    fun mustIncludeIssueCommand() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                output(PermissionContract.ID, perm)
//                command(listOf(ALICE.publicKey, BOB.publicKey), DummyCommand()) // Wrong type.
//// attachment(attachment(attachmentFile.inputStream()))
//                this.fails()
//            }
//            transaction {
//                output(PermissionContract.ID, perm)
//                command(listOf(ALICE.publicKey, BOB.publicKey), PermissionContract.Commands.Issue()) // Correct type.
//// attachment(attachment(attachmentFile.inputStream()))
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun noInputsAllowed() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                input(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm)
//                this `fails with` "No inputs should be consumed when issuing Permission."
//            }
//        }
//    }
//
//    @Test
//    fun singleOutputAllowed() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
//                output(PermissionContract.ID, perm)
//                this `fails with` "Only one output state should be created when issuing Permission."
//            }
//        }
//    }
//
//    @Test
//    fun allPartiesMustSign1() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
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
//                command(listOf(BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
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
//                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
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
//                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
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
//                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun subscriberAndProviderCannotBeTheSame() {
//        val perm = PermissionState("data_set", ALICE.party, BOB.party)
//        val subIsProv = PermissionState("data_set", ALICE.party, ALICE.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, subIsProv)
//                this `fails with` "The subscriber and provider cannot be the same."
//            }
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey), PermissionContract.Commands.Issue())
//// attachment(attachment(attachmentFile.inputStream()))
//                output(PermissionContract.ID, perm)
//                this.verifies()
//            }
//        }
//    }
}
