package com.marketdata.states

import com.marketdata.contracts.DataSetContract
import com.marketdata.contracts.DistributorContract
import com.marketdata.contracts.UsageContract
import com.marketdata.data.PricingParameter
import com.marketdata.schema.DataSetSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.time.LocalDate

@CordaSerializable
enum class DistributorContractState {
    PROPOSED, SIGNED
}

@BelongsToContract(DistributorContract::class)
data class DistributorState(val provider: Party,
                       val redistributor: Party,
                       val termsAndConditions: LinearPointer<SignedTermsAndConditionsState>,
                       val contractState: DistributorContractState = DistributorContractState.PROPOSED,
                       override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState  { //QueryableState

    override val participants: List<Party> get() = listOf(provider, redistributor)

     fun withSignedStatus() : DistributorState {
        return this.copy(contractState = DistributorContractState.SIGNED, linearId = linearId)
    }
//
//    override fun generateMappedObject(schema: MappedSchema): PersistentState {
//        return when (schema) {
//            is DataSetSchemaV1 -> DataSetSchemaV1.PersistentDataSet(
//                    this.name,
//                    this.provider.name.toString()
//            )
//            else -> throw IllegalArgumentException("Unrecognised schema $schema")
//        }
//    }
//
//    override fun toString() : String {
//        return  "$name/${provider.name}"
//    }
//
//    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DataSetSchemaV1)
}