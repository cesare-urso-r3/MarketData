package com.marketdata.contracts

import com.marketdata.*
import com.marketdata.data.PricingParameter
import com.marketdata.states.*
//import com.sun.org.apache.xpath.internal.operations.Bool
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.node.NotaryInfo
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.*
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.apache.activemq.artemis.core.server.cluster.impl.Redistributor
import org.junit.Test
import java.io.File
import java.security.Permission
import java.security.PublicKey

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

    // TODO: This throws some errors which is down to the reference states and their ability to be verified. Strangely
    // the TX as a whole still verifies, so further investigation is required

    data class BaseTestParams(
            val dataSetName : String = "LSE L1",
            val redistributor: TestIdentity = CHARLIE,
            val provider : TestIdentity = BOB,
            val removeSigner : Boolean = false,
            val addSigner : Boolean = false,
            val dataTandCName : String = "StandardTerms",
            val distTandCName : String = "DistributableTerms",
            val dataTandCParty : TestIdentity = BOB,
            val distTandCParty : TestIdentity = CHARLIE,
            val dataTandCSigner : TestIdentity = ALICE,
            val distTandCSigner: TestIdentity = ALICE,
            val extraOutput : Boolean = false,
            val extraInput : Boolean = false
    )

    fun baseTest(params : BaseTestParams = BaseTestParams(),
                 verificationBlock : TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail) {
        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val dataTandCpointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), dataTandCpointer)

                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)

                val distAttachmentId = attachment(distAttachmentFile.inputStream())
                val distTandC = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)
                val distTandCpointer = LinearPointer(distTandC.linearId, TermsAndConditionsState::class.java)

                val distPrices = PricingParameter(13.0)

                val distDataSet = DistributableDataSetState("LSE L1",
                        CHARLIE.party,
                        BOB.party,
                        dataSetPointer,
                        listOf(distPrices),
                        distTandCpointer)

                val distDataSetPointer =
                        LinearPointer(distDataSet.linearId,
                                DistributableDataSetState::class.java)

                val signedDataTandCs =
                        SignedTermsAndConditionsState(params.dataTandCName, params.dataTandCParty.party, params.dataTandCSigner.party, dataTandCpointer)
                val signedDataTandCsPointer =
                        LinearPointer(signedDataTandCs.linearId, SignedTermsAndConditionsState::class.java)
                val signedDistTandCs =
                        SignedTermsAndConditionsState(params.distTandCName, params.distTandCParty.party, params.distTandCSigner.party, distTandCpointer)
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
                                params.dataSetName,
                                params.provider.party,
                                ALICE.party,
                                params.redistributor.party))

                if (params.extraOutput) {
                    output(PermissionRequestContract.ID,
                            PermissionRequestState(
                                    distDataSetPointer,
                                    signedDataTandCsPointer,
                                    signedDistTandCsPointer,
                                    params.dataSetName,
                                    params.provider.party,
                                    ALICE.party,
                                    params.redistributor.party))
                }

                if (params.extraInput) {
                    input(PermissionRequestContract.ID,
                            PermissionRequestState(
                                    distDataSetPointer,
                                    signedDataTandCsPointer,
                                    signedDistTandCsPointer,
                                    params.dataSetName,
                                    params.provider.party,
                                    ALICE.party,
                                    params.redistributor.party))
                }

                var signers = listOf(ALICE, params.provider, params.redistributor)
                if (params.removeSigner) {
                    signers = signers.dropLast(1)
                }
                if (params.addSigner) {
                    signers += EMPTY_IDENTITY
                }
                command(signers.map { it.publicKey }, PermissionRequestContract.Commands.Issue())
                verificationBlock()
            }
        }
    }

    @Test
    fun permissionRequest() {
       baseTest {
           verifies()
       }
    }

    @Test
    fun permissionRequestEmptyName() {
        baseTest (BaseTestParams(dataSetName = "")) {
            `fails with`("The dataSet name cannot be empty")
        }
    }

    @Test
    fun permissionRequestMissingSigner() {
        baseTest (BaseTestParams(removeSigner = true)) {
            `fails with`("All parties must sign")
        }
    }

    @Test
    fun permissionRequestExtraSigner() {
        baseTest (BaseTestParams(addSigner = true)) {
            `fails with`("All parties must sign")
        }
    }

    @Test
    fun permissionRequestWrongRedistributor() {
        baseTest (BaseTestParams(redistributor = DAN)) {
            `fails with`("The supplied redistributor terms and conditions must be issued by the redistributor")
        }
    }

    @Test
    fun permissionRequestWrongProvider() {
        baseTest (BaseTestParams(provider = DAN)) {
            `fails with`("The supplied data terms and conditions must be issued by the provider")
        }
    }

    @Test
    fun permissionRequestWrongSignedRedistName() {
        baseTest (BaseTestParams(distTandCName = "WrongDist")) {
            `fails with`("The supplied signed redistributor terms and conditions name is incorrect")
        }
    }

    @Test
    fun permissionRequestWrongSignedDataName() {
        baseTest (BaseTestParams(dataTandCName = "WrongData")) {
            `fails with`("The supplied signed provider terms and conditions name is incorrect")
        }
    }

    @Test
    fun permissionRequestWrongSignedRedistParty() {
        baseTest (BaseTestParams(distTandCParty = DAN)) {
            `fails with`("The supplied signed redistributor terms and conditions issuer is incorrect")
        }
    }

    @Test
    fun permissionRequestWrongSignedDataParty() {
        baseTest (BaseTestParams(dataTandCParty = DAN)) {
            `fails with`("The supplied signed provider terms and conditions issuer is incorrect")
        }
    }


    @Test
    fun permissionRequestWrongSignerRedistTerms() {
        baseTest (BaseTestParams(distTandCSigner = DAN)) {
            `fails with`("The redistributor T&Cs must be signed by the subscriber in this request")
        }
    }

    @Test
    fun permissionRequestWrongSignerDataTerms() {
        baseTest (BaseTestParams(dataTandCSigner = DAN)) {
            `fails with`("The data T&Cs must be signed by the subscriber in this request")
        }
    }

    @Test
    fun permissionRequestWrongName() {
        baseTest (BaseTestParams(dataSetName = "WrongDS")) {
            `fails with`("The dataSet requested does not match the provided dataSet details")
        }
    }

    @Test
    fun permissionRequestTooManyOutput() {
        baseTest (BaseTestParams(extraOutput = true)) {
            `fails with`("Only one output state should be created when issuing permission.")
        }
    }

    @Test
    fun permissionRequestTooManyInput() {
        baseTest (BaseTestParams(extraInput = true)) {
            `fails with`("No inputs should be consumed when issuing permission.")
        }
    }
}
