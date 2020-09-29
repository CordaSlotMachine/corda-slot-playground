package com.template.contracts

import com.template.states.GameConfigState
import com.template.states.GameState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class GameContract : Contract {
    companion object {
        val CREATE: GameCommands = GameCommands("CREATE")
        val CONFIG: GameCommands = GameCommands("CONFIG")
        val RESERVE: GameCommands = GameCommands("RESERVE")
        val RESULT: GameCommands = GameCommands("RESULT")
    }

    override fun verify(tx: LedgerTransaction) {
        tx.commandsOfType<GameCommands>()
                .also {
                    require(it.isNotEmpty())
                    { "A transaction must contain at least one command" }
                }
                .forEach() { it ->
                    if (it.value == GameContract.CONFIG) {
                        verifyConfig(tx)
                    }
                }
    }


    private fun verifyConfig(tx: LedgerTransaction) {

        requireThat {
            "No input states are allowed for for create command" using (tx.inputStates.isEmpty())
            "There must be only one output state for create command" using (tx.outputStates.count() == 1)
            "The output state for create command must be a GameConfigState" using (tx.outputStates.any { it is GameConfigState })
            "The GameConfigState needs gameCombinations" using ((tx.outputStates.first { it is GameConfigState } as GameConfigState).gameCombinations.isNotEmpty())
        }
    }
}

data class GameCommands(val id: String) : CommandData