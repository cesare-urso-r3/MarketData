package com.marketdata.states

import com.marketdata.contracts.SignedTermsAndConditionsContract
import com.marketdata.schema.SignedTermsAndConditionsSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(SignedTermsAndConditionsContract::class)
class SignedTermsAndConditionsState(val name : String,
                                    val issuer : Party,
                                    val signer: Party,
                                    val termsAndConditions: LinearPointer<TermsAndConditionsState>,
                                    override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<Party> get() = listOf(signer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SignedTermsAndConditionsSchemaV1 -> SignedTermsAndConditionsSchemaV1.PersistentSignedTandC(
                    this.name,
                    this.issuer.name.toString(),
                    this.signer.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SignedTermsAndConditionsSchemaV1)

    override fun toString() : String {
        return stateToString(
                mapOf(
                        "T&C name" to name,
                        "Issuer" to issuer.name.toString(),
                        "Signer" to signer.name.toString()
                )
        )
    }
}