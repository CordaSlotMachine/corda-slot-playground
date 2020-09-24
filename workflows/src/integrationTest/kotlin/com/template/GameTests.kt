package com.template

import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.flows.CreateStakeAccountFlow
import com.template.flows.GenerateResultForGameFlow
import com.template.flows.IssueGameConfigFlow
import com.template.flows.IssueUserFlow
import com.template.flows.ReserveTokensForGameFlow
import com.template.flows.StartGameFlow
import com.template.states.UserState
import com.template.states.UserStateInput
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_RESERVE_ACCOUNT
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
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

        aliceNode.startFlow(CreateStakeAccountFlow()).getOrThrow()

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

        val userInitAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.account!!.state.data.identifier.id))).states
        val casinoAliceInitAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states
        val casinoBobAmount: List<StateAndRef<FungibleToken>> = bobNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(bobMainAccount.state.data.identifier.id))).states
        val casinoCharlieAmount: List<StateAndRef<FungibleToken>> = charlieNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(charlieMainAccount.state.data.identifier.id))).states

        assertEquals(1000L,userInitAmount.first().state.data.amount.quantity)
        assertEquals(900000000,casinoAliceInitAmount.first().state.data.amount.quantity)
        assertEquals(900000000,casinoBobAmount.first().state.data.amount.quantity)
        assertEquals(900000000,casinoCharlieAmount.first().state.data.amount.quantity)

        val gameState = aliceNode.startFlow(StartGameFlow(userState.state.data,1)).getOrThrow()
        val res = aliceNode.startFlow(ReserveTokensForGameFlow(gameState.linearId)).getOrThrow()
        assertEquals(true, res)

        val userPostReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.account!!.state.data.identifier.id))).states
        val casinoAlicePostReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states
        val casinoBobPostReserveAmount: List<StateAndRef<FungibleToken>> = bobNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(bobMainAccount.state.data.identifier.id))).states
        val casinoCharliePostReserveAmount: List<StateAndRef<FungibleToken>> = charlieNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(charlieMainAccount.state.data.identifier.id))).states

        val userReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.reserveAccount!!.state.data.identifier.id))).states
        val casinoAliceReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceReserveAccount.state.data.identifier.id))).states
        val casinoBobReserveAmount: List<StateAndRef<FungibleToken>> = bobNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(bobReserveAccount.state.data.identifier.id))).states
        val casinoCharlieReserveAmount: List<StateAndRef<FungibleToken>> = charlieNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(charlieReserveAccount.state.data.identifier.id))).states

        assertEquals(1,userPostReserveAmount.size)
        assertEquals(1,casinoAlicePostReserveAmount.size)
        assertEquals(1,casinoBobPostReserveAmount.size)
        assertEquals(1,casinoCharliePostReserveAmount.size)

        assertEquals(900L,userPostReserveAmount.first().state.data.amount.quantity)
        assertEquals(899993200L,casinoAlicePostReserveAmount.first().state.data.amount.quantity)
        assertEquals(899993400L,casinoBobPostReserveAmount.first().state.data.amount.quantity)
        assertEquals(899993400L,casinoCharliePostReserveAmount.first().state.data.amount.quantity)
        assertEquals(100L,userReserveAmount.first().state.data.amount.quantity)
        assertEquals(6800L,casinoAliceReserveAmount.first().state.data.amount.quantity)
        assertEquals(6600L,casinoBobReserveAmount.first().state.data.amount.quantity)
        assertEquals(6600L,casinoCharlieReserveAmount.first().state.data.amount.quantity)

        val updatedGame = aliceNode.startFlow(GenerateResultForGameFlow(gameState.linearId)).getOrThrow()

        val userFinalAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.account!!.state.data.identifier.id))).states
        val casinoAliceFinalAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states
        val casinoBobFinalAmount: List<StateAndRef<FungibleToken>> = bobNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states
        val casinoCharlieFinalAmount: List<StateAndRef<FungibleToken>> = charlieNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceMainAccount.state.data.identifier.id))).states
        val userFinalReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(userState.state.data.reserveAccount!!.state.data.identifier.id))).states
        val casinoAliceFinalReserveAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceReserveAccount.state.data.identifier.id))).states
        val casinoBobFinalReserveAmount: List<StateAndRef<FungibleToken>> = bobNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceReserveAccount.state.data.identifier.id))).states
        val casinoCharlieFinalReserveAmount: List<StateAndRef<FungibleToken>> = charlieNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceReserveAccount.state.data.identifier.id))).states

        if(updatedGame.success == false){
            assertEquals(1,userFinalAmount.size)
            assertEquals(3,casinoAliceFinalAmount.size)
        } else {
            assertEquals(5,userFinalAmount.size)
            assertEquals(2,casinoAliceFinalAmount.size)
        }
        assertEquals(0,userFinalReserveAmount.size)
        assertEquals(0,casinoAliceFinalReserveAmount.size)
    }

}