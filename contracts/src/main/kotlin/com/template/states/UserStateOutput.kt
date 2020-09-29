package com.template.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class UserStateOutput(val name: String, val balance: Long, val linearId: String)