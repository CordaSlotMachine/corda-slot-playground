package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.GameState
import com.template.states.GameStep
import com.template.states.StakeDeposit
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
        require(tx.commandsOfType<StakeCommands>().isNotEmpty()){
            "A StakeContract transaction must contain at least one Stake Command"
        }
        tx.commandsOfType<StakeCommands>()
                .forEach() {it->
                    if (it.value == StakeContract.STAKE){
                        verifyStake(tx)
                    }

                }
    }

    private fun verifyStake(tx: LedgerTransaction) {
        require(tx.outputStates.all { it is StakeDeposit && it.amount > 0}){
            "a StakeDeposit with a positive deposit  required for the output of a StakeContract with STAKE command"
        }

    }

}



data class StakeCommands(val id: String) : CommandData