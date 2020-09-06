package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {
    companion object {
        val CREATE: UserCommands = UserCommands("CREATE")
    }
    override fun verify(tx: LedgerTransaction) {
        return
    }

}

data class UserCommands(val id: String) : CommandData