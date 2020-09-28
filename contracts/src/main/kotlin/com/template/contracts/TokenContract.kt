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
        require(tx.commandsOfType<TokenCommands>().isNotEmpty()){
            "A TokenContract transaction must contain at least one Token Commands"
        }
//        tx.commandsOfType<TokenCommands>()
//                .forEach() {it->
//                    if (it.value == TokenContract.MOVE){
//                        //todo implement
//                    }
//
//                }
    }

}

data class TokenCommands(val id: String) : CommandData