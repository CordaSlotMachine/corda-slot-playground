package com.template.states

import com.template.contracts.GameContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(GameContract::class)
@CordaSerializable
data class GameConfigState(val gameCombinations: Array<GameCombination>,
                           val maxMultiplier: Int,
                           override val participants: List<AbstractParty>,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()
) :LinearState {}

@CordaSerializable
data class GameCombination(val rheel1:String, val rheel2:String, val rheel3:String, val payout:Long){
}