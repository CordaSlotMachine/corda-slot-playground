package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.template.contracts.UserContract
import com.template.states.UserState
import com.template.states.UserStateInput
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.getOrThrow

@StartableByRPC
@InitiatingFlow
class IssueUserWrapperFlow(private val accountInfo: StateAndRef<AccountInfo>, private val userState: UserStateInput) : FlowLogic<StateAndRef<UserState>>() {
    @Suspendable
    override fun call(): StateAndRef<UserState> {
        return (subFlow(IssueUserFlow(setOf(initiateFlow(accountInfo.state.data.host)), accountInfo, userState)))
    }
}

@InitiatedBy(IssueUserWrapperFlow::class)
class IssueUserWrapperResponse(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherSession))
    }

}
@StartableByRPC
@InitiatingFlow
class IssueUserFlow(
        private val sessions: Collection<FlowSession>,
        private val accountInfo: StateAndRef<AccountInfo>,
        private val user: UserStateInput) : FlowLogic<StateAndRef<UserState>>() {

    @Suspendable
    override fun call(): StateAndRef<UserState> {
        val keyToUse = if (accountInfo.state.data.host == ourIdentity) {
            serviceHub.createKeyForAccount(accountInfo.state.data).owningKey
        } else {
            subFlow(RequestKeyForAccount(accountInfo.state.data)).owningKey
        }
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(UserContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(UserState(user.user, user.password, keyToUse, accountInfo))
        val signedTxLocally = serviceHub.signInitialTransaction(txBuilder)
        val finalizedTx = subFlow(FinalityFlow(signedTxLocally, sessions.filterNot { it.counterparty.name == ourIdentity.name }))
        return finalizedTx.coreTransaction.outRefsOfType(UserState::class.java).single()
    }
}

class IssueAccountHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherSession))
    }
}


@StartableByRPC
class CreateAccountFlow(private val player: UserStateInput) : FlowLogic<StateAndRef<AccountInfo>>() {
    @Suspendable
    override fun call(): StateAndRef<AccountInfo> {
        val accountService = serviceHub.accountService
        return accountService.createAccount(player.user).getOrThrow()
    }
}