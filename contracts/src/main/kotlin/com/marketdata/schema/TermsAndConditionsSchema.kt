package com.marketdata.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object TermsAndConditionsSchema

object TermsAndConditionsSchemaV1 : MappedSchema(
        schemaFamily = TermsAndConditionsSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentTandC::class.java)) {

    override val migrationResource: String?
        get() = "t-and-c-states.changelog-master";

    @Entity
    @Table(name = "t_and_c_states")
    class PersistentTandC(
            @Column(name = "name")
            var name: String,

            @Column(name = "issuer")
            var issuerName: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "")
    }
}