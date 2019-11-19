package com.marketdata.states

import com.marketdata.contracts.DataSetContract
import com.marketdata.contracts.DistributableDataSetContract
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
import java.time.LocalDate

@BelongsToContract(DistributableDataSetContract::class)
class DistributableDataSetState(val dataSetName : String,
                                val provider: Party,
                                val dataSet: LinearPointer<DataSetState>,
                                val pricingParameters: List<PricingParameter>,
                                val termsAndConditions: LinearPointer<TermsAndConditionsState>,
                                override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<Party> get() = listOf(provider)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is DataSetSchemaV1 -> DataSetSchemaV1.PersistentDataSet(
                    this.dataSetName,
                    this.provider.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun toString() : String {
        return  "$dataSetName/${provider.name}"
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DataSetSchemaV1)
}