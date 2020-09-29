package com.template.utils

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.template.states.StakeDeposit
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import java.util.*

val CASINO_ACCOUNT = "CASINO_ACCOUNT"
val CASINO_RESERVE_ACCOUNT = "CASINO_RESERVE_ACCOUNT"
val CASINO_STAKE_ACCOUNT = "CASINO_STAKE_ACCOUNT"
val STAKE_AMOUNT = 1000000L
val CASINO_DEPOSIT_AMOUNT = 10000000L

fun ServiceHub.getAllParticipants(): List<Party> {
    return this.networkMapCache.allNodes.map { it.legalIdentities.first() }.minus(this.networkMapCache.notaryIdentities.first())
}

fun generateRandomCombinationFromSeed(seed:Long): Array<Int>{
    return getRandomList(Random(seed)).toTypedArray()
}

fun main(args: Array<String>){
    println("Hello kotlin")
    println("Hello, world!!!")
    val unixTime = System.currentTimeMillis() / 1000L;
    val randomValues1 = getRandomList(Random(unixTime))
    // prints the same sequence every time
    println(randomValues1) // [33, 40, 41, 2, 41, 32, 21, 40, 69, 87]

    val randomValues2 = getRandomList(Random(unixTime))
    // random with the same seed produce the same sequence
    println(randomValues2)
    println("randomValues1 == randomValues2 is ${randomValues1 == randomValues2}") // true

    val randomValues3 = getRandomList(Random(0))
    // random with another seed produce another sequence
    println(randomValues3) // [14, 48, 57, 67, 82, 7, 61, 27, 14, 59]
}

fun getRandomList(random: Random): List<Int> =
        List(3) { random.nextInt(6)+1 }

fun checkRheel(rheel: String, result: Int): Boolean {
    var found = false
    if(rheel.contains('/')){
        rheel.split('/').forEach { rh ->
            if(result == Integer.parseInt(rh)){
                found = true
            }
        }
    } else {
        if(result == Integer.parseInt(rheel)){
            found = true
        }
    }
    return found
}

fun checkCasinoStakeDeposit(serviceHub: ServiceHub){
    val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
    val stakeDeposits = serviceHub.vaultService.queryBy<StakeDeposit>()
    val ownDeposit = stakeDeposits.states.filter { it.state.data.account.identifier ==  casinoAccount.state.data.identifier}
    if(ownDeposit.isEmpty()){
        throw FlowException("Missing stake deposit for casino ${serviceHub.myInfo.legalIdentities.first()}")
    }
}
