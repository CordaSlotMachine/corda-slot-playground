package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class GameContract : Contract {
    companion object {
        val CREATE: GameCommands = GameCommands("CREATE")
    }
    override fun verify(tx: LedgerTransaction) {
        return
    }

}

data class GameCommands(val id: String) : CommandData