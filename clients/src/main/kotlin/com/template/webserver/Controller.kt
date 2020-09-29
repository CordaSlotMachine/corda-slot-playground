package com.template.webserver

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.flows.GenerateResultForGameFlow
import com.template.flows.IssueUserFlow
import com.template.flows.ReserveTokensForGameFlow
import com.template.flows.StartGameFlow
import com.template.inputs.GameInput
import com.template.output.GameOutput
import com.template.states.UserState
import com.template.states.UserStateOutput
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    //User endpoints
    @PostMapping(value = ["/user/create"], produces = ["application/json"], consumes = ["application/json"])
    private fun createUser(): UserStateOutput {
        val userState = proxy.startFlow(::IssueUserFlow).returnValue.getOrThrow().state.data
        return UserStateOutput(userState.linearId.toString(), 10)
    }

    @PostMapping(value = ["/game/spin"], produces = ["application/json"], consumes = ["application/json"])
    private fun spinGame(@RequestBody gameInput: GameInput): GameOutput {
        val userState = proxy.vaultQueryBy<UserState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(gameInput.user))
                )).states.single().state.data
        val gameState = proxy.startFlow(::StartGameFlow, userState, gameInput.amout).returnValue.getOrThrow()
        val res = proxy.startFlow(::ReserveTokensForGameFlow, gameState.linearId).returnValue.getOrThrow()
        val updatedGame = proxy.startFlow(::GenerateResultForGameFlow, gameState.linearId).returnValue.getOrThrow()
        var userBalance = 0L
        val userBalances = proxy.vaultQueryBy<FungibleToken>(QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(userState.account!!.state.data.identifier.id))).states
        userBalances.forEach {
            userBalance += it.state.data.amount.displayTokenSize.multiply(BigDecimal(it.state.data.amount.quantity)).toLong()
        }
        return GameOutput(updatedGame.result!!.toList(), updatedGame.winningAmount, updatedGame.success!!, userBalance)
    }
}