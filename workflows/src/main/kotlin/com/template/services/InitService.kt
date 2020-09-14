package com.template.services

import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService

@CordaService
class InitService(serviceHub: AppServiceHub){
    companion object{
        val CASINO_ACCOUNT = "CASINO_ACCOUNT"
        val CASINO_RESERVE_ACCOUNT = "CASINO_ACCOUNT"

    }
    init {
        serviceHub.accountService.createAccount(CASINO_ACCOUNT)
        serviceHub.accountService.createAccount(CASINO_RESERVE_ACCOUNT)
        serviceHub.ourIdentity
        val tokens = listOf(999999999999 of EUR issuedBy serviceHub.ourIdentity heldBy serviceHub.ourIdentity.toParty(serviceHub))
        serviceHub.startFlow(IssueTokens(tokens))
    }
}