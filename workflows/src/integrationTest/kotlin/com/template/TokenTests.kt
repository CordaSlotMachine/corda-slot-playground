package com.template

import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.template.flows.IssueUserWrapperFlow
import com.template.flows.MoveTokenFlow
import com.template.states.UserState
import com.template.states.UserStateInput
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
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


class TokenTests {

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
    fun `move tokens`() {
        val aliceService = aliceNode.services.accountService
        val aliceAccount = aliceService.createAccount("TEST_ACCOUNT").getOrThrow()
        val casinoAccount = aliceService.createAccount("TEST_ACCOUNT2").getOrThrow()

        val user1 = UserStateInput("user1", "password", null)
        val userState = aliceNode.startFlow(IssueUserWrapperFlow(aliceAccount, user1)).getOrThrow()

        Assert.assertThat(userState.state.data, `is`(notNullValue(UserState::class.java)))

        aliceNode.transaction {
            val owningAccount = aliceService.accountInfo(userState.state.data.owningKey!!)
            Assert.assertThat(owningAccount!!.state.data.identifier, `is`(IsEqual.equalTo(aliceAccount.state.data.identifier)))
        }

        aliceNode.startFlow(MoveTokenFlow(aliceAccount.state.data, casinoAccount.state.data, 6))
        Thread.sleep(2000)

        val aliceAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(aliceAccount.state.data.identifier.id))).states
        val casinoAmount: List<StateAndRef<FungibleToken>> = aliceNode.services.vaultService.queryBy(FungibleToken::class.java, VaultQueryCriteria()
                .withExternalIds(listOf(casinoAccount.state.data.identifier.id))).states

        assertEquals(600L,casinoAmount.first().state.data.amount.quantity)
        assertEquals(400L,aliceAmount.first().state.data.amount.quantity)


    }

}