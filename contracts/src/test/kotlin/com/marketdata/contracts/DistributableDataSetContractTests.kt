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
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File
import java.security.Permission

class DistributableDataSetContractTests {
    private val ledgerServices = MockServices()

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
                val prices  = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), pointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)
                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)

                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)
                val distPrices  = PricingParameter(13.0)


                output(DistributableDataSetContract.ID,
                        DistributableDataSetState("LSE L1",
                                CHARLIE.party,
                                BOB.party,
                                dataSetPointer,
                                listOf(distPrices),
                                distTandCpointer))
                command(listOf(CHARLIE.publicKey), DistributableDataSetContract.Commands.Issue()) // Correct type.
                this.verifies()
            }
        }
    }
}
