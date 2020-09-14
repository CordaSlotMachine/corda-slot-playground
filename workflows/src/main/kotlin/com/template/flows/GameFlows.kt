package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.GameContract
import com.template.contracts.UserContract
import com.template.states.GameState
import com.template.states.UserState
import com.template.states.UserStateInput
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class StartGameWrapperFlow(private val user: UserState, private val stake: Long) : FlowLogic<GameState>() {
    @Suspendable
    override fun call(): GameState {
        val party = subFlow(RequestKeyForAccount(user.account!!.state.data))
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(GameContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(GameState(user = user, stake = stake, participants = listOf(party)))
        return GameState(user = user, stake = stake, participants = listOf(party))
    }
}

//class ReserveTokensFlow(private val game: GameState, private val stake: Long) : FlowLogic<StateAndRef<GameState>>() {
//    @Suspendable
//    override fun call(): StateAndRef<GameState> {
//        val party = subFlow(RequestKeyForAccount(user.account!!.state.data))
//        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
//        txBuilder.addCommand(GameContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
//        txBuilder.addOutputState(GameState(user = user, stake = stake, participants = listOf(party)))
//
//
//    }
//}