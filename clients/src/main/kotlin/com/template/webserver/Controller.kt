package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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

    @GetMapping(value = ["/templateendpoint"], produces = ["text/plain"])
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    //User endpoints
    @PostMapping(value = ["/user/create"], produces = ["text/plain"])
    private fun createUser(): String {
        return "Define an endpoint here."
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
    @GetMapping(value = ["/user/{id}"], produces = ["text/plain"])
    private fun getRheelsAndPrizes(): String {
        return "Define an endpoint here."
    }

    @PostMapping(value = ["/game/spin"], produces = ["text/plain"])
    private fun spinGame(): String {
        return "Define an endpoint here."
    }
}