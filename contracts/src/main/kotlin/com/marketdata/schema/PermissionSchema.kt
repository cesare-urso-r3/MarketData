package com.marketdata.schema

import com.marketdata.states.DataSetState
import net.corda.core.contracts.StatePointer
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object PermissionSchema

object PermissionSchemaV1 : MappedSchema(
        schemaFamily = PermissionSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPermission::class.java)) {

    @Entity
    @Table(name = "permission_states")
    class PersistentPermission(
            @Column(name = "dataSet")
            var dataSetName: String,

            @Column(name = "provider")
            var providerName: String,

            @Column(name = "subscriber")
            var subscriberName: String,

            @Column(name = "dataChargeOwner")
            var dataChargeOwnerName: String,

            @Column(name = "linear_id")
            var linearId: UUID

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "", "", "", UUID.randomUUID())
    }
}