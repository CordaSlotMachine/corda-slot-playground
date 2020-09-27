package com.template

import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.flows.CreateStakeAccountFlow
import com.template.flows.IssueGameConfigFlow
import com.template.utils.CASINO_ACCOUNT
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

private class Client {
    companion object {
        val logger = loggerFor<Client>()
    }

    fun main(args: Array<String>) {

        val casinoA = NodeConfig("localhost:10006", "user1","test")
        val casinoB = NodeConfig("localhost:10009", "user1","test")
        val casinoC = NodeConfig("localhost:10011", "user1","test")

        val clientA = CordaRPCClient(parse(casinoA.address))
        val clientConnectionA = clientA.start(casinoA.user, casinoA.password)
        val proxyA = clientConnectionA.proxy

        val clientB = CordaRPCClient(parse(casinoB.address))
        val clientConnectionB = clientB.start(casinoB.user, casinoB.password)
        val proxyB = clientConnectionB.proxy

        val clientC = CordaRPCClient(parse(casinoC.address))
        val clientConnectionC = clientC.start(casinoC.user, casinoC.password)
        val proxyC = clientConnectionC.proxy

        val casinoAccountA = proxyA.startFlow(::CreateAccount, CASINO_ACCOUNT).returnValue.getOrThrow()
        val casinoAccountB = proxyB.startFlow(::CreateAccount, CASINO_ACCOUNT).returnValue.getOrThrow()
        val casinoAccountC = proxyC.startFlow(::CreateAccount, CASINO_ACCOUNT).returnValue.getOrThrow()

        val casinoAccountPartyA = proxyA.startFlow(::RequestKeyForAccount, casinoAccountA.state.data).returnValue.getOrThrow()
        val casinoAccountPartyB = proxyB.startFlow(::RequestKeyForAccount, casinoAccountB.state.data).returnValue.getOrThrow()
        val casinoAccountPartyC = proxyC.startFlow(::RequestKeyForAccount, casinoAccountC.state.data).returnValue.getOrThrow()

        val tokensA = listOf(10000000.00 of EUR issuedBy proxyA.nodeInfo().legalIdentities.first() heldBy casinoAccountPartyA)
        val tokensB = listOf(10000000.00 of EUR issuedBy proxyB.nodeInfo().legalIdentities.first() heldBy casinoAccountPartyB)
        val tokensC = listOf(10000000.00 of EUR issuedBy proxyC.nodeInfo().legalIdentities.first() heldBy casinoAccountPartyC)

        proxyA.startFlow { IssueTokens(tokensA) }.returnValue.getOrThrow()
        proxyB.startFlow { IssueTokens(tokensB) }.returnValue.getOrThrow()
        proxyC.startFlow { IssueTokens(tokensC) }.returnValue.getOrThrow()

        proxyA.startFlow(::CreateStakeAccountFlow).returnValue.getOrThrow()

        proxyA.startFlow(::IssueGameConfigFlow).returnValue.getOrThrow()
        proxyB.startFlow(::IssueGameConfigFlow).returnValue.getOrThrow()
        proxyC.startFlow(::IssueGameConfigFlow).returnValue.getOrThrow()

        //Close the client connection
        clientConnectionA.close()
        clientConnectionB.close()
        clientConnectionC.close()
    }
}

data class NodeConfig(val address: String, val user: String, val password: String)