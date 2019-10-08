package com.marketdata.contracts

import com.marketdata.ALICE
import com.marketdata.BOB
import com.marketdata.CHARLIE
import com.marketdata.MEGACORP
import com.marketdata.MINICORP
import com.marketdata.states.PermissionState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class PermissionContractTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    @Test
    fun mustIncludeIssueCommand() {
        val perm = PermissionState("data_set", ALICE.party, BOB.party)
        ledgerServices.ledger {
            transaction {
                output(PermissionContract.ID, perm)
                command(listOf(ALICE.publicKey, BOB.publicKey), DummyCommand()) // Wrong type.
                this.fails()
            }
            transaction {
                output(PermissionContract.ID, perm)
                command(listOf(ALICE.publicKey, BOB.publicKey), PermissionContract.Commands.Issue()) // Correct type.
                this.verifies()
            }
        }
    }

    @Test
    fun subscriberAndProviderCannotBeTheSame() {
        val perm = PermissionState("data_set", ALICE.party, BOB.party)
        val subIsProv = PermissionState("data_set", ALICE.party, ALICE.party)
        ledgerServices.ledger {
            transaction {
                command(listOf(ALICE.publicKey, BOB.publicKey),PermissionContract.Commands.Issue())
                output(PermissionContract.ID, subIsProv)
                this `fails with` "The subscriber and provider cannot be the same."
            }
            transaction {
                command(listOf(ALICE.publicKey, BOB.publicKey), PermissionContract.Commands.Issue())
                output(PermissionContract.ID, perm)
                this.verifies()
            }
        }
    }
}