package com.marketdata.states

import com.marketdata.contracts.UsageContract
import com.marketdata.schema.UsageSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.LocalDate

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
                        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState { // QueryableState
    override val participants: List<AbstractParty> get() = setOf(provider, subscriber, redistributor).toList()

    override fun toString(): String = "UsageReceiptState $dataSetName $provider $redistributor $subscriber"
//
//    override fun generateMappedObject(schema: MappedSchema): PersistentState {
//        return when (schema) {
//            is UsageSchemaV1 -> UsageSchemaV1.PersistentUsage(
//                    this.dataSetName,
//                    this.provider.name.toString(),
//                    this.subscriber.name.toString(),
//                    this.redistributor.owningKey.toString(),
//                    this.date
//            )
//            else -> throw IllegalArgumentException("Unrecognised schema $schema")
//        }
//    }
//
//    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UsageSchemaV1)
}