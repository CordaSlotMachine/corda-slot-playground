package com.template.webserver

import com.template.flows.CreateAccountFlow
import com.template.flows.IssueUserWrapperFlow
import com.template.states.UserState
import com.template.states.UserStateInput
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    private fun createUser(@RequestBody user: UserStateInput): UserStateInput {
        val newAccount = proxy.startFlow(::CreateAccountFlow, user).returnValue.getOrThrow()
        proxy.startFlow(::IssueUserWrapperFlow, newAccount, user).returnValue.getOrThrow()
        return user
    }

    @PostMapping(value = ["/user/login"], produces = ["text/plain"])
    private fun loginUser(): String {
        return "Define an endpoint here."
    }

    @GetMapping(value = ["/user/{id}"], produces = ["text/plain"])
    private fun getUser(): String {
        return "Define an endpoint here."
    }

    //Game endpoints
    @GetMapping(value = ["/game"], produces = ["text/plain"])
    private fun getRheelsAndPrizes(): String {
        return "Define an endpoint here."
    }

    @PostMapping(value = ["/game/spin"], produces = ["text/plain"])
    private fun spinGame(): String {
        return "Define an endpoint here."
    }
}