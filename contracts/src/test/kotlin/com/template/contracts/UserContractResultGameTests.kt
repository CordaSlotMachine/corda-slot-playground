package com.template.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.states.GameState
import com.template.states.GameStep
import com.template.states.UserState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test


class UserContractResultGameTests {

    private val playerId = TestIdentity(CordaX500Name("player", "London", "GB"))
    private val casinoId = TestIdentity(CordaX500Name("casino", "London", "GB"))
    private val player = playerId.identity.party
    private val casino = casinoId.identity.party
    private val notary = TestIdentity(CordaX500Name("notary", "London", "GB"))
    private val ledgerServices = MockServices(cordappPackages = listOf("com.template.contracts", "net.corda.testing.contracts", "com.r3.corda.lib.accounts.contracts"),
            firstIdentity = notary,
            networkParameters = testNetworkParameters().copy(minimumPlatformVersion = 4))

    @Test
    fun `Result Command needs a game state with finished game step as output`() {

        ledgerServices.ledger {
            val accountInfoStateRef: StateAndRef<AccountInfo> = mockStateAndRefAccountInfo(player);
            val playerState = UserState( account = accountInfoStateRef, reserveAccount = accountInfoStateRef, userParty = player.anonymise(), participants = listOf(player, casino))

            transaction {

                output(UserContract::class.java.canonicalName, GameState(playerState, stake = 100, participants = listOf(player, casino), result = null, success = null))

                failsWith("A transaction must contain at least one command")
                command(player.owningKey, GameContract.RESULT)
                failsWith("At least one input states is required for for result command")
                val gameState = GameState(playerState, stake = 100, participants = listOf(player, casino), result = null, success = null)
                input(UserContract::class.java.canonicalName, gameState)
                tweak {
                    output(UserContract::class.java.canonicalName, gameState)
                    failsWith("An output step with a 'GameStep.FINISHED' field is required for for result command")
                }
                output(UserContract::class.java.canonicalName, gameState.copy(step = GameStep.FINISHED))
                verifies()
            }

        }
    }


}

