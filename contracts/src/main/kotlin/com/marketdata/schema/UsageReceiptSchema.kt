package com.marketdata.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object UsageReceiptSchema

object UsageReceiptSchemaV1 : MappedSchema(
        schemaFamily = UsageReceiptSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentUsageReceipt::class.java)) {

    override val migrationResource: String?
        get() = "usage-receipt-states.changelog-master";

    @Entity
    @Table(name = "usage_receipt_states")
    class PersistentUsageReceipt(

            @Column(name = "dataset")
            var dataSetName: String,

            @Column(name = "provider")
            var providerName: String,

            @Column(name = "subscriber")
            var subscriberName: String,

            @Column(name = "userName")
            var userName: String,

            @Column(name = "date")
            var date: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("",
                "",
                "",
                "",
                "")
    }
}