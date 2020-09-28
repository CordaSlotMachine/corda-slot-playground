package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class CasinoAccountContract : Contract {
    companion object {
        val STAKE: CasinoAccountCommands = CasinoAccountCommands("STAKE")
        val DEPOSIT: CasinoAccountCommands = CasinoAccountCommands("DEPOSIT")
    }
    override fun verify(tx: LedgerTransaction) {
        return
    }

}

data class CasinoAccountCommands(val id: String) : CommandData