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
import java.time.LocalDate

// Kotlin shell helper script. Sets up some helpers and also has some functions that run through demo scenarios


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
class rpcParty (val name : String, val port : Int) {
    lateinit var proxy : CordaRPCOps
    lateinit var party : PartyAndCertificate

    init {
        proxy = connect(port)!!
        party = getParty(proxy)
    }

    /*
     * Display helpers
     */
    fun displayTandCs() {
        displayStates(com.marketdata.states.TermsAndConditionsState::class.java)
    }

    fun displayDataSets() {
        displayStates(com.marketdata.states.DataSetState::class.java)
    }

    fun displayDistributableDataSets() {
        displayStates(com.marketdata.states.DistributableDataSetState::class.java)
    }

    fun displaySignedTandCs() {
        displayStates(com.marketdata.states.SignedTermsAndConditionsState::class.java)
    }

    fun displayPermissionRequests() {
        displayStates(com.marketdata.states.PermissionRequestState::class.java)
    }

    fun displayUsages() {
        displayStates(com.marketdata.states.UsageState::class.java)
    }

    fun displayUsageReceipts() {
        displayStates(com.marketdata.states.UsageReceiptState::class.java)
    }

    fun displayBill() {
        displayStates(com.marketdata.states.BillingState::class.java)
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
        ).returnValue.get()
    }

    fun createDataSet(dataSetName : String, tandCName : String, pricePerUser : Double) {

        val tandcState = getTandCState(tandCName)
        proxy.startFlowDynamic(
                com.marketdata.flows.DataSetIssue::class.java,
                dataSetName,
                tandcState,
                PricingParameter(pricePerUser)
        ).returnValue.get()
    }

    fun downloadDataSet(dataSetName : String, p : rpcParty) {
        proxy.startFlowDynamic(
                com.marketdata.flows.DataSetRequestInitiator::class.java,
                p.party.party,
                dataSetName
        ).returnValue.get()
    }

    fun downloadDistributableDataSet(dataSetName : String, p : rpcParty, rer : rpcParty) {
        proxy.startFlowDynamic(
                com.marketdata.flows.DistributableDataSetRequestInitiator::class.java,
                rer.party.party,
                dataSetName,
                p.party.party
        ).returnValue.get()
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
        ).returnValue.get()
    }

    fun signTandC(tandcName: String, issuer: rpcParty) {

        proxy.startFlowDynamic(
                com.marketdata.flows.SignTandC::class.java,
                tandcName,
                issuer.party.party
        ).returnValue.get()
    }

    fun requestPermission(p : rpcParty, rer : rpcParty, dataSetName : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.PermissionRequest::class.java,
                p.party.party,
                rer.party.party,
                dataSetName
        ).returnValue.get()
    }

    fun createUsage(p : rpcParty, rer : rpcParty, dataSetName : String, userName : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.UsageIssue::class.java,
                dataSetName,
                p.party.party,
                rer.party.party,
                userName,
                null
        ).returnValue.get()
    }

    fun createUsage(p : rpcParty, rer : rpcParty, dataSetName : String, userName : String, rpcParty: rpcParty) {
        proxy.startFlowDynamic(
                com.marketdata.flows.UsageIssue::class.java,
                dataSetName,
                p.party.party,
                rer.party.party,
                userName,
                rpcParty.party.party
        ).returnValue.get()
    }

    fun createBill(partyToBill : rpcParty, fromDate : String, toDate : String) {
        proxy.startFlowDynamic(
                com.marketdata.flows.BillCreate::class.java,
                fromDate,
                toDate,
                partyToBill.party.party
        ).returnValue.get()
    }

    /*
     * Internal
     */
    private fun attach(filename: String) : SecureHash? {
        val stream = java.io.File("files/$filename").inputStream()
        return proxy.uploadAttachment(stream)
    }

    /*
     *
     */
    private fun <T : ContractState> displayStates(clazz : Class<T>) {
        val criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
        val result = proxy.vaultQueryByCriteria<ContractState>(criteria, clazz)
        println("\n>>>> ${name} <<<<")
        result.states.forEach{
            println(it.state.data.toTable())
        }
    }

}
val provider = rpcParty("Provider",10006)
val redistributor = rpcParty("Redistributor",10009)
val subscriber = rpcParty("Subscriber",10012)
val redistributor2 = rpcParty("Redistributor2",10015)

fun logThenDo(description: String, commandText : String, block : () -> Unit) {
    readLine()
    println()
    println(description)
//    readLine()
    println("> "+commandText)

    try {
        block()
    } catch (e : net.corda.core.CordaRuntimeException) {
        println(e.toString())
    }
}

