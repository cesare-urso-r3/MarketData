import helpers.*

import java.io.File
import net.corda.*
import net.corda.core.contracts.*
import net.corda.core.crypto.*
import net.corda.core.flows.*
import net.corda.core.identity.*
import net.corda.core.messaging.*
import net.corda.core.utilities.*
import net.corda.core.messaging.*
import net.corda.core.transactions.*
import net.corda.core.schemas.*
import net.corda.core.node.services.*
import net.corda.core.node.services.vault.*
import net.corda.core.node.services.vault.Builder
import com.marketdata.states.*
import com.marketdata.flows.*
import com.marketdata.schema.*
import com.marketdata.data.*

// Kotlin shell helper script


/*
 * Helpers used in this script
 */
fun <T : ContractState> CordaRPCOps.displayStates(clazz : Class<T>) {
    val criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
    val result = vaultQueryByCriteria<ContractState>(criteria, clazz)
    result.states.forEach{
        println(it.state.data)
    }
}

fun listFiles() {
    File("files/").list().forEach {
        println(it)
    }
}

// helper class to make it easy to work with parties
class rpcParty (val port : Int) {
    var proxy : CordaRPCOps
    var party : PartyAndCertificate

    init {
        proxy = connect(port)!!
        party = getParty(proxy)
    }

    /*
     * Display helpers
     */
    fun displayTandCs() {
        proxy.displayStates(com.marketdata.states.TermsAndConditionsState::class.java)
    }

    fun displayDataSets() {
        proxy.displayStates(com.marketdata.states.DataSetState::class.java)
    }

    fun displayDistributableDataSets() {
        proxy.displayStates(com.marketdata.states.DistributableDataSetState::class.java)
    }

    fun displaySignedTandCs() {
        proxy.displayStates(com.marketdata.states.SignedTermsAndConditionsState::class.java)
    }

    fun displayPermissionRequests() {
        proxy.displayStates(com.marketdata.states.PermissionRequestState::class.java)
    }

    fun displayUsages() {
        proxy.displayStates(com.marketdata.states.UsageState::class.java)
    }

    fun displayUsageReceipts() {
        proxy.displayStates(com.marketdata.states.UsageReceiptState::class.java)
    }

    fun displayBill() {
        proxy.displayStates(com.marketdata.states.BillingState::class.java)
    }

    /*
     * Vault helpers
     */
    fun getTandCState(tandCName : String) : TermsAndConditionsState {
        val results = builder {
            val tAndCIdx = TermsAndConditionsSchemaV1.PersistentTandC::name.equal(tandCName)
            val issuerIdx = TermsAndConditionsSchemaV1.PersistentTandC::issuerName.equal(party.name.toString())

            val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(tAndCIdx)
            val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(issuerIdx)

            val criteria = QueryCriteria.VaultQueryCriteria()
                    .and(customCriteria1
                            .and(customCriteria2))

            proxy.vaultQueryByCriteria<ContractState>(
                    criteria,
                    com.marketdata.states.TermsAndConditionsState::class.java)

        }

        return results.states.single().state.data as TermsAndConditionsState
    }

    fun getDataSet(name : String, p: rpcParty) : DataSetState {
        val results = builder {
            val nameIdx = DataSetSchemaV1.PersistentDataSet::name.equal(name)
            val pIdx = DataSetSchemaV1.PersistentDataSet::providerName.equal(p.party.party.name.toString())

            val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(nameIdx)
            val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(pIdx)

            val criteria = QueryCriteria.VaultQueryCriteria()
                    .and(customCriteria1
                            .and(customCriteria2))

            proxy.vaultQueryByCriteria<ContractState>(
                    criteria,
                    com.marketdata.states.DataSetState::class.java)
        }

        return results.states.single().state.data as DataSetState
    }

