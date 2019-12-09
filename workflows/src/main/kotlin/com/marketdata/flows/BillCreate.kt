package com.marketdata.flows

import co.paralleluniverse.fibers.Suspendable
import com.marketdata.contracts.BillingContract
import com.marketdata.schema.UsageSchemaV1
import com.marketdata.states.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.*
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class BillCreate(private val fromDate: String,
                 private val toDate: String,
                 private val partyToBill : Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        val txb = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val cmd = BillingContract.Commands.Create()

        val usages = getUsageForPeriod(partyToBill, fromDate, toDate)
        val billAmount = calculateBill(usages)

        val billState = BillingState(fromDate, toDate, ourIdentity, partyToBill, billAmount)

        txb.withItems(
                StateAndContract(billState, BillingContract.ID),
                Command(cmd, ourIdentity.owningKey)
                )

        txb.verify(serviceHub)

        val ourSignedTx = serviceHub.signInitialTransaction(txb)

        val sessions = (billState.participants - ourIdentity)
                .map { initiateFlow(it) }

        return subFlow(FinalityFlow(ourSignedTx, sessions))
    }

    fun getUsageForPeriod( billableParty: Party, startDate : String, endDate : String) : List<UsageState> {
        return builder {

            val startDateIdx = UsageSchemaV1.PersistentUsage::date.greaterThanOrEqual(startDate)
            val endDateIdx = UsageSchemaV1.PersistentUsage::date.lessThanOrEqual(endDate)
            val partyIdx = UsageSchemaV1.PersistentUsage::subscriberName.equal(billableParty.name.toString())
            val redistIdx = UsageSchemaV1.PersistentUsage::redistributorName.equal(ourIdentity.name.toString())

            val customCriteria1 = VaultCustomQueryCriteria(startDateIdx)
            val customCriteria2 = VaultCustomQueryCriteria(endDateIdx)
            val customCriteria3 = VaultCustomQueryCriteria(partyIdx)
            val customCriteria4 = VaultCustomQueryCriteria(redistIdx)

            val criteria = QueryCriteria.VaultQueryCriteria()
                    .and(customCriteria1
                            .and(customCriteria2
                                    .and(customCriteria3
                                            and(customCriteria4))))

            serviceHub.vaultService.queryBy(UsageState::class.java, criteria)
        }.states.map { it.state.data }
    }

    fun calculateBill(usages : List<UsageState>) : Double {

        val distinctUsages = usages.distinctBy { "${it.dataSetName}|${it.provider}|{${it.date}|${it.userName}" }

        var providerCharges = 0.0
        var distributorCharges = 0.0

        distinctUsages.forEach {
            val permState = it.permissionState.resolveToState(serviceHub)
            val distDataSet = permState.distributableDataSet.resolveToState(serviceHub)
            val dataSet = distDataSet.dataSet.resolveToState(serviceHub)

            if (it.paidUsageState != null) {
                providerCharges += distDataSet.pricingParameters
                        .map { pParam -> pParam.monthlyCostPerUser }
                        .reduce{sum, price -> sum + price }
            }

            distributorCharges += dataSet.pricingParameters
                    .map { pParam -> pParam.monthlyCostPerUser }
                    .reduce{sum, price -> sum + price }
        }

        return providerCharges + distributorCharges
    }
}

@InitiatedBy(BillCreate::class)
class BillCreateResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

