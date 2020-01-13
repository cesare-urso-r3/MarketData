package com.marketdata.contracts

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.CHARLIE
import com.marketdata.MEGACORP
import com.marketdata.MINICORP
import com.marketdata.data.PricingParameter
import com.marketdata.states.*
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
import java.security.Permission

class DistributableDataSetContractTests {
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
    val distAttachmentFile = File("src/test/resources/DistributableT&C.zip")

    @Test
    fun distributableDataSetIssue() {

        ledgerServices.ledger {


            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices = PricingParameter(13.0)

                reference(DataSetContract.ID, dataSet)

                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L1",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.verifies()
            }
        }
    }

    @Test
    fun distributableDataSetInvalidInput() {

        ledgerServices.ledger {


            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices = PricingParameter(13.0)

                reference(DataSetContract.ID, dataSet)

                val tandc2 = TermsAndConditionsState("StandardTerms2", BOB.party, attachmentId)
                val pointer2 = LinearPointer(tandc2.linearId, TermsAndConditionsState::class.java)
                val prices2 = PricingParameter(10.0)
                val dataSet2 = DataSetState("LSE L2", BOB.party, listOf(prices2), pointer2)
                val dataSetPointer2 = LinearPointer(dataSet2.linearId, DataSetState::class.java)

                input(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L2",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer2,
                                listOf(distPrices),
                                distTandCpointer))
                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L1",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("No inputs should be consumed when issuing data set.")
            }
        }
    }

    @Test
    fun distributableDataSetTooManyOutput() {

        ledgerServices.ledger {


            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices = PricingParameter(13.0)

                reference(DataSetContract.ID, dataSet)

                val tandc2 = TermsAndConditionsState("StandardTerms2", BOB.party, attachmentId)
                val pointer2 = LinearPointer(tandc2.linearId, TermsAndConditionsState::class.java)
                val prices2 = PricingParameter(10.0)
                val dataSet2 = DataSetState("LSE L2", BOB.party, listOf(prices2), pointer2)
                val dataSetPointer2 = LinearPointer(dataSet2.linearId, DataSetState::class.java)

                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L2",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer2,
                                listOf(distPrices),
                                distTandCpointer))
                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L1",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("Only one output state should be created when issuing data set.")
            }
        }
    }

    @Test
    fun distributableDataSetEmptyName() {

        ledgerServices.ledger {


            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices = PricingParameter(13.0)

                reference(DataSetContract.ID, dataSet)

                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("The dataSet name cannot be empty")
            }
        }
    }

    @Test
    fun distributableDataSetWrongName() {

        ledgerServices.ledger {


            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices = PricingParameter(13.0)

                reference(DataSetContract.ID, dataSet)

                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("Wrong Name",
                                BOB.party,
                                CHARLIE.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("The data set name Wrong Name does not match the supplied data set LSE L1")
            }
        }
    }

    @Test
    fun distributableDataSetWrongProvider() {

        ledgerServices.ledger {


            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val pointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices = PricingParameter(13.0)

                reference(DataSetContract.ID, dataSet)

                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L1",
                                ALICE.party,
                                CHARLIE.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.`fails with`("The data set provider O=Alice, L=TestLand, C=US does not match the supplied data set O=Bob, L=TestCity, C=US")
            }
        }
    }
}