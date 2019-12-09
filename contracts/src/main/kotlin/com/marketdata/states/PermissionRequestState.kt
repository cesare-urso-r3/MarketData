package com.marketdata.states

import com.marketdata.contracts.PermissionRequestContract
import com.marketdata.schema.PermissionSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

// *********
// * State *
// *********
@BelongsToContract(PermissionRequestContract::class)
data class PermissionRequestState(val distributableDataSet: LinearPointer<DistributableDataSetState>,
                                  val providerTandCs : LinearPointer<SignedTermsAndConditionsState>,
                                  val redistributorTandCs : LinearPointer<SignedTermsAndConditionsState>,
                                  val dataSetName: String,
                                  val provider: Party,
                                  val subscriber : Party,
                                  val redistributor: Party,
                                  override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<Party> get() = setOf(subscriber, redistributor, provider).toList()

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PermissionSchemaV1 -> PermissionSchemaV1.PersistentPermission(
                    this.dataSetName,
                    this.provider.name.toString(),
                    this.subscriber.name.toString(),
                    this.redistributor.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PermissionSchemaV1)

    override fun toString() : String {
        return stateToString(
                mapOf(
                        "Subscriber" to subscriber.name.toString(),
                        "DataSet" to dataSetName,
                        "Provider" to provider.name.toString(),
                        "Redistributor" to redistributor.name.toString()
                )
        )
    }
}
