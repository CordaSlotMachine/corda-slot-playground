package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class StakeContract : Contract {
    companion object {
        val STAKE: StakeCommands = StakeCommands("STAKE")
    }
    override fun verify(tx: LedgerTransaction) {
        return
    }

}

data class StakeCommands(val id: String) : CommandData