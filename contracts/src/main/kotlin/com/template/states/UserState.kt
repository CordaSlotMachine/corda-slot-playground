package com.template.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(UserContract::class)
@CordaSerializable
data class UserState(val name: String,
                     val password: String,
                     var account: StateAndRef<AccountInfo>?,
                     var reserveAccount: StateAndRef<AccountInfo>?,
                     var userParty: AnonymousParty,
                     override val linearId: UniqueIdentifier = UniqueIdentifier(),
                     override val participants: List<AbstractParty>) : LinearState {}
