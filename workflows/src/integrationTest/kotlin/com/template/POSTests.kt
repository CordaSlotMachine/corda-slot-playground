package com.template

import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.testing.node.TestCordapp

import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.flows.CreateStakeAccountFlow
import com.template.flows.IssueUserFlow
import com.template.flows.MoveTokenFlow
import com.template.states.UserState
import com.template.states.UserStateInput
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_STAKE_ACCOUNT
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.*
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class POSTests {

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
    fun `POS flow test`() {
        val aliceService = aliceNode.services.accountService
        val bobService = bobNode.services.accountService
        val charlieService = charlieNode.services.accountService
        val aliceMainAccount = aliceService.createAccount(CASINO_ACCOUNT).getOrThrow()
        val bobMainAccount = bobService.createAccount(CASINO_ACCOUNT).getOrThrow()
        val charlieMainAccount = charlieService.createAccount(CASINO_ACCOUNT).getOrThrow()

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

        val aliceAccount = aliceService.accountInfo(CASINO_STAKE_ACCOUNT).single()
        val bobAccount = bobService.accountInfo(CASINO_STAKE_ACCOUNT).single()
        val charlieAccount = charlieService.accountInfo(CASINO_STAKE_ACCOUNT).single()

        val bobAccount2 = bobService.accountInfo(CASINO_ACCOUNT).single()


        assertNotNull(aliceAccount)
        assertNotNull(bobAccount)
        assertNotNull(charlieAccount)


        val aliceAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(aliceAccount.state.data.identifier.id))).states
        val bobAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(bobAccount.state.data.identifier.id))).states
        val charlieAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(charlieAccount.state.data.identifier.id))).states

        assertEquals(3, aliceAmount.size)
        assertEquals(3, bobAmount.size)
        assertEquals(3, charlieAmount.size)
        aliceAmount.forEach {
            assertEquals(100000000, it.state.data.amount.quantity)
        }
        bobAmount.forEach {
            assertEquals(100000000, it.state.data.amount.quantity)
        }
        charlieAmount.forEach {
            assertEquals(100000000, it.state.data.amount.quantity)
        }

    }

}