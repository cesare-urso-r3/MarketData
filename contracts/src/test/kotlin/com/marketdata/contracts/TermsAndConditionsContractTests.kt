package com.marketdata.contracts

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.states.SignedTermsAndConditionsState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.node.NotaryInfo
import net.corda.finance.contracts.asset.Obligation
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File

class TermsAndConditionsContractTests {
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
    fun termAndConditionIssue() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                this.verifies()
            }
        }
    }

    @Test
    fun termAndConditionWrongCommand() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), DummyCommand()) // Correct type.
                attachment(attachmentId)
                this.fails()
            }
        }
    }

    @Test
    fun termAndConditionWithInputs() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            val tandc2 = TermsAndConditionsState("StandardTerms2", BOB.party, attachmentId)
            println(tandc)
            transaction {
                input(TermsAndConditionsContract.ID, tandc2)
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                this.`fails with`("No inputs should be consumed when issuing data set.")
            }
        }
    }

    @Test
    fun termAndConditionTooManyOutputs() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            val tandc2 = TermsAndConditionsState("StandardTerms2", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc2)
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                this.`fails with`("Only one output state should be created when issuing terms and conditions.")
            }
        }
    }

    @Test
    fun termAndConditionWrongSigner() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(ALICE.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                this.`fails with`("The issuer must be the one and only signer")
            }
        }
    }

    @Test
    fun termAndConditionEmptyDataSet() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                this.`fails with`("The dataSet name cannot be empty")
            }
        }
    }

    @Test
    fun termAndConditionNoAttachment() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                this.`fails with`("Terms and Conditions must be present")
            }
        }
    }

    @Test
    fun termAndConditionTooManyAttachments() {

        ledgerServices.ledger {
            val attachmentId = attachment(attachmentFile.inputStream())
            val attachmentId2 = attachment(attachmentFile2.inputStream())
            val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
            println(tandc)
            transaction {
                output(TermsAndConditionsContract.ID, tandc)
                command(listOf(BOB.publicKey), TermsAndConditionsContract.Commands.Issue(attachmentId)) // Correct type.
                attachment(attachmentId)
                attachment(attachmentId2)
                this.`fails with`("Only one attachment is permitted")
            }
        }
    }
}
