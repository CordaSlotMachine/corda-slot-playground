package com.template.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.contracts.GameContract
import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@BelongsToContract(UserContract::class)
@CordaSerializable
data class GameState(val user: UserState,
                     val stake: Long,
                     override val linearId: UniqueIdentifier = UniqueIdentifier(),
                     override val participants: List<AbstractParty>) : LinearState {
}