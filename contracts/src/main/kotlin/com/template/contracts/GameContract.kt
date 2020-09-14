package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class GameContract : Contract {
    companion object {
        val CREATE: GameCommands = GameCommands("CREATE")
        val CONFIG: GameCommands = GameCommands("CONFIG")
        val RESERVE: GameCommands = GameCommands("RESERVE")
        val RESULT: GameCommands = GameCommands("RESULT")
    }
    override fun verify(tx: LedgerTransaction) {
        return
    }

}

data class GameCommands(val id: String) : CommandData