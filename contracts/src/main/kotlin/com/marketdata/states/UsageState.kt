package com.marketdata.states

import com.marketdata.contracts.UsageContract
import com.marketdata.schema.UsageSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.LocalDate

// TODO: need to think about dates and timezones

@BelongsToContract(UsageContract::class)
class UsageState(val dataSetName : String,
                 val permissionState : LinearPointer<PermissionRequestState>,
                 val paidUsageState: LinearPointer<UsageReceiptState>?,
                 val provider: Party,
                 val redistributor : Party,
                 val subscriber: Party,
                 val userName: String,
                 val date : String,
                 override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<Party> get() = setOf(provider, subscriber, redistributor).toList()


    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is UsageSchemaV1 -> UsageSchemaV1.PersistentUsage(
                    this.subscriber.name.toString(),
                    this.redistributor.toString(),
                    this.date
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UsageSchemaV1)
}