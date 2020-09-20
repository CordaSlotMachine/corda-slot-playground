package com.template.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.contracts.StakeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(StakeContract::class)
@CordaSerializable
class StakeDeposit(val account:AccountInfo,
                   val amount: Long,
                   override val linearId: UniqueIdentifier = UniqueIdentifier(),
                   override val participants: List<AbstractParty>
) : LinearState {
}