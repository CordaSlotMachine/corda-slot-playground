package com.template

import com.template.flows.CreateCasinoAccountFlow
import com.template.flows.CreateStakeAccountFlow
import com.template.flows.IssueGameConfigFlow
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

//        val userState = proxyA.startFlow(::IssueUserFlow, UserStateInput("test","test", linearId = null)).returnValue.getOrThrow()
//
//        val gameState = proxyA.startFlow(::StartGameFlow, userState.state.data, 10).returnValue.getOrThrow()
//        val res = proxyA.startFlow(::ReserveTokensForGameFlow, gameState.linearId).returnValue.getOrThrow()
//        val updatedGame = proxyA.startFlow(::GenerateResultForGameFlow, gameState.linearId).returnValue.getOrThrow()

        //Close the client connection
        clientConnectionA.close()
        clientConnectionB.close()
        clientConnectionC.close()
    }
}

data class NodeConfig(val address: String, val user: String, val password: String)