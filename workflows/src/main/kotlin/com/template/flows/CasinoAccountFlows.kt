package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.CasinoAccountContract
import com.template.states.CasinoDeposit
import com.template.states.StakeDeposit
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_DEPOSIT_AMOUNT
import com.template.utils.CASINO_RESERVE_ACCOUNT
import com.template.utils.CASINO_STAKE_ACCOUNT
import com.template.utils.STAKE_AMOUNT
import com.template.utils.getAllParticipants
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.unwrap

@StartableByRPC
@InitiatingFlow
class CreateStakeAccountFlow() : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val accountService = serviceHub.accountService
        val stakeAccount = accountService.createAccount(CASINO_STAKE_ACCOUNT).getOrThrow()
        val casinoAccount = accountService.accountInfo(CASINO_ACCOUNT)
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(CasinoAccountContract.STAKE, serviceHub.myInfo.legalIdentities.first().owningKey)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        val deposit = StakeDeposit(casinoAccount.single().state.data, STAKE_AMOUNT, participants = serviceHub.getAllParticipants())
        txBuilder.addOutputState(deposit)
        txBuilder.verify(serviceHub)

        subFlow(MoveTokenFlow(casinoAccount.single().state.data,stakeAccount.state.data, STAKE_AMOUNT))
        subFlow(ShareAccountInfo(stakeAccount,allOtherParticipants))

        txBuilder.verify(serviceHub)
        val initiatedParticipants = allOtherParticipants.map { initiateFlow(it) }
        sendAll(deposit, initiatedParticipants.toSet())
        val stakesDeposits = receiveAll(StakeDeposit::class.java,initiatedParticipants)
        stakesDeposits.forEach {
            val partyDeposit = it.unwrap { data -> data }
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

@StartableByRPC
@InitiatingFlow
class CreateCasinoAccountFlow() : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val accountService = serviceHub.accountService
        val casinoAccount =  accountService.createAccount(CASINO_ACCOUNT).getOrThrow()
        accountService.createAccount(CASINO_RESERVE_ACCOUNT).getOrThrow()
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(CasinoAccountContract.DEPOSIT, serviceHub.myInfo.legalIdentities.first().owningKey)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())

        val party = if (casinoAccount.state.data.host == ourIdentity) {
            serviceHub.createKeyForAccount(casinoAccount.state.data)
        } else {
            subFlow(RequestKeyForAccount(casinoAccount.state.data))
        }
        val tokens = listOf(10000000.00 of EUR issuedBy ourIdentity heldBy party)
        subFlow(IssueTokens(tokens))

        val deposit = CasinoDeposit(CASINO_DEPOSIT_AMOUNT, participants = serviceHub.getAllParticipants())
        txBuilder.addOutputState(deposit)
        txBuilder.verify(serviceHub)


        val initiatedParticipants = allOtherParticipants.map { initiateFlow(it) }
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, initiatedParticipants))
        subFlow(FinalityFlow(fullySignedTx, initiatedParticipants))
    }
}


@InitiatedBy(CreateCasinoAccountFlow::class)
class CreateCasinoAccountResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}
