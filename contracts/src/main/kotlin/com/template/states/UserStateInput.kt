package com.template.states

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class UserStateInput(val name: String, val password: String, val linearId: String?)