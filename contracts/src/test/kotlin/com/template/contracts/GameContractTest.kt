package com.template.contracts

import com.template.states.GameCombination
import com.template.states.GameConfigState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Test


class GameContractTests {

    private val playerId = TestIdentity(CordaX500Name("player", "London", "GB"))
    private val casinoId = TestIdentity(CordaX500Name("casino", "London", "GB"))
    private val player = playerId.identity.party
    private val casino = casinoId.identity.party
    private val notary = TestIdentity(CordaX500Name("notary", "London", "GB"))
    private val ledgerServices = MockServices(cordappPackages = listOf("com.template.contracts"),
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
        ledgerServices.transaction {

            output(GameContract::class.java.canonicalName, GameConfigState(gameCombinations, 200, listOf(casino), UniqueIdentifier()))
            failsWith("A transaction must contain at least one command")
            command(player.owningKey, GameContract.CREATE)
            verifies()
        }
    }




}