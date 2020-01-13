package com.marketdata.contracts

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.data.PricingParameter
import com.marketdata.states.DataSetState
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

class DataSetContractTests {
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
    fun dataSetIssue() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                output(DataSetContract.ID, DataSetState("LSE L1", BOB.party, listOf(prices), pointer))
                command(listOf(BOB.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                reference(TermsAndConditionsContract.ID, tandc)
                this.verifies()
            }
        }
    }

    @Test
    fun dataSetNoInput() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val attachmentId2 = attachment(attachmentFile2.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val tandc2 = TermsAndConditionsState("StandardTerms2", ALICE.party, attachmentId2)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val pointer2 = LinearPointer(tandc2.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                input(DataSetContract.ID, DataSetState("LSE L1", BOB.party, listOf(prices), pointer))
                output(DataSetContract.ID, DataSetState("LSE L1", ALICE.party, listOf(prices), pointer2))
                command(listOf(BOB.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("No inputs should be consumed when issuing data set.")
            }
        }
    }

    @Test
    fun dataSetTwoOutput() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val attachmentId2 = attachment(attachmentFile2.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val tandc2 = TermsAndConditionsState("StandardTerms2", ALICE.party, attachmentId2)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val pointer2 = LinearPointer(tandc2.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                output(DataSetContract.ID, DataSetState("LSE L1", BOB.party, listOf(prices), pointer))
                output(DataSetContract.ID, DataSetState("LSE L1", ALICE.party, listOf(prices), pointer2))
                command(listOf(BOB.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("Only one output state should be created when issuing data set.")
            }
        }
    }

    @Test
    fun dataSetIssueTooManySigners() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                output(DataSetContract.ID, DataSetState("LSE L1", BOB.party, listOf(prices), pointer))
                command(listOf(ALICE.publicKey, BOB.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("Only the provider must sign")
            }
        }
    }

    @Test
    fun dataSetIssueMissingProviderSigner() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                output(DataSetContract.ID, DataSetState("LSE L1", BOB.party, listOf(prices), pointer))
                command(listOf(ALICE.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("Only the provider must sign")
            }
        }
    }

    @Test
    fun dataSetEmptyName() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                output(DataSetContract.ID, DataSetState("", BOB.party, listOf(prices), pointer))
                command(listOf(BOB.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("The dataSet name cannot be empty")
            }
        }
    }

    @Test
    fun dataSetWrongTandCIssuer() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)
                val prices = PricingParameter(10.0)
                output(DataSetContract.ID, DataSetState("LSE L1", ALICE.party, listOf(prices), pointer))
                command(listOf(ALICE.publicKey), DataSetContract.Commands.Issue()) // Correct type.
                reference(TermsAndConditionsContract.ID, tandc)
                this.`fails with`("The terms and conditions must be issued by the provider")
            }
        }
    }
}