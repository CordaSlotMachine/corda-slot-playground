package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class TokenContract : Contract {
    companion object {
        val MOVE: TokenCommands = TokenCommands("MOVE")
    }
    override fun verify(tx: LedgerTransaction) {
        return
    }

}

data class TokenCommands(val id: String) : CommandData