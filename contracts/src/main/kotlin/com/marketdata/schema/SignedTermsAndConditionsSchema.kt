package com.marketdata.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object SignedTermsAndConditionsSchema

object SignedTermsAndConditionsSchemaV1 : MappedSchema(
        schemaFamily = SignedTermsAndConditionsSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentSignedTandC::class.java)) {

    @Entity
    @Table(name = "signed_t_and_c_states")
    class PersistentSignedTandC(
            @Column(name = "name")
            var name: String,

            @Column(name = "issuer")
            var issuerName: String,

            @Column(name = "signer")
            var signerName : String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "", "")
    }
}