package com.marketdata.states

import com.marketdata.contracts.DataSetContract
import com.marketdata.data.PricingParameter
import com.marketdata.schema.DataSetSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(DataSetContract::class)
class DataSetState(val name : String,
                   val provider: Party,
                   val pricingParameters: List<PricingParameter>,
                   val termsAndConditions: LinearPointer<TermsAndConditionsState>,
                   override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<Party> get() = listOf(provider)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is DataSetSchemaV1 -> DataSetSchemaV1.PersistentDataSet(
                    this.name,
                    this.provider.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DataSetSchemaV1)

    override fun toString() : String {

        return stateToString(
                mapOf(
                        "Name" to name,
                        "Provider" to provider.name.toString(),
                        "linearId" to linearId.toString()
                )
        )
    }
}