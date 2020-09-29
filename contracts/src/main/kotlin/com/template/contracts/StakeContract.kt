package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.StakeDeposit
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class StakeContract : Contract {
    companion object {
        val STAKE: StakeCommands = StakeCommands("STAKE")
    }

    override fun verify(tx: LedgerTransaction) {
        requireThat {
            "A StakeContract transaction must contain at least one Stake Command" using (tx.commandsOfType<StakeCommands>().isNotEmpty())
        }
        tx.commandsOfType<StakeCommands>()
                .forEach() { it ->
                    if (it.value == StakeContract.STAKE) {
                        verifyStake(tx)
                    }

                }
    }

    private fun verifyStake(tx: LedgerTransaction) {
        requireThat {
            "a StakeDeposit with a positive deposit  required for the output of a StakeContract with STAKE command" using (tx.outputStates.all { it is StakeDeposit && it.amount > 0 })
        }
    }

}


data class StakeCommands(val id: String) : CommandData