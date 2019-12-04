package com.marketdata.states

import com.marketdata.contracts.PermissionRequestContract
import com.marketdata.schema.PermissionSchemaV1
import net.corda.core.contracts.*
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
                                  val redistributor: Party, // TODO make abstract?
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

    override fun toString() : String {

        return "${subscriber.name}, ($dataSetName/${provider.name}), ${redistributor.name}"
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PermissionSchemaV1)
}
