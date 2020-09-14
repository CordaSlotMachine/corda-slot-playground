package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
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
class IssueUserWrapperFlow(private val userState: UserStateInput, private val accountInfo: StateAndRef<AccountInfo>, private val reserveAccountInfo: StateAndRef<AccountInfo> ) : FlowLogic<StateAndRef<UserState>>() {
    @Suspendable
    override fun call(): StateAndRef<UserState> {
        return (subFlow(IssueUserFlow(setOf(initiateFlow(accountInfo.state.data.host)), userState, accountInfo, reserveAccountInfo )))
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
        private val user: UserStateInput,
        private val accountInfo: StateAndRef<AccountInfo>,
        private val reserveAccountInfo: StateAndRef<AccountInfo>
        ) : FlowLogic<StateAndRef<UserState>>() {

    @Suspendable
    override fun call(): StateAndRef<UserState> {
        val party = if (accountInfo.state.data.host == ourIdentity) {
            serviceHub.createKeyForAccount(accountInfo.state.data)
        } else {
            subFlow(RequestKeyForAccount(accountInfo.state.data))
        }
        val tokens = listOf(10 of EUR issuedBy ourIdentity heldBy party)
        subFlow(IssueTokens(tokens))
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(UserContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(UserState(user.name, user.password, party.owningKey, accountInfo, reserveAccountInfo))
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
class CreateAccountFlow(private val name: String) : FlowLogic<StateAndRef<AccountInfo>>() {
    @Suspendable
    override fun call(): StateAndRef<AccountInfo> {
        val accountService = serviceHub.accountService
        return accountService.createAccount(name).getOrThrow()
    }
}