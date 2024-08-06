package com.marketdata.contracts

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.CHARLIE
import com.marketdata.states.SignedTermsAndConditionsState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.node.NotaryInfo
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File

class SignedTermsAndConditionsContractTests {
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
    val attachmentFile2 = File("src/test/resources/prices.zip")


    @Test
    fun signedTermAndConditionIssue() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, pointer))
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), SignedTermsAndConditionsContract.Commands.Issue()) // Correct type.
                this.verifies()
            }
        }
    }

    @Test
    fun signedTermAndConditionWrongCommand() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, pointer))
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), DummyCommand()) // Correct type.
                this.fails()
            }
        }
    }

    @Test
    fun signedTermAndConditionWithInputs() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
            val tandc2 = TermsAndConditionsState("StandardTerms2", BOB.party, attachmentId)
            val pointer2 = LinearPointer(tandc2.linearId, TermsAndConditionsState::class.java)
            println(tandc)
            transaction {
                input(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("StandardTerms2", tandc2.issuer, ALICE.party, pointer2))
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, pointer))
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), SignedTermsAndConditionsContract.Commands.Issue()) // Correct type.
                this.`fails with`("No inputs should be consumed when issuing data set.")
            }
        }
    }

    @Test
    fun signedTermAndConditionTooManyOutputs() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
            val tandc2 = TermsAndConditionsState("StandardTerms2", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc2)
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, pointer))
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                this.`fails with`("Only one output state should be created when issuing terms and conditions.")
            }
        }
    }

    @Test
    fun signedTermAndConditionWrongSigner() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("ERROR", tandc.issuer, ALICE.party, pointer))
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), SignedTermsAndConditionsContract.Commands.Issue()) // Correct type.
                this.`fails with`("The specified name does not match the attached terms and conditions")
            }
        }
    }

    @Test
    fun signedTermAndConditionWrongIssuer() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("StandardTerms", CHARLIE.party, ALICE.party, pointer))
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), SignedTermsAndConditionsContract.Commands.Issue()) // Correct type.
                this.`fails with`("The specified issuer does not match the attached terms and conditions issuer")
            }
        }
    }

    @Test
    fun signedTermAndConditionEmptyDataSet() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                output(SignedTermsAndConditionsContract.ID, SignedTermsAndConditionsState("", tandc.issuer, ALICE.party, pointer))
                reference(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), SignedTermsAndConditionsContract.Commands.Issue()) // Correct type.
                this.`fails with`("The dataSet name cannot be empty")
            }
        }
    }
}
