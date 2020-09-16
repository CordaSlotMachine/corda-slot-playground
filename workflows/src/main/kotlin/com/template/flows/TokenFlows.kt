package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.template.contracts.TokenContract
import com.template.states.TokenMovement
import com.template.utils.getAllParticipants
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class MoveTokenFlow(private val origin: AccountInfo, private val destination: AccountInfo, val amount: Long) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val originParty = subFlow(RequestKeyForAccount(origin))
        val destinationParty = subFlow(RequestKeyForAccount(destination))
        subFlow(MoveFungibleTokens(partyAndAmount = PartyAndAmount(destinationParty, amount.EUR), changeHolder = originParty, queryCriteria = QueryCriteria.VaultQueryCriteria().withExternalIds(Collections.singletonList(origin.identifier.id))))
    }
}

@InitiatedBy(MoveTokenFlow::class)
class MoveTokenResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(MoveFungibleTokensHandler(counterPartySession))
    }
}
