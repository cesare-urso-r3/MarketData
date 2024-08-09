package com.marketdata.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object UsageSchema

object UsageSchemaV1 : MappedSchema(
        schemaFamily = UsageSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentUsage::class.java)) {

    override val migrationResource: String?
        get() = "usage-states.changelog-master";

    @Entity
    @Table(name = "usage_states")
    class PersistentUsage(
            @Column(name = "subscriber")
            var subscriberName: String,

            @Column(name = "redistributor")
            var redistributorName: String,

            @Column(name = "date")
            var date: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("",
                "",
                "")
    }
}