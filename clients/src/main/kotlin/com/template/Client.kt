package com.template

import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.flows.CreateCasinoAccountFlow
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

        proxyA.startFlow(::CreateCasinoAccountFlow).returnValue.getOrThrow()
        proxyB.startFlow(::CreateCasinoAccountFlow).returnValue.getOrThrow()
        proxyC.startFlow(::CreateCasinoAccountFlow).returnValue.getOrThrow()
        println("Casino Accounts created")

        proxyA.startFlow(::IssueGameConfigFlow).returnValue.getOrThrow()
        proxyB.startFlow(::IssueGameConfigFlow).returnValue.getOrThrow()
        proxyC.startFlow(::IssueGameConfigFlow).returnValue.getOrThrow()
        println("Game config created")

        proxyA.startFlow(::CreateStakeAccountFlow).returnValue.getOrThrow()

        println("Stake Accounts created")

        //Close the client connection
        clientConnectionA.close()
        clientConnectionB.close()
        clientConnectionC.close()
    }
}

data class NodeConfig(val address: String, val user: String, val password: String)