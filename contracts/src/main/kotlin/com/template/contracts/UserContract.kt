package com.template.contracts

import com.template.states.GameConfigState
import com.template.states.GameState
import com.template.states.GameStep
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {
    companion object {
        val CREATE: UserCommands = UserCommands("CREATE")
    }
    override fun verify(tx: LedgerTransaction) {
        require(tx.commandsOfType<GameCommands>().isNotEmpty() || tx.commandsOfType<UserCommands>().isNotEmpty()){
            "A transaction must contain at least one command"
        }
        tx.commandsOfType<GameCommands>() //todo this is not always the case
                .forEach() {it->
                    if (it.value == GameContract.RESERVE){
                        verifyReserve(tx)
                    }

                    if (it.value == GameContract.RESULT){
                        verifyResult(tx)
                    }

                }
//        todo add verification for UserCommands
    }

    private fun verifyReserve(tx: LedgerTransaction) {

        require(tx.inputStates.any { it is GameState }){
            "At least one input states is required for for reserve command"
        }

        require(tx.outputStates.any { it is GameState && it.step == GameStep.RESERVED}){
            "An output step with a 'GameStep.RESERVED' field is required for for reserve command"
        }
    }
    private fun verifyResult(tx: LedgerTransaction) {

        require(tx.inputStates.any { it is GameState }){
            "At least one input states is required for for result command"
        }

        require(tx.outputStates.any { it is GameState && it.step == GameStep.FINISHED}){
            "An output step with a 'GameStep.FINISHED' field is required for for result command"
        }
    }
}

data class UserCommands(val id: String) : CommandData