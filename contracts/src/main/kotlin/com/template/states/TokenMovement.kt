package com.template.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(TokenContract::class)
@CordaSerializable
data class TokenMovement (val origin: AccountInfo,
                          val destination: AccountInfo,
                          val amount: Long,
                          override val linearId: UniqueIdentifier = UniqueIdentifier(),
                          override val participants: List<AbstractParty>) : LinearState {

}