package com.marketdata.states

import com.marketdata.contracts.PermissionContract
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
@BelongsToContract(PermissionContract::class)
data class PermissionState(val dataSetName: String,
                           val permissionRequestState: LinearPointer<PermissionRequestState>,
                           val provider: Party,
                           val subscriber : Party,
                           val dataChargeOwner: Party, // TODO make abstract
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState { //, QueryableState
    override val participants: List<Party> get() = setOf(subscriber, provider, dataChargeOwner).toList()

//
//    override fun generateMappedObject(schema: MappedSchema): PersistentState {
//        return when (schema) {
//            is PermissionSchemaV1 -> PermissionSchemaV1.PersistentPermission(
//                    this.dataSetName,
//                    this.provider.name.toString(),
//                    this.subscriber.name.toString(),
//                    this.dataChargeOwner.name.toString(),
//                    //this.dataChargeOwner.nameOrNull()?.x500Principal?.name ?: "",
//                    this.linearId.id
//            )
//            else -> throw IllegalArgumentException("Unrecognised schema $schema")
//        }
//    }
//
//    override fun toString() : String {
//
//        return "${subscriber.name}, ($dataSetName/${provider.name}), ${dataChargeOwner}"
//
//    }
//
//    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PermissionSchemaV1)
}
