package com.marketdata.states

import com.marketdata.contracts.PermissionContract
import com.marketdata.contracts.PermissionRequestContract
import com.marketdata.schema.*
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.toX500Name
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
                                  val dataChargeOwner: Party, // TODO make abstract?
                                  override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState { // QueryableState
    override val participants: List<Party> get() = setOf(subscriber, dataChargeOwner).toList()


//    fun withNewDataChargeOwner(newDataChargeOwner: Party): PermissionRequestState {
//        // TODO: how to manage notifying the old owner that they are no longer the owner?
//        // may be easier to just revoke and re-issue
//        return this.copy(dataChargeOwner = newDataChargeOwner, linearId = linearId)
//    }
//
//    override fun generateMappedObject(schema: MappedSchema): PersistentState {
//        return when (schema) {
//            is PermissionSchemaV1 -> PermissionSchemaV1.PersistentPermission(
//                    this.dataSetName,
//                    this.provider.name.toString(),
//                    this.subscriber.name.toString(),
//                    this.dataChargeOwner.name.toString(),
//            )
//            else -> throw IllegalArgumentException("Unrecognised schema $schema")
//        }
//    }

//    override fun toString() : String {
//
//        return "${subscriber.name}, ($dataSetName/${provider.name}), ${dataChargeOwner.name}"
//    }
//
//    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PermissionSchemaV1)
}