// Step through the entire lifecycle of creating and sharing all states, right up to creating permission and usage
fun demo1() {

    logThenDo("Provider create T&C", "provider.createTandC(\"provider.zip\")") {
        provider.createTandC("provider.zip")
        provider.displayTandCs()
    }

    logThenDo("Provider create Data Set", "provider.createDataSet(\"LSE\", \"provider.zip\", 10.0)") {
        provider.createDataSet("LSE", "provider.zip", 10.0)
        provider.displayDataSets()
    }

    logThenDo("Redistributor download Data Set", "redistributor.downloadDataSet(\"LSE\", provider)") {
        redistributor.downloadDataSet("LSE", provider)
        Thread.sleep(5000)
        redistributor.displayDataSets()
    }

    logThenDo("Redistibutor create T&C","redistributor.createTandC(\"redistributor.zip\")") {
        redistributor.createTandC("redistributor.zip")
        redistributor.displayTandCs()
    }

    logThenDo("Redistibutor create distributable data set", "redistributor.createDistributableDataSet(\"LSE\", provider, \"redistributor.zip\", 1.0)") {
        redistributor.createDistributableDataSet("LSE", provider, "redistributor.zip", 1.0)
        redistributor.displayDistributableDataSets()
    }

    logThenDo("Subscriber download distributable data set", "subscriber.downloadDistributableDataSet(\"LSE\", provider, redistributor)") {
        subscriber.downloadDistributableDataSet("LSE", provider, redistributor)
        Thread.sleep(7000)
        subscriber.displayDistributableDataSets()
        subscriber.displayDataSets()
        subscriber.displayTandCs()
    }

    logThenDo("Subscriber sign T&Cs", "subscriber.signTandC(\"provider.zip\", p)\n> subscriber.signTandC(\"redistributor.zip\", redistributor)") {
        subscriber.signTandC("provider.zip", provider)
        subscriber.signTandC("redistributor.zip", redistributor)
        subscriber.displaySignedTandCs()
    }

    logThenDo("Subscriber request permission", "subscriber.requestPermission(provider, redistributor, \"LSE\")") {
        subscriber.requestPermission(provider, redistributor, "LSE")
        Thread.sleep(5000)
        subscriber.displayPermissionRequests()
        redistributor.displayPermissionRequests()
        provider.displayPermissionRequests()
    }

    logThenDo("Subscriber create usages", "subscriber.createUsage(provider, redistributor, \"LSE\", \"Alice\")\n" +
            "> subscriber.createUsage(provider, redistributor, \"LSE\", \"Bob\")\n" +
            "> subscriber.createUsage(provider, redistributor, \"LSE\", \"Charlie\")") {

        subscriber.createUsage(provider, redistributor, "LSE", "Alice")
        subscriber.createUsage(provider, redistributor, "LSE", "Bob")
        subscriber.createUsage(provider, redistributor, "LSE", "Charlie")
        Thread.sleep(3000)
        subscriber.displayUsages()
        redistributor.displayUsages()
        provider.displayUsages()
    }

    logThenDo("Redistributor create bill", "redistributor.createBill(subscriber, ${LocalDate.now().minusWeeks(1).toString()}, ${LocalDate.now().plusWeeks(1).toString()})") {
        redistributor.createBill(subscriber, LocalDate.now().minusWeeks(1).toString(), LocalDate.now().plusWeeks(1).toString())
        Thread.sleep(5000)
        subscriber.displayBill()
    }
}

// Create a paid-for usage
fun demo2() {

    logThenDo("Subscriber display usage receipts", "subscriber.displayUsageReceipts()") {
        subscriber.displayUsageReceipts()
    }

    logThenDo("Redistributor2 download data set", "redistributor2.downloadDataSet(\"LSE\", provider)") {
        redistributor2.downloadDataSet("LSE", provider)
        Thread.sleep(5000)
        redistributor2.displayDataSets()
    }

    logThenDo("Redistributor2 create T&C","redistributor2.createTandC(\"redistributor2.zip\")"){
        redistributor2.createTandC("redistributor2.zip")
        redistributor2.displayTandCs()
    }

    logThenDo("Redistributor2 create distributable data set ","redistributor2.createDistributableDataSet(\"LSE\", provider, \"redistributor2.zip\", 2.1)"){
        redistributor2.createDistributableDataSet("LSE", provider, "redistributor2.zip", 2.1)
        redistributor2.displayDistributableDataSets()
    }

    logThenDo("Subscriber download distributable data set ","subscriber.downloadDistributableDataSet(\"LSE\", provider, redistributor2)"){
        subscriber.downloadDistributableDataSet("LSE", provider, redistributor2)
        Thread.sleep(5000)
        subscriber.displayDistributableDataSets()
    }

    logThenDo("Subscriber sign T&C","subscriber.signTandC(\"redistributor2.zip\", redistributor2)"){
        subscriber.signTandC("redistributor2.zip", redistributor2)
        subscriber.displaySignedTandCs()
    }

    logThenDo("Subscriber request permission","subscriber.requestPermission(provider, redistributor2, \"LSE\")"){
        subscriber.requestPermission(provider, redistributor2, "LSE")
        subscriber.displayPermissionRequests()
    }

    logThenDo("Subscriber create usage ","subscriber.createUsage(provider, redistributor2, \"LSE\", \"Dan\")"){
        subscriber.createUsage(provider, redistributor2, "LSE", "Dan")
    }

    logThenDo("Subscriber create usage with receipt ","subscriber.createUsage(provider, redistributor2, \"LSE\", \"Alice\", redistributor)"){
        subscriber.createUsage(provider, redistributor2, "LSE", "Alice", redistributor)
        Thread.sleep(3000)
        subscriber.displayUsages()
    }

    logThenDo("Redistributor2 create bill", "redistributor2.createBill(subscriber, ${LocalDate.now().minusWeeks(1).toString()}, ${LocalDate.now().plusWeeks(1).toString()})") {
        redistributor2.createBill(subscriber, LocalDate.now().minusWeeks(1).toString(), LocalDate.now().plusWeeks(1).toString())
        Thread.sleep(3000)

        subscriber.displayUsages()
        subscriber.displayBill()
    }
}







