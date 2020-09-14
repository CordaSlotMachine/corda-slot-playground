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
import com.template.utils.getAllParticipants
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
class IssueUserFlow(private val user: UserStateInput) : FlowLogic<StateAndRef<UserState>>() {

    @Suspendable
    override fun call(): StateAndRef<UserState> {
            val accountService = serviceHub.accountService
            val userAccount = accountService.createAccount(user.name).getOrThrow()
            val userReserveAccount = accountService.createAccount(user.name+"-RESERVE").getOrThrow()

            val party = if (userAccount.state.data.host == ourIdentity) {
                serviceHub.createKeyForAccount(userAccount.state.data)
            } else {
                subFlow(RequestKeyForAccount(userAccount.state.data))
            }
            val tokens = listOf(10 of EUR issuedBy ourIdentity heldBy party)
            subFlow(IssueTokens(tokens))
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            txBuilder.addCommand(UserContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
            txBuilder.addOutputState(UserState(user.name, user.password, userAccount, userReserveAccount, party, participants = serviceHub.getAllParticipants()))
            txBuilder.verify(serviceHub)
            val signedTx = serviceHub.signInitialTransaction(txBuilder)
            val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
            val finalizedTx = subFlow(FinalityFlow(signedTx, allOtherParticipants.map { initiateFlow(it) }))
            return finalizedTx.coreTransaction.outRefsOfType(UserState::class.java).single()
    }
}


@InitiatedBy(IssueUserFlow::class)
class IssueUserResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}