    /*
     * Flow Helpers
     */
    fun createTandC(filename : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.IssueTandC::class.java,
                attach(filename),
                filename
        ).returnValue.getOrThrow()
    }

    fun createDataSet(dataSetName : String, tandCName : String, pricePerUser : Double) {

        val tandcState = getTandCState(tandCName)
        proxy.startFlowDynamic(
                com.marketdata.flows.DataSetIssue::class.java,
                dataSetName,
                tandcState,
                PricingParameter(pricePerUser)
        ).returnValue.getOrThrow()
    }

    fun downloadDataSet(dataSetName : String, p : rpcParty) {
        proxy.startFlowDynamic(
                com.marketdata.flows.DataSetRequestInitiator::class.java,
                p.party.party,
                dataSetName
        ).returnValue.getOrThrow()
    }

    fun downloadDistributableDataSet(dataSetName : String, p : rpcParty, rer : rpcParty) {
        proxy.startFlowDynamic(
                com.marketdata.flows.DistributableDataSetRequestInitiator::class.java,
                rer.party.party,
                dataSetName,
                p.party.party
        ).returnValue.getOrThrow()
    }

    fun createDistributableDataSet(dataSetName : String, p: rpcParty, tandCName : String, pricePerUser : Double) {

        val dataSetState = getDataSet(dataSetName, p)
        val tandcState = getTandCState(tandCName)

        proxy.startFlowDynamic(
                com.marketdata.flows.DistributableDataSetIssue::class.java,
                dataSetName,
                dataSetState,
                tandcState,
                PricingParameter(pricePerUser)
        ).returnValue.getOrThrow()
    }

    fun signTandC(tandcName: String, issuer: rpcParty) {

        proxy.startFlowDynamic(
                com.marketdata.flows.SignTandC::class.java,
                tandcName,
                issuer.party.party
        ).returnValue.getOrThrow()
    }

    fun requestPermission(p : rpcParty, rer : rpcParty, dataSetName : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.PermissionRequest::class.java,
                p.party.party,
                rer.party.party,
                dataSetName
        ).returnValue.getOrThrow()
    }

    fun createUsage(p : rpcParty, rer : rpcParty, dataSetName : String, userName : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.UsageIssue::class.java,
                dataSetName,
                p.party.party,
                rer.party.party,
                userName,
                null
        ).returnValue.getOrThrow()
    }

    fun createUsage(p : rpcParty, rer : rpcParty, dataSetName : String, userName : String, rpcParty: rpcParty) {
        proxy.startFlowDynamic(
                com.marketdata.flows.UsageIssue::class.java,
                dataSetName,
                p.party.party,
                rer.party.party,
                userName,
                rpcParty.party.party
        ).returnValue.getOrThrow()
    }

    fun createBill(partyToBill : rpcParty, fromDate : String, toDate : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.BillCreate::class.java,
                fromDate,
                toDate,
                partyToBill.party.party
        ).returnValue.getOrThrow()
    }

    /*
     * Internal
     */
    private fun attach(filename: String) : SecureHash? {
        val stream = java.io.File("files/$filename").inputStream()
        return proxy.uploadAttachment(stream)
    }

}
val p = rpcParty(10006)
val r = rpcParty(10009)
val s = rpcParty(10012)
val r2 = rpcParty(10015)

// main
fun main () {

    p.createTandC("provider2.zip")
    p.createDataSet("LSE", "p2.zip", 10.0)

    r.downloadDataSet("LSE", p)
    r.createTandC("RedistributorDemoT&C.zip")
    r.createDistributableDataSet("LSE", p, "RedistributorDemoT&C.zip", 1.0)

    s.downloadDistributableDataSet("LSE", p, r)
    s.signTandC("provider2.zip", p)
    s.signTandC("RedistributorDemoT&C.zip", r)
    s.requestPermission(p, r, "LSE")
    s.createUsage(p, r, "LSE", "Adam.Houston")
    s.createUsage(p, r, "LSE", "Honest.Joe")
    s.createUsage(p, r, "LSE", "Dodgy.Dave")


    r.createBill(s, "2019-12-01", "2019-12-31")

    r2.downloadDataSet("LSE", p)
    r2.createTandC("r2.zip")
    r2.createDistributableDataSet("LSE", p, "r2.zip", 2.1)

    s.downloadDistributableDataSet("LSE", p, r2)
    s.signTandC("r2.zip", r2)
    s.requestPermission(p, r2, "LSE")
    s.createUsage(p, r2, "LSE", "Alice.McBob")
    s.createUsage(p, r2, "LSE", "Honest.Joe", r)

    r2.createBill(s, "2019-12-01", "2019-12-31")
}


// val r2 = rpcParty(10015)








