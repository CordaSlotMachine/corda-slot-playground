package com.template.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.states.GameCombination
import com.template.states.GameState
import com.template.states.UserState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.transaction
import org.junit.Test


class GameContractTests {

    private val playerId = TestIdentity(CordaX500Name("player", "London", "GB"))
    private val casinoId = TestIdentity(CordaX500Name("casino", "London", "GB"))
    private val player = playerId.identity.party
    private val casino = casinoId.identity.party
    private val notary = TestIdentity(CordaX500Name("notary", "London", "GB"))
    private val ledgerServices = MockServices(cordappPackages = listOf("com.template.contracts","net.corda.testing.contracts","com.r3.corda.lib.accounts.contracts"),
            firstIdentity = notary,
            networkParameters = testNetworkParameters().copy(minimumPlatformVersion = 4))


    private val gameCombinations: Array<GameCombination> = arrayOf(
            GameCombination("6", "6", "6", 200),
            GameCombination("4", "4", "4", 50),
            GameCombination("2", "2", "2", 20),
            GameCombination("1/3", "5/2", "4/6", 15),
            GameCombination("5", "5", "5", 13),
            GameCombination("1", "1", "1", 12),
            GameCombination("3", "3", "3", 10),
            GameCombination("1/3/5", "1/3/5", "1/3/5", 4)
    )

    @Test
    fun `Create contract needs a command`() {
        ledgerServices.ledger {
            val accountInfoStateRef: StateAndRef<AccountInfo> = mockStateAndRefAccountInfo(player);
            val playerState = UserState("player", password = "pwd", account = accountInfoStateRef, reserveAccount = accountInfoStateRef, userParty = player.anonymise(), participants = listOf(player, casino))
            ledgerServices.transaction {

              output(UserContract::class.java.canonicalName, GameState(playerState, stake = 100, participants = listOf(player,casino), result = null, success = null))
                failsWith("A transaction must contain at least one command")
                command(player.owningKey, GameContract.CREATE)
                verifies()
            }
        }
    }


    @Test
    fun `Create Command needs a game state as output`() {

        ledgerServices.ledger {
            val accountInfoStateRef:StateAndRef<AccountInfo> = mockStateAndRefAccountInfo(player);
            val playerState = UserState("player", password = "pwd",account = accountInfoStateRef, reserveAccount=accountInfoStateRef,userParty= player.anonymise(), participants = listOf(player,casino))

            transaction {

                output(UserContract::class.java.canonicalName,  GameState(playerState, stake = 100, participants = listOf(player,casino), result = null, success = null))

                failsWith("A transaction must contain at least one command")
                command(player.owningKey, GameContract.CREATE)
                    verifies()
                }

            }
    }






}

