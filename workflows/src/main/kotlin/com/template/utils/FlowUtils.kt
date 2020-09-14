package com.template.utils

import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub

val CASINO_ACCOUNT = "CASINO_ACCOUNT"
val CASINO_RESERVE_ACCOUNT = "CASINO_ACCOUNT"

fun ServiceHub.getAllParticipants(): List<Party> {
    return this.networkMapCache.allNodes.map { it.legalIdentities.first() }.minus(this.networkMapCache.notaryIdentities.first())
}