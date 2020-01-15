package com.marketdata.contracts

import com.marketdata.*
import com.marketdata.data.PricingParameter
import com.marketdata.states.*
import com.sun.org.apache.xpath.internal.operations.Bool
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
import java.util.spi.CalendarDataProvider

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
        val user : String = "Tester",
        val wrongTandCs : Boolean = false,
        val paidUsage : Boolean = false,
        val paidBadDataSet : Boolean = false,
        val paidDataSetName : String = "LSE L1",
        val paidUser : String = "Tester",
        val paidDate : String = LocalDate.now().toString(),
        val paidSubscriber : TestIdentity = ALICE,
        val paidProvider : TestIdentity = BOB,
        val paidBadSignedTandC : Boolean = false,
        val paidBadSignedTandCName : String = "StandardTerms",
        val paidBadSignedTandCIssuer : TestIdentity = BOB,
        val paidBadSignedTandCSigner : TestIdentity = ALICE
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
    val badAttachmentFile = File("src/test/resources/prices.zip")

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

                val paidUsage = if (params.paidUsage) {

                    var dataSetPointerToUse =  dataSetPointer
                    var tandCPointerToUse =  dataTandCpointer
                    var signedTandCPointerToUse = signedDataTandCsPointer

                    if (params.paidBadDataSet) {
                        val badAttachmentId = attachment(badAttachmentFile.inputStream())
                        val badTandc = TermsAndConditionsState("BadTerms", DAN.party, badAttachmentId)
                        val badDataTandCpointer = LinearPointer(badTandc.linearId, TermsAndConditionsState::class.java)

                        val badPrices = PricingParameter(110.0)
                        val badDataSet = DataSetState("BAD DATA", DAN.party, listOf(badPrices), badDataTandCpointer)

                        dataSetPointerToUse = LinearPointer(badDataSet.linearId, DataSetState::class.java)
                        println("I am Using the bad data set.")

                        reference(DataSetContract.ID, badDataSet)
                        reference(TermsAndConditionsContract.ID, badTandc)
                    }

                    if (params.paidBadSignedTandC) {
                        val signedBadTandCs =
                                SignedTermsAndConditionsState(params.paidBadSignedTandCName,
                                        params.paidBadSignedTandCIssuer.party,
                                        params.paidBadSignedTandCSigner.party,
                                        tandCPointerToUse)

                        signedTandCPointerToUse = LinearPointer(signedBadTandCs.linearId,
                                SignedTermsAndConditionsState::class.java)
                        reference(SignedTermsAndConditionsContract.ID, signedBadTandCs)
                    }

                    val receipt = UsageReceiptState(
                            dataSetPointerToUse,
                            signedTandCPointerToUse,
                            params.paidDataSetName,
                            params.paidProvider.party,
                            params.redistributor.party,
                            params.paidSubscriber.party,
                            params.paidUser,
                            params.paidDate
                    )
                    reference(UsageContract.ID, receipt)
                    LinearPointer(receipt.linearId,
                            UsageReceiptState::class.java)
                } else {
                    null
                }

                val usage = UsageState(
                        params.dataSetName,
                        permissionPointer,
                        paidUsage,
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

                val dsTandCs =
                        SignedTermsAndConditionsState("StandardTerms", tandc.issuer, ALICE.party, dataTandCpointer)

                var signedTandCs = dsTandCs

                var altTandCs : TermsAndConditionsState? = null

                if (params.wrongTandCs) {
                    val distAttachmentId = attachment(distAttachmentFile.inputStream())
                    altTandCs = TermsAndConditionsState("DistributableTerms", CHARLIE.party, distAttachmentId)
                    val distTandCpointer = LinearPointer(altTandCs.linearId, TermsAndConditionsState::class.java)
                    signedTandCs =
                            SignedTermsAndConditionsState("DistributableTerms", altTandCs.issuer, ALICE.party, distTandCpointer)
                }

                if (altTandCs != null) {
                    reference(TermsAndConditionsContract.ID, altTandCs)
                }

                reference(SignedTermsAndConditionsContract.ID, signedTandCs)
                reference(DataSetContract.ID, dataSet)
                reference(TermsAndConditionsContract.ID, tandc)

                val signedTandCPointer =
                        LinearPointer(signedTandCs.linearId, SignedTermsAndConditionsState::class.java)

                val usageReceipt =  UsageReceiptState(
                        dataSetPointer,
                        signedTandCPointer,
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

                var signers = listOf(params.subscriber, params.provider, params.redistributor)
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

    @Test
    fun usageReceiptWrongDataSet() {
        receiptBaseTest (BaseTestParams(dataSetName = "WrongDS")) {
            `fails with`("The data set name must match that of the provided data set state")
        }
    }

    @Test
    fun usageReceiptWrongProvider() {
        receiptBaseTest (BaseTestParams(provider = DAN)) {
            `fails with`("The provider must match that of the provided data set state")
        }
    }

    @Test
    fun usageReceiptWrongTandCs() {
        receiptBaseTest (BaseTestParams(wrongTandCs = true)) {
            `fails with`("Incorrect signed T&Cs")
        }
    }

    @Test
    fun usageReceiptExtraSigner() {
        receiptBaseTest (BaseTestParams(addSigner = true)) {
            `fails with`("All parties are required to sign")
        }
    }

    @Test
    fun usageReceiptMissingSigner() {
        receiptBaseTest (BaseTestParams(wrongSigner = true)) {
            `fails with`("All parties are required to sign")
        }
    }

    // Paid usage tests

    @Test
    fun paidUsageTest() {
        baseTest (BaseTestParams(paidUsage = true)){
            verifies()
        }
    }

    @Test
    fun paidUsageTestWrongSubscriber() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidSubscriber = DAN
                )) {
            `fails with`("Paid permission must be for the correct subscriber")
        }
    }

    @Test
    fun paidUsageTestWrongProvider() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidProvider = DAN
                )) {
            `fails with`("Paid permission must be for the correct provider")
        }
    }

    @Test
    fun paidUsageTestWrongDate() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidDate = LocalDate.now().minusDays(7).toString()
                )) {
            `fails with`("Paid permission must be for the correct date")
        }
    }

    @Test
    fun paidUsageTestWrongDataSetName() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidDataSetName = "Wrong"
                )) {
            `fails with`("Paid permission must be for the correct data set name")
        }
    }

    @Test
    fun paidUsageTestWrongSignedTandCName() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidBadSignedTandC = true,
                        paidBadSignedTandCName = "Wrong TC Name"
                )) {
            `fails with`("Signed Terms and conditions must be for the correct T&C name")
        }
    }

    @Test
    fun paidUsageTestWrongSignedTandCIssuer() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidBadSignedTandC = true,
                        paidBadSignedTandCIssuer = DAN
                )) {
            `fails with`("Signed Terms and conditions must be for the correct T&C issuer")
        }
    }

    @Test
    fun paidUsageTestWrongSignedTandCSigner() {
        baseTest (
                BaseTestParams(
                        paidUsage = true,
                        paidBadSignedTandC = true,
                        paidBadSignedTandCSigner = DAN
                )) {
            `fails with`("Signed Terms and conditions must be signed by the subscriber")
        }
    }
}
