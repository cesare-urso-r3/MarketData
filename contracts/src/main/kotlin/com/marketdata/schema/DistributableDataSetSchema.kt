package com.marketdata.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object DistributableDataSetSchema

object DistributableDataSetSchemaV1 : MappedSchema(
        schemaFamily = DistributableDataSetSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentDistributableDataSet::class.java)) {

    override val migrationResource: String?
        get() = "distributable-data-set-states.changelog-master";

    @Entity
    @Table(name = "distributable_data_set_states")
    class PersistentDistributableDataSet(
            @Column(name = "name")
            var name: String,

            @Column(name = "provider")
            var providerName: String,

            @Column(name = "redistributor")
            var redistributorName: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "", "")
    }
}