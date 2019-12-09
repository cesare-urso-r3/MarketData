package com.marketdata.flows

import com.marketdata.schema.DataSetSchemaV1
import com.marketdata.schema.DistributableDataSetSchemaV1
import com.marketdata.schema.SignedTermsAndConditionsSchemaV1
import com.marketdata.schema.TermsAndConditionsSchemaV1
import com.marketdata.states.DataSetState
import com.marketdata.states.DistributableDataSetState
import com.marketdata.states.SignedTermsAndConditionsState
import com.marketdata.states.TermsAndConditionsState
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.CordaSerializable

// Functions and extension helpers for flow related functions

@CordaSerializable
data class DataSetNameAndProvider(val name: String, val provider: Party)

fun <T : LinearState> LinearPointer<T>.resolveToState(hub: ServiceHub) : T {
    return this.resolve(hub).state.data
}

fun VaultService.getDistributableDataSetStateAndRef(provider: Party, redistributor: Party, dataSetName : String) : StateAndRef<DistributableDataSetState> {

    val vaultService = this
    // now get the state with that name
    val dataSetStateAndRefs = builder {
        val dataSetIdx = DistributableDataSetSchemaV1.PersistentDistributableDataSet::name.equal(dataSetName)
        val providerIdx = DistributableDataSetSchemaV1.PersistentDistributableDataSet::providerName.equal(provider.name.toString())
        val redistributorIdx = DistributableDataSetSchemaV1.PersistentDistributableDataSet::redistributorName.equal(redistributor.name.toString())

        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(dataSetIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)
        val customCriteria3 = QueryCriteria.VaultCustomQueryCriteria(redistributorIdx)

        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2
                                .and(customCriteria3)))

        vaultService.queryBy(DistributableDataSetState::class.java, criteria)
    }.states

    check(dataSetStateAndRefs.size == 1)

    return dataSetStateAndRefs.single()
}

fun VaultService.getSignedTandCStateAndRef(tandc: TermsAndConditionsState, signer : Party) : StateAndRef<SignedTermsAndConditionsState> {

    val vaultService = this

    val signedTandCStateAndRefs = builder {
        val dataSetIdx = SignedTermsAndConditionsSchemaV1.PersistentSignedTandC::name.equal(tandc.name)
        val providerIdx = SignedTermsAndConditionsSchemaV1.PersistentSignedTandC::issuerName.equal(tandc.issuer.name.toString())
        val signerIdx = SignedTermsAndConditionsSchemaV1.PersistentSignedTandC::signerName.equal(signer.name.toString())

        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(dataSetIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)
        val customCriteria3 = QueryCriteria.VaultCustomQueryCriteria(signerIdx)

        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2
                                .and(customCriteria3)))

        vaultService.queryBy(SignedTermsAndConditionsState::class.java, criteria)
    }.states

    check(signedTandCStateAndRefs.size == 1)

    return signedTandCStateAndRefs.single()
}

fun VaultService.hasDataSet(name: String, provider: Party) : Boolean {
    val vaultService = this
    val results = builder {
        val nameIdx = DataSetSchemaV1.PersistentDataSet::name.equal(name)
        val providerIdx = DataSetSchemaV1.PersistentDataSet::providerName.equal(provider.name.toString())
        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(nameIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)
        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2))

        vaultService.queryBy(DataSetState::class.java,criteria)
    }.states

    return results.isNotEmpty()
}

fun VaultService.getDataSetStateAndRef(name: String, provider: Party) : StateAndRef<DataSetState> {
    val vaultService = this
    val results = builder {
        val nameIdx = DataSetSchemaV1.PersistentDataSet::name.equal(name)
        val providerIdx = DataSetSchemaV1.PersistentDataSet::providerName.equal(provider.name.toString())
        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(nameIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)
        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2))

        vaultService.queryBy(DataSetState::class.java,criteria)
    }.states

    check(results.size == 1)

    return results.single()
}

fun VaultService.getTandCState(name : String, party: Party) : TermsAndConditionsState {

    val vaultService = this

    return builder {
        val tAndCIdx = TermsAndConditionsSchemaV1.PersistentTandC::name.equal(name)
        val issuerIdx = TermsAndConditionsSchemaV1.PersistentTandC::issuerName.equal(party.name.toString())

        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(tAndCIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(issuerIdx)

        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2))

        vaultService.queryBy(TermsAndConditionsState::class.java,criteria)
    }.states.single().state.data
}
