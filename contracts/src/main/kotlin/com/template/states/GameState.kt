package com.template.states


import com.template.contracts.GameContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(GameContract::class)
@CordaSerializable
data class GameState(val user: UserState,
                     val stake: Long,
                     val success: Boolean?,
                     val result: Array<Int>?,
                     val step: GameStep = GameStep.CREATED,
                     val timestamp: Long = System.currentTimeMillis(),
                     val winningAmount: Long = 0,
                     override val linearId: UniqueIdentifier = UniqueIdentifier(),
                     override val participants: List<AbstractParty>) : LinearState {
}

@CordaSerializable
enum class GameStep {
    CREATED, RESERVED, FINISHED
}