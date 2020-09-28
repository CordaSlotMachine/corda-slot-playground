package com.template.contracts

import com.template.states.GameConfigState
import com.template.states.GameState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class GameContract : Contract {
    companion object {
         val CREATE: GameCommands = GameCommands("CREATE") //todo perhaps this should be on the UserContract since we add a GameState as output when it is used
        val CONFIG: GameCommands = GameCommands("CONFIG")
        val RESERVE: GameCommands = GameCommands("RESERVE")
        val RESULT: GameCommands = GameCommands("RESULT")
    }
    override fun verify(tx: LedgerTransaction) {
        tx.commandsOfType<GameCommands>()
                .also { require(it.isNotEmpty()) { "A transaction must contain at least one command" } }
                .forEach() {it->
                    if (it.value == GameContract.CONFIG){
                        verifyConfig(tx)
                }
                }
    }



    private fun verifyConfig(tx: LedgerTransaction) {
        require(tx.inputStates.isEmpty()){
            "No input states are allowed for for create command"
        }

        require(tx.outputStates.count()==1){
            "There must be only one output state for create command"
        }
        require(tx.outputStates.any { it is GameConfigState }){
            "The output state for create command must be a GameConfigState"
        }

        require(tx.outputStates.any { it is GameConfigState }){
            "The output state for create command must be a GameConfigState"
        }

        val gameConfig:GameConfigState = tx.outputStates.first { it is GameConfigState } as GameConfigState
       require(gameConfig.gameCombinations.isNotEmpty()){
            "The GameConfigState needs gameCombinations"
        }

//todo verify that there is at least a participant that is a casino

    }

}

data class GameCommands(val id: String) : CommandData