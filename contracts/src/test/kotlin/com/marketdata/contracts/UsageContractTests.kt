package com.marketdata.contracts

import com.marketdata.*
import com.marketdata.data.PricingParameter
import com.marketdata.states.*
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.node.NotaryInfo
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.EnforceVerifyOrFail
import net.corda.testing.dsl.TransactionDSL
import net.corda.testing.dsl.TransactionDSLInterpreter
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File
import java.time.LocalDate

data class BaseTestParams(
        val dataSetName : String = "LSE L1",
        val redistributor: TestIdentity = CHARLIE,
        val provider : TestIdentity = BOB,
        val addSigner : Boolean = false,
        val wrongSigner : Boolean = false,
        val subscriber : TestIdentity = ALICE,
        val extraOutput : Boolean = false,
        val extraInput : Boolean = false,
        val date : String = LocalDate.now().toString(),
        val user : String = "Adam"
)

class UsageContractTests {

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

    fun baseTest(params : BaseTestParams = BaseTestParams(),
                 verificationBlock : TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail) {

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
                        BOB.party,
                        dataSetPointer,
                        listOf(distPrices),
                        distTandCpointer)

                val distDataSetPointer =
                        LinearPointer(distDataSet.linearId,
                                DistributableDataSetState::class.java)

                val signedDataTandCs =
                        SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, dataTandCpointer)
                val signedDataTandCsPointer =
                        LinearPointer(signedDataTandCs.linearId, SignedTermsAndConditionsState::class.java)
                val signedDistTandCs =
                        SignedTermsAndConditionsState("DistributableTerms", distTandC.issuer, ALICE.party, distTandCpointer)
                val signedDistTandCsPointer =
                        LinearPointer(signedDistTandCs.linearId, SignedTermsAndConditionsState::class.java)

                reference(DistributableDataSetContract.ID, distDataSet)
                reference(SignedTermsAndConditionsContract.ID, signedDataTandCs)
                reference(SignedTermsAndConditionsContract.ID, signedDistTandCs)
                reference(DataSetContract.ID, dataSet)
                reference(TermsAndConditionsContract.ID, tandc)
                reference(TermsAndConditionsContract.ID, distTandC)

                val permission =  PermissionRequestState(
                        distDataSetPointer,
                        signedDataTandCsPointer,
                        signedDistTandCsPointer,
                        "LSE L1",
                        BOB.party,
                        ALICE.party,
                        CHARLIE.party)

                val permissionPointer = LinearPointer(permission.linearId, PermissionRequestState::class.java )

                reference(PermissionRequestContract.ID, permission)

                val usage = UsageState(
                        params.dataSetName,
                        permissionPointer,
                        null,
                        params.provider.party,
                        params.redistributor.party,
                        params.subscriber.party,
                        params.user,
                        params.date)

                output(UsageContract.ID, usage)

                if (params.extraInput) {
                    input(UsageContract.ID, usage)
                }
                if(params.extraOutput) {
                    output(UsageContract.ID, usage)
                }

                var signers = listOf(params.subscriber)
                if (params.addSigner) {
                    signers = (signers + EMPTY_IDENTITY)
                }
                if (params.wrongSigner) {
                    signers = listOf(EMPTY_IDENTITY)
                }

                command(signers.map { it.publicKey },
                        UsageContract.Commands.Issue())

                verificationBlock()

            }
        }
    }

    @Test
    fun usageTest() {
        baseTest {
            verifies()
        }
    }

    @Test
    fun usageTooManyOutput() {
        baseTest (BaseTestParams(extraOutput = true)) {
            `fails with`("Only one output state should be created when issuing Usage.")
        }
    }

    @Test
    fun usageTooManyInput() {
        baseTest (BaseTestParams(extraInput = true)) {
            `fails with`("No inputs should be consumed when issuing Usage.")
        }
    }

    @Test
    fun usageMissingSigner() {
        baseTest (BaseTestParams(wrongSigner = true)) {
            `fails with`("Only the subscriber is required to sign")
        }
    }

    @Test
    fun usageExtraSigner() {
        baseTest (BaseTestParams(addSigner = true)) {
            `fails with`("Only the subscriber is required to sign")
        }
    }

    @Test
    fun usageWrongProvider() {
        baseTest (BaseTestParams(provider = ALICE)) {
            `fails with`("The subscriber and provider cannot be the same.")
        }
    }

    @Test
    fun usageEmptyDate() {
        baseTest (BaseTestParams(date = "")) {
            `fails with`("The date cannot be empty")
        }
    }

    @Test
    fun usageEmptyUser() {
        baseTest (BaseTestParams(user = "")) {
            `fails with`( "The username cannot be empty" )
        }
    }

    @Test
    fun usageWrongDataSet() {
        baseTest (BaseTestParams(dataSetName = "WrongDS")) {
            `fails with`("The data set must match the data set on the permission state")
        }
    }

    @Test
    fun permissionRequestWrongProvider() {
        baseTest (BaseTestParams(provider = DAN)) {
            `fails with`("The provider must match the provider on the permission state")
        }
    }

    @Test
    fun permissionRequestWrongRedistributorPerm() {
        baseTest (BaseTestParams(redistributor = DAN)) {
            `fails with`("The redistributor must match the provider on the permission state")
        }
    }

    @Test
    fun permissionRequestWrongSubscriberPerm() {
        baseTest (BaseTestParams(subscriber = DAN)) {
            `fails with`("The subscriber must match the subscriber on the permission state")
        }
    }

    fun receiptBaseTest(params : BaseTestParams = BaseTestParams(),
                 verificationBlock : TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail) {

        ledgerServices.ledger {
            transaction {
                val attachmentId = attachment(attachmentFile.inputStream())
                val tandc = TermsAndConditionsState("StandardTerms", BOB.party, attachmentId)
                val dataTandCpointer = LinearPointer(tandc.linearId, TermsAndConditionsState::class.java)

                val prices  = PricingParameter(10.0)
                val dataSet = DataSetState("LSE L1", BOB.party, listOf(prices), dataTandCpointer)
                val dataSetPointer = LinearPointer(dataSet.linearId, DataSetState::class.java)

                val signedDataTandCs =
                        SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, dataTandCpointer)
                val signedDataTandCsPointer =
                        LinearPointer(signedDataTandCs.linearId, SignedTermsAndConditionsState::class.java)


                reference(SignedTermsAndConditionsContract.ID, signedDataTandCs)
                reference(DataSetContract.ID, dataSet)
                reference(TermsAndConditionsContract.ID, tandc)

                val usageReceipt =  UsageReceiptState(
                        dataSetPointer,
                        signedDataTandCsPointer,
                        params.dataSetName,
                        params.provider.party,
                        params.redistributor.party,
                        params.subscriber.party,
                        params.user,
                        params.date
                )

                output(UsageContract.ID, usageReceipt)

                if (params.extraInput) {
                    input(UsageContract.ID, usageReceipt)
                }
                if(params.extraOutput) {
                    output(UsageContract.ID, usageReceipt)
                }

                var signers = listOf(params.subscriber)
                if (params.addSigner) {
                    signers = (signers + EMPTY_IDENTITY)
                }
                if (params.wrongSigner) {
                    signers = listOf(EMPTY_IDENTITY)
                }

                command(signers.map { it.publicKey },
                        UsageContract.Commands.SendReceipt())

                verificationBlock()

            }
        }
    }

    @Test
    fun usageReceiptTest() {
        receiptBaseTest {
            verifies()
        }
    }

    @Test
    fun usageReceiptTooManyOutput() {
        receiptBaseTest (BaseTestParams(extraOutput = true)) {
            `fails with`("Only one output state should be created when issuing receipt.")
        }
    }

    @Test
    fun usageReceiptTooManyInput() {
        receiptBaseTest (BaseTestParams(extraInput = true)) {
            `fails with`("No inputs should be consumed when issuing receipt.")
        }
    }

}
