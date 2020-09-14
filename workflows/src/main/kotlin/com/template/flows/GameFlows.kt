package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.GameContract
import com.template.contracts.UserContract
import com.template.states.GameCombination
import com.template.states.GameConfigState
import com.template.states.GameState
import com.template.states.GameStep
import com.template.states.UserState
import com.template.states.UserStateInput
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_RESERVE_ACCOUNT
import com.template.utils.getAllParticipants
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder

@StartableByService
@InitiatingFlow
class IssueGameConfigFlow() : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val gameCombinations: Array<GameCombination> = arrayOf(
                GameCombination( "6", "6", "6", 200),
                GameCombination("4", "4", "4", 50),
                GameCombination( "2", "2", "2", 20),
                GameCombination( "1/3", "5/2", "4/6", 15),
                GameCombination( "5", "5", "5", 13),
                GameCombination( "1", "1", "1", 12),
                GameCombination( "3", "3", "3", 10),
                GameCombination( "1/3/5", "1/3/5", "1/3/5", 4)
        )
        txBuilder.addCommand(GameContract.CONFIG, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(GameConfigState(gameCombinations, 200, listOf(serviceHub.ourIdentity)))
        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        subFlow(FinalityFlow(signedTx, allOtherParticipants.map { initiateFlow(it) }))
    }
}

@InitiatedBy(IssueGameConfigFlow::class)
class IssueGameConfigResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}


@StartableByRPC
@InitiatingFlow
class StartGameFlow(private val user: UserState, private val stake: Long) : FlowLogic<GameState>() {
    @Suspendable
    override fun call(): GameState {
        val party = subFlow(RequestKeyForAccount(user.account!!.state.data))
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val game = GameState(user = user, stake = stake, participants = listOf(party), result = null, success = null)
        txBuilder.addCommand(GameContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(game)
        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        subFlow(FinalityFlow(signedTx, allOtherParticipants.map { initiateFlow(it) }))
        return game
    }
}

@InitiatedBy(StartGameFlow::class)
class StartGameResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}

@StartableByRPC
@InitiatingFlow
class ReserveTokensForGameFlow(private val gameId: UniqueIdentifier) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val gameConfig = serviceHub.vaultService.queryBy(GameConfigState::class.java)
        val game = serviceHub.vaultService.queryBy<GameState>(QueryCriteria.LinearStateQueryCriteria(linearId = listOf(gameId))).states.single()
        val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
        val casinoReserveAccount = serviceHub.accountService.accountInfo(CASINO_RESERVE_ACCOUNT).single()
        txBuilder.addCommand(GameContract.RESERVE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addInputState(game)
        txBuilder.addOutputState(game.state.data.copy(step = GameStep.RESERVED))
        subFlow(MoveTokenFlow(game.state.data.user.account!!.state.data, game.state.data.user.reserveAccount!!.state.data, game.state.data.stake))
        subFlow(MoveTokenFlow(casinoAccount.state.data, casinoReserveAccount.state.data, game.state.data.stake*gameConfig.states.single().state.data.maxMultiplier))
        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        subFlow(FinalityFlow(signedTx, allOtherParticipants.map { initiateFlow(it) }))
    }
}

@InitiatedBy(ReserveTokensForGameFlow::class)
class ReserveTokensForGameResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}