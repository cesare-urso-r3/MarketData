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

class PermissionRequestContractTests {

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
    fun permissionRequest() {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val dataTandCpointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices  = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), dataTandCpointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)

                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)
                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)

                val distPrices  = PricingParameter(13.0)

                val distDataSet = DistributableDataSetState("LSE L1",
                        CHARLIE.party,
                        dataSetPointer,
                        listOf(distPrices),
                        distTandCpointer)

                val distDataSetPointer =
                        LinearPointer(distDataSet.linearId,
                                DistributableDataSetState::class.java)

                val signedDataTandCs =
                        SignedTermsAndConditionsState("StandardTerms", ALICE.party, dataTandCpointer)
                val signedDataTandCsPointer =
                        LinearPointer(signedDataTandCs.linearId, SignedTermsAndConditionsState::class.java)
                val signedDistTandCs =
                        SignedTermsAndConditionsState("DistributableTerms", ALICE.party, distTandCpointer)
                val signedDistTandCsPointer =
                        LinearPointer(signedDistTandCs.linearId, SignedTermsAndConditionsState::class.java)

                reference(DistributableDataSetContract.ID, distDataSet)
                reference(SignedTermsAndConditionsContract.ID, signedDataTandCs)
                reference(SignedTermsAndConditionsContract.ID, signedDistTandCs)
                reference(DataSetContract.ID, dataSet)
                reference(TermsAndConditionsContract.ID, tandc)
                reference(TermsAndConditionsContract.ID, distTandC)

                output(PermissionRequestContract.ID,
                        PermissionRequestState(
                                distDataSetPointer,
                                signedDataTandCsPointer,
                                signedDistTandCsPointer,
                                "LSE L1",
                                BOB.party,
                                ALICE.party,
                                CHARLIE.party)
                        )
                command(listOf(ALICE.publicKey), PermissionRequestContract.Commands.Issue())
                this.verifies()

            }
        }
    }
}
