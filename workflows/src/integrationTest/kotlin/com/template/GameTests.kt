package com.template

import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
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
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_RESERVE_ACCOUNT
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.CHARLIE_NAME
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
        bobNode = mockNet.createPartyNode(BOB_NAME)
        charlieNode = mockNet.createPartyNode(CHARLIE_NAME)
        alice = aliceNode.info.singleIdentity()
        bob = bobNode.info.singleIdentity()
        charlie = charlieNode.info.singleIdentity()
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
        val bobService = bobNode.services.accountService
        val charlieService = charlieNode.services.accountService
        val aliceMainAccount = aliceService.createAccount(CASINO_ACCOUNT).getOrThrow()
        val bobMainAccount = bobService.createAccount(CASINO_ACCOUNT).getOrThrow()
        val charlieMainAccount = charlieService.createAccount(CASINO_ACCOUNT).getOrThrow()

        val aliceReserveAccount = aliceService.createAccount(CASINO_RESERVE_ACCOUNT).getOrThrow()
        val bobReserveAccount = bobService.createAccount(CASINO_RESERVE_ACCOUNT).getOrThrow()
        val charlieReserveAccount = charlieService.createAccount(CASINO_RESERVE_ACCOUNT).getOrThrow()

        val partyAlice = aliceNode.startFlow(RequestKeyForAccount(aliceMainAccount.state.data)).getOrThrow()
        val aliceTokens = listOf(10000000.00 of EUR issuedBy alice heldBy partyAlice)
        aliceNode.startFlow(IssueTokens(aliceTokens)).getOrThrow()

        val partyBob = aliceNode.startFlow(RequestKeyForAccount(bobMainAccount.state.data)).getOrThrow()
        val bobTokens = listOf(10000000.00 of EUR issuedBy bob heldBy partyBob)
        bobNode.startFlow(IssueTokens(bobTokens)).getOrThrow()

        val partyCharlie = aliceNode.startFlow(RequestKeyForAccount(charlieMainAccount.state.data)).getOrThrow()
        val charlieTokens = listOf(10000000.00 of EUR issuedBy charlie heldBy partyCharlie)
        charlieNode.startFlow(IssueTokens(charlieTokens)).getOrThrow()

        val user1 = UserStateInput("user1", "password", null)
        val userState = aliceNode.startFlow(IssueUserFlow(user1)).getOrThrow()

        Assert.assertThat(userState.state.data, `is`(notNullValue(UserState::class.java)))

        aliceNode.transaction {
            val owningAccount = aliceService.accountInfo(userState.state.data.userParty.owningKey!!)
            Assert.assertThat(owningAccount!!.state.data.name, `is`(IsEqual.equalTo(user1.name)))
        }

        aliceNode.startFlow(IssueGameConfigFlow())
        bobNode.startFlow(IssueGameConfigFlow())
        charlieNode.startFlow(IssueGameConfigFlow())

        val aliceInitAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.account!!.state.data.identifier.id))).states
        val casinoInitAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states

        assertEquals(1000L,aliceInitAmount.first().state.data.amount.quantity)
        assertEquals(1000000000,casinoInitAmount.first().state.data.amount.quantity)

        val gameState = aliceNode.startFlow(StartGameFlow(userState.state.data,1)).getOrThrow()
        val res = aliceNode.startFlow(ReserveTokensForGameFlow(gameState.linearId)).getOrThrow()
        assertEquals(true, res)

        val alicePostReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.account!!.state.data.identifier.id))).states
        val casinoPostReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states

        val aliceReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.reserveAccount!!.state.data.identifier.id))).states
        val casinoReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceReserveAccount.state.data.identifier.id))).states

        assertEquals(1,alicePostReserveAmount.size)
        assertEquals(1,casinoPostReserveAmount.size)
        assertEquals(900L,alicePostReserveAmount.first().state.data.amount.quantity)
        assertEquals(999993200L,casinoPostReserveAmount.first().state.data.amount.quantity)
        assertEquals(100L,aliceReserveAmount.first().state.data.amount.quantity)
        assertEquals(6800L,casinoReserveAmount.first().state.data.amount.quantity)

        val updatedGame = aliceNode.startFlow(GenerateResultForGameFlow(gameState.linearId)).getOrThrow()

        val aliceFinalAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.account!!.state.data.identifier.id))).states
        val casinoFinalAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states
        val aliceFinalReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.reserveAccount!!.state.data.identifier.id))).states
        val casinoFinalReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceReserveAccount.state.data.identifier.id))).states

        if(updatedGame.success == false){
            assertEquals(1,aliceFinalAmount.size)
            assertEquals(3,casinoFinalAmount.size)
        } else {
            assertEquals(5,aliceFinalAmount.size)
            assertEquals(2,casinoFinalAmount.size)
        }
        assertEquals(0,aliceFinalReserveAmount.size)
        assertEquals(0,casinoFinalReserveAmount.size)
    }

}