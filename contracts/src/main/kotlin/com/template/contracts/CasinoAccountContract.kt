package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.CasinoDeposit
import com.template.states.StakeDeposit
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
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
        requireThat {
            "A StakeContract transaction must contain at least one Stake Command" using (tx.commandsOfType<CasinoAccountCommands>().isNotEmpty())
        }
        tx.commandsOfType<CasinoAccountCommands>()
                .forEach() { it ->
                    if (it.value == STAKE) {
                        verifyStake(tx)
                    } else if (it.value == DEPOSIT){
                        verifyDeposit(tx)
                    }

                }
    }

    private fun verifyStake(tx: LedgerTransaction) {
        requireThat {
            "a StakeDeposit with a positive deposit  required for the output of a StakeContract with STAKE command" using (tx.outputStates.all { it is StakeDeposit && it.amount > 0 })
        }
    }

    private fun verifyDeposit(tx: LedgerTransaction) {
        requireThat {
            "a CasinoDeposit with a positive deposit  required for the output of a StakeContract with DEPOSIT command" using (tx.outputStates.all { it is CasinoDeposit && it.amount > 0 })
        }
    }

}

data class CasinoAccountCommands(val id: String) : CommandData