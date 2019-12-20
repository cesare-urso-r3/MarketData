package com.marketdata.states

import com.marketdata.contracts.UsageContract
import com.marketdata.schema.UsageReceiptSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

// TODO: need to think about dates and timezones

@BelongsToContract(UsageContract::class)
class UsageReceiptState(val dataSet : LinearPointer<DataSetState>,
                        val signedTandCs : LinearPointer<SignedTermsAndConditionsState>,
                        val dataSetName : String,
                        val provider: Party,
                        val redistributor : AbstractParty,
                        val subscriber: Party,
                        val userName: String,
                        val date : String,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<AbstractParty> get() = setOf(provider, subscriber, redistributor).toList()

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is UsageReceiptSchemaV1 -> UsageReceiptSchemaV1.PersistentUsageReceipt(
                    this.dataSetName,
                    this.provider.name.toString(),
                    this.subscriber.name.toString(),
                    this.userName,
                    this.date
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UsageReceiptSchemaV1)
}