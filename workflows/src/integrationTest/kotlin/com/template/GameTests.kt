package com.template

import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.template.flows.GenerateResultForGameFlow
import com.template.flows.IssueGameConfigFlow
import com.template.flows.IssueUserFlow
import com.template.flows.MoveTokenFlow
import com.template.flows.ReserveTokensForGameFlow
import com.template.flows.StartGameFlow
import com.template.states.UserState
import com.template.states.UserStateInput
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals


class GameTests {

    private lateinit var mockNet: MockNetwork
    private lateinit var aliceNode: StartedMockNode
    private lateinit var bobNode: StartedMockNode
    private lateinit var charlieNode: StartedMockNode
    private lateinit var alice: Party
    private lateinit var bob: Party
    private lateinit var charlie: Party
    private lateinit var notary: Party
    private val REQUIRED_CORDAPP_PACKAGES = listOf(
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.money"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.ci")
    )

    @Before
    fun before() {
        mockNet = MockNetwork(
                parameters = MockNetworkParameters(
                        cordappsForAllNodes = REQUIRED_CORDAPP_PACKAGES,
                        threadPerNode = true,
                        networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
                )
        )
        aliceNode = mockNet.createPartyNode(ALICE_NAME)
        //bobNode = mockNet.createPartyNode(BOB_NAME)
        //charlieNode = mockNet.createPartyNode(CHARLIE_NAME)
        alice = aliceNode.info.singleIdentity()
        //bob = bobNode.info.singleIdentity()
        //charlie = charlieNode.info.singleIdentity()
        notary = mockNet.defaultNotaryIdentity

        mockNet.startNodes()
    }

    @After
    fun cleanUp() {
        mockNet.stopNodes()
    }


    @Test
    fun `game test`() {
        val aliceService = aliceNode.services.accountService
        val casinoAccount = aliceService.createAccount("CASINO_ACCOUNT").getOrThrow()
        val casinoReserveAccount = aliceService.createAccount("CASINO_RESERVE_ACCOUNT").getOrThrow()

        val tokens = listOf(999999999999 of EUR issuedBy aliceNode.services.ourIdentity heldBy aliceNode.services.ourIdentity.toParty(aliceNode.services))
        aliceNode.startFlow(IssueTokens(tokens))

        val user1 = UserStateInput("user1", "password", null)
        val userState = aliceNode.startFlow(IssueUserFlow(user1)).getOrThrow()

        Assert.assertThat(userState.state.data, `is`(notNullValue(UserState::class.java)))

        aliceNode.transaction {
            val owningAccount = aliceService.accountInfo(userState.state.data.userParty.owningKey!!)
            Assert.assertThat(owningAccount!!.state.data.name, `is`(IsEqual.equalTo(user1.name)))
        }

        aliceNode.startFlow(IssueGameConfigFlow())
        Thread.sleep(2000)

        val gameState = aliceNode.startFlow(StartGameFlow(userState.state.data,1)).getOrThrow()
        val res = aliceNode.startFlow(ReserveTokensForGameFlow(gameState.linearId)).getOrThrow()
        val updatedGame = aliceNode.startFlow(GenerateResultForGameFlow(gameState.linearId)).getOrThrow()
        Thread.sleep(2000)


    }

}