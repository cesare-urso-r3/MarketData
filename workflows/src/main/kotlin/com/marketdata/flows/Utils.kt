package com.marketdata.flows

import com.marketdata.schema.DistributableDataSetSchemaV1
import com.marketdata.schema.SignedTermsAndConditionsSchemaV1
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
import net.corda.core.transactions.LedgerTransaction

fun <T : LinearState> LedgerTransaction.resolveToState(pointer : LinearPointer<T>) : T {
    return pointer.resolve(this).state.data
}


fun <T : LinearState> LinearPointer<T>.resolveToState(hub: ServiceHub) : T {
    return this.resolve(hub).state.data
}

fun VaultService.GetDistributableDataSet(provider: Party, redistributor: Party, dataSetName : String) : StateAndRef<DistributableDataSetState> {

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

fun VaultService.GetSignedTandCStateAndRef(tandc: TermsAndConditionsState, signer : Party) : StateAndRef<SignedTermsAndConditionsState> {

    val vaultService = this
    // now get the state with that name

    println("[INPUT] $tandc, $signer")
    vaultService.queryBy(SignedTermsAndConditionsState::class.java).states.forEach {
        it.state.data.let {
            state -> println(state)
        }
    }

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

fun VaultService.GetTandCStateAndRef(name: String, issuer: Party) : StateAndRef<TermsAndConditionsState> {

    val vaultService = this
    // now get the state with that name

    val tandCStateAndRefs = builder {
        val dataSetIdx = SignedTermsAndConditionsSchemaV1.PersistentSignedTandC::name.equal(name)
        val providerIdx = SignedTermsAndConditionsSchemaV1.PersistentSignedTandC::issuerName.equal(issuer.name.toString())

        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(dataSetIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(providerIdx)

        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2))

        vaultService.queryBy(TermsAndConditionsState::class.java, criteria)
    }.states

    check(tandCStateAndRefs.size == 1)

    return tandCStateAndRefs.single()
}