package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

class MoveTokenFlow(private val origin: AccountInfo, private val destination: AccountInfo, val amount: Long) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val originParty = subFlow(RequestKeyForAccount(origin))
        val destinationParty = subFlow(RequestKeyForAccount(destination))
        val session = initiateFlow(origin.host)
        val signedTx = subFlow(MoveFungibleTokensFlow(partyAndAmount = PartyAndAmount(destinationParty, amount.EUR), participantSessions = listOf(session), changeHolder = originParty))
        return subFlow(FinalityFlow(signedTx))
    }
}