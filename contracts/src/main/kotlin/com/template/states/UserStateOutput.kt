package com.template.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class UserStateOutput(val identifier: String, val balance: Long)