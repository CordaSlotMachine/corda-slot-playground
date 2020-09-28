package com.template.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.states.StakeDeposit
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test


class StakeContractStakeTests {

    private val playerId = TestIdentity(CordaX500Name("player", "London", "GB"))
    private val casinoId = TestIdentity(CordaX500Name("casino", "London", "GB"))
    private val player = playerId.identity.party
    private val casino = casinoId.identity.party
    private val notary = TestIdentity(CordaX500Name("notary", "London", "GB"))
    private val ledgerServices = MockServices(cordappPackages = listOf("com.template.contracts", "net.corda.testing.contracts", "com.r3.corda.lib.accounts.contracts"),
            firstIdentity = notary,
            networkParameters = testNetworkParameters().copy(minimumPlatformVersion = 4))


    @Test
    fun `Stake Command needs a StakeDeposit state as output`() {

        ledgerServices.ledger {

            transaction {
                val zeroDeposit = StakeDeposit(AccountInfo("casino", casino, UniqueIdentifier()), 0, UniqueIdentifier(), listOf(casino, player))
                tweak {
                    output(StakeContract::class.java.canonicalName, zeroDeposit)
                    failsWith("A transaction must contain at least one command")
                    command(player.owningKey, StakeContract.STAKE)
                    failsWith("a StakeDeposit with a positive deposit  required for the output of a StakeContract with STAKE command")
                }
                //This time with a valid amount
                command(player.owningKey, StakeContract.STAKE)
                output(StakeContract::class.java.canonicalName, zeroDeposit.copy(amount = 1000000L))
                verifies()

            }
        }

    }
}








