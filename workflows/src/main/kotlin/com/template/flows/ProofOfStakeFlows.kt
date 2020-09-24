package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfoFlow
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfoHandlerFlow
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.StakeContract
import com.template.contracts.UserContract
import com.template.states.StakeDeposit
import com.template.states.UserState
import com.template.states.UserStateInput
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_STAKE_ACCOUNT
import com.template.utils.STAKE_AMOUNT
import com.template.utils.getAllParticipants
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.unwrap
import java.security.PublicKey

@StartableByRPC
@InitiatingFlow
class CreateStakeAccountFlow() : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val accountService = serviceHub.accountService
        val stakeAccount = accountService.createAccount(CASINO_STAKE_ACCOUNT).getOrThrow()
        val casinoAccount = accountService.accountInfo(CASINO_ACCOUNT)
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(StakeContract.STAKE, serviceHub.myInfo.legalIdentities.first().owningKey)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        val deposit = StakeDeposit(casinoAccount.single().state.data, STAKE_AMOUNT, participants = serviceHub.getAllParticipants())
        txBuilder.addOutputState(deposit)
        txBuilder.verify(serviceHub)


        subFlow(MoveTokenFlow(casinoAccount.single().state.data,stakeAccount.state.data, STAKE_AMOUNT))
        subFlow(ShareAccountInfo(stakeAccount,allOtherParticipants))

        txBuilder.verify(serviceHub)
        val initiatedParticipants = allOtherParticipants.map { initiateFlow(it) }
        initiatedParticipants.forEach {
            val partyDeposit = it.sendAndReceive<StakeDeposit>(deposit).unwrap { it }
            requireThat {
                "Deposit must not be null" using (deposit != null)
                "Deposit amount must be STAKE_AMOUNT" using (deposit.amount == STAKE_AMOUNT)
                "Incoming deposit account must not be our account" using (partyDeposit.account.identifier != casinoAccount.single().state.data.identifier )
            }
            txBuilder.addOutputState(partyDeposit)
        }
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, initiatedParticipants))
        subFlow(FinalityFlow(fullySignedTx, initiatedParticipants))
    }
}


@InitiatedBy(CreateStakeAccountFlow::class)
class CreateStakeAccountResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val deposit = counterPartySession.receive<StakeDeposit>().unwrap { it }
        val stakeAccount = serviceHub.accountService.accountInfo(CASINO_STAKE_ACCOUNT).single()
        val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
        requireThat {
            "Deposit must not be null" using (deposit != null)
            "Deposit amount must be STAKE_AMOUNT" using (deposit.amount == STAKE_AMOUNT)
            "Incoming deposit account must not be our account" using (deposit.account.identifier != casinoAccount.state.data.identifier )
        }

        subFlow(MoveTokenFlow(casinoAccount.state.data,stakeAccount.state.data, STAKE_AMOUNT))
        counterPartySession.send(StakeDeposit(casinoAccount.state.data, STAKE_AMOUNT, participants = serviceHub.getAllParticipants()))
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}
