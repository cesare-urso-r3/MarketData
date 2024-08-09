package com.marketdata.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object DataSetSchema

object DataSetSchemaV1 : MappedSchema(
        schemaFamily = DataSetSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentDataSet::class.java)) {

    override val migrationResource: String?
        get() = "data-set-states.changelog-master";

    @Entity
    @Table(name = "data_set_states")
    class PersistentDataSet(
            @Column(name = "name")
            var name: String,

            @Column(name = "provider")
            var providerName: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "")
    }
}