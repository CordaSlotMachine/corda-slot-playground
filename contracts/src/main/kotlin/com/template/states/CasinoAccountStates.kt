package com.template.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.contracts.CasinoAccountContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(CasinoAccountContract::class)
@CordaSerializable
data class StakeDeposit(val account:AccountInfo,
                   val amount: Long,
                   override val linearId: UniqueIdentifier = UniqueIdentifier(),
                   override val participants: List<AbstractParty>
) : LinearState {
}

@BelongsToContract(CasinoAccountContract::class)
@CordaSerializable
class CasinoDeposit(val amount: Long,
                   override val linearId: UniqueIdentifier = UniqueIdentifier(),
                   override val participants: List<AbstractParty>
) : LinearState {
}