package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
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
