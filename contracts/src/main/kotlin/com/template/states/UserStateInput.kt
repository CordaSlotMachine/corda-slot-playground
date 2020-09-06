package com.template.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class UserStateInput(val user: String, val password: String)