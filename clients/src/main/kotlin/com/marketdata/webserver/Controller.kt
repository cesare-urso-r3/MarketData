package com.marketdata.webserver

import com.marketdata.contracts.PermissionContract
import com.marketdata.flows.PermissionIssueInitiator
import com.marketdata.flows.UsageIssueInitiator
import com.marketdata.states.PermissionState
import com.marketdata.states.UsageState
import com.sun.org.apache.xalan.internal.lib.NodeInfo
import net.corda.core.crypto.internal.providerMap
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.FlowHandle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*

interface JSONItem {
    override fun toString() : String
}


class JSONNamedItem<T>(val name : String, val item : T) : JSONItem where T : JSONItem {
    override fun toString(): String {
        return "\"$name\" : ${item.toString()}"
    }
}

class JSONString( val value : String ) : JSONItem {
    override fun toString(): String {
        return "\"$value\""
    }
}

class JSONInt( val value : Int) : JSONItem {
    override fun toString(): String {
        return value.toString()
    }
}

class JSONArray( val value : List<JSONItem> ) : JSONItem {
    // todo: don't allow mixed types to be added
    override fun toString(): String {
        var ret = ""

        for ((i, v) in value.withIndex()) {
            if (i > 0) {
                ret += ","
            }
            ret += v.toString()
        }
        return "[$ret]"
    }
}

class JSONObject( val value : List<JSONItem> ) : JSONItem {
    override fun toString(): String {
        var ret =  ""

        for ((i, v) in value.withIndex()) {
            if (i > 0) {
                ret += ","
            }
            ret += v.toString()
        }
        return "{$ret}"
    }
}
/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @PostMapping(value = ["/permission"])
    private fun setpermission(@RequestParam dataSet : String,
                            @RequestParam provider: String,
                            @RequestParam redistributor : String): ResponseEntity<String> {

        // TODO: need to check that valid parties are supplied else this will barf
        val tx = proxy.startFlowDynamic(PermissionIssueInitiator::class.java,
                dataSet,
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(provider)),
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(redistributor)))

        val headers = HttpHeaders()
        headers.put("Access-Control-Allow-Origin", mutableListOf("*"))
        return ResponseEntity<String>(
                "Transaction with id: ${tx.id} created", headers, HttpStatus.OK);
    }

    @GetMapping(value = "/permission", produces = arrayOf("text/plain"))
    private fun getPermission() : ResponseEntity<String> {
        var ret = JSONObject(
                listOf(JSONNamedItem("permissions",
                        JSONArray(
                            proxy.vaultQuery(PermissionState::class.java).states
                            .map {
                                val stateData = it.state.data
                                JSONObject (
                                    listOf(
                                            JSONNamedItem("dataSet", JSONString(stateData.dataSetName)),
                                            JSONNamedItem("provider", JSONString(stateData.provider.toString())),
                                            JSONNamedItem("subscriber", JSONString(stateData.subscriber.toString())),
                                            JSONNamedItem("dataChargeOwner", JSONString(stateData.dataChargeOwner.toString()))
                                    )
                                )
                            })
                    )
                )
        )

        val headers = HttpHeaders()
        headers.put("Access-Control-Allow-Origin", mutableListOf("*"))
        return ResponseEntity<String>(
                ret.toString(), headers, HttpStatus.OK);
    }

    @PostMapping(value = ["/usage"])
    private fun setusage(@RequestParam dataSet : String,
                          @RequestParam provider: String,
                          @RequestParam dataChargeOwner : String) : ResponseEntity<String> {

        // TODO: need to check that valid parties are supplied else this will barf
        val tx = proxy.startFlowDynamic(UsageIssueInitiator::class.java,
                dataSet,
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(provider)),
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(dataChargeOwner)))

        val headers = HttpHeaders()
        headers.put("Access-Control-Allow-Origin", mutableListOf("*"))
        return ResponseEntity<String>(
                "Transaction with id: ${tx.id} created", headers, HttpStatus.OK);
    }

    @GetMapping(value = "/usage", produces = arrayOf("text/plain"))
    private fun getUsage(@RequestParam dataSet : String,
                         @RequestParam provider: String,
                         @RequestParam dataChargeOwner : String) : ResponseEntity<String> {
        var ret = JSONObject(
                listOf(JSONNamedItem("usages",
                        JSONArray(
                                proxy.vaultQuery(UsageState::class.java).states
                                        .filter { it.state.data.dataSetName == dataSet &&
                                                it.state.data.provider.toString() == provider &&
                                                it.state.data.redistributor.toString() == dataChargeOwner }
                                        .map {
                                            val stateData = it.state.data
                                            JSONObject (
                                                    listOf(
                                                            JSONNamedItem("date", JSONString(stateData.date))
                                                    )
                                            )
                                        })
                    )
                )
        )

        val headers = HttpHeaders()
        headers.put("Access-Control-Allow-Origin", mutableListOf("*"))
        return ResponseEntity<String>(
                ret.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/party", produces = arrayOf("text/json"))
    private fun getParties() : ResponseEntity<String> {


        val parties = proxy.networkMapSnapshot().flatMap {
            nodeInfo -> (nodeInfo.legalIdentities - proxy.notaryIdentities().first()).map{ JSONString(it.toString()) }
        }

        var ret = JSONObject(
                    listOf(JSONNamedItem("parties", JSONArray(parties)))
        )

        val headers = HttpHeaders()
        headers.put("Access-Control-Allow-Origin", mutableListOf("*"))
        return ResponseEntity<String>(
                ret.toString(), headers, HttpStatus.OK);
    }
}