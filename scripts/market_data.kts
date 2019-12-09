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
import com.marketdata.states.*
import com.marketdata.flows.*
import com.marketdata.schema.*
import com.marketdata.data.*

// Kotlin shell helper script

//val provProxy = connect(10006)
//val redistProxy = connect(10009)
//val subProxy = connect(10012)

//val provider = getParty(provProxy)
//val redistributor = getParty(redistProxy)
//val subscriber = getParty(subProxy)

fun <T : ContractState> CordaRPCOps.displayStates(clazz : Class<T>) {
    val criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
    val result = vaultQueryByCriteria<ContractState>(criteria, clazz)
    result.states.forEach{
        println(it.state.data)
    }
}

class rpcParty (val port : Int) {
    var proxy : CordaRPCOps
    var party : PartyAndCertificate

    init {
        proxy = connect(port)!!
        party = getParty(proxy)
    }

    fun createTandC(filename : String) {
        proxy.startFlowDynamic(com.marketdata.flows.IssueTandC::class.java, attach(filename), filename)
    }

    fun displayTandCs() {
        proxy.displayStates(com.marketdata.states.TermsAndConditionsState::class.java)
    }

    fun displayDataSets() {
        proxy.displayStates(com.marketdata.states.DataSetState::class.java)
    }

    fun createDataSet(dataSetName : String, tandCName : String) {

        val tAndCIdx = TermsAndConditionsSchemaV1.PersistentTandC::name.equal(dataSetName)
        val issuerIdx = TermsAndConditionsSchemaV1.PersistentTandC::issuerName.equal(party.name.toString())

        val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(tAndCIdx)
        val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(issuerIdx)

        val criteria = QueryCriteria.VaultQueryCriteria()
                .and(customCriteria1
                        .and(customCriteria2))

        val result = proxy.vaultQueryByCriteria<ContractState>(
                criteria,
                com.marketdata.states.TermsAndConditionsState::class.java)

        val tandcState = result.states.single().state.data

        proxy.startFlowDynamic(com.marketdata.flows.DataSetIssue::class.java, dataSetName,  tandcState, PricingParameter(10.0))
    }

    private fun attach(filename: String) : SecureHash? {
        val stream = java.io.File("files/$filename").inputStream()
        return proxy.uploadAttachment(stream)
    }

}

val provider = rpcParty(10006)

fun listFiles() {
    File("files/").list().forEach {
        println(it)
    }
}




