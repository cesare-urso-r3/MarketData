package com.marketdata.states

import com.marketdata.contracts.TermsAndConditionsContract
import com.marketdata.schema.TermsAndConditionsSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.QueryableState

@BelongsToContract(TermsAndConditionsContract::class)
class TermsAndConditionsState(val name : String,
                              val issuer: Party,
                              val termsAndConditions: AttachmentId,
                              override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<Party> get() = listOf(issuer)

    override fun generateMappedObject(schema: MappedSchema): TermsAndConditionsSchemaV1.PersistentTandC {
        return when (schema) {
            is TermsAndConditionsSchemaV1 -> TermsAndConditionsSchemaV1.PersistentTandC(
                    this.name,
                    this.issuer.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(TermsAndConditionsSchemaV1)
}