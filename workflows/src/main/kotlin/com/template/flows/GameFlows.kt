package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.EUR
import com.template.contracts.GameContract
import com.template.states.GameCombination
import com.template.states.GameConfigState
import com.template.states.GameState
import com.template.states.GameStep
import com.template.states.StakeDeposit
import com.template.states.UserState
import com.template.utils.CASINO_ACCOUNT
import com.template.utils.CASINO_RESERVE_ACCOUNT
import com.template.utils.checkCasinoStakeDeposit
import com.template.utils.checkRheel
import com.template.utils.generateRandomCombinationFromSeed
import com.template.utils.getAllParticipants
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.internal.sumByLong
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@StartableByRPC
@InitiatingFlow
class IssueGameConfigFlow() : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val gameCombinations: Array<GameCombination> = arrayOf(
                GameCombination( "6", "6", "6", 200),
                GameCombination("4", "4", "4", 50),
                GameCombination( "2", "2", "2", 20),
                GameCombination( "1/3", "5/2", "4/6", 15),
                GameCombination( "5", "5", "5", 13),
                GameCombination( "1", "1", "1", 12),
                GameCombination( "3", "3", "3", 10),
                GameCombination( "1/3/5", "1/3/5", "1/3/5", 4)
        )
        txBuilder.addCommand(GameContract.CONFIG, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(GameConfigState(gameCombinations, 200, listOf(serviceHub.ourIdentity)))
        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        subFlow(FinalityFlow(signedTx, allOtherParticipants.map { initiateFlow(it) }))
    }
}

@InitiatedBy(IssueGameConfigFlow::class)
class IssueGameConfigResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}


@StartableByRPC
@InitiatingFlow
class StartGameFlow(private val user: UserState, private val stake: Long) : FlowLogic<GameState>() {
    @Suspendable
    override fun call(): GameState {
        checkCasinoStakeDeposit(serviceHub)

        val userBalance = serviceHub.vaultService.queryBy(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(user.account!!.state.data.identifier.id))).states
        val totalBalance = userBalance.sumByLong { t -> t.state.data.amount.quantity }
        if (totalBalance < stake){
            throw InsufficientBalanceException(stake.EUR)
        }
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val game = GameState(user = user, stake = stake, participants = serviceHub.getAllParticipants(), result = null, success = null)
        txBuilder.addCommand(GameContract.CREATE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addOutputState(game)
        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        subFlow(FinalityFlow(signedTx, allOtherParticipants.map { initiateFlow(it) }))
        return game
    }
}

@InitiatedBy(StartGameFlow::class)
class StartGameResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        checkCasinoStakeDeposit(serviceHub)
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}

@StartableByRPC
@InitiatingFlow
class ReserveTokensForGameFlow(private val gameId: UniqueIdentifier) : FlowLogic<Boolean>() {
    @Suspendable
    override fun call(): Boolean {
        checkCasinoStakeDeposit(serviceHub)

        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val gameConfig = serviceHub.vaultService.queryBy(GameConfigState::class.java)
        val game = serviceHub.vaultService.queryBy<GameState>(QueryCriteria.LinearStateQueryCriteria(linearId = listOf(gameId))).states.single()
        val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
        val casinoReserveAccount = serviceHub.accountService.accountInfo(CASINO_RESERVE_ACCOUNT).single()

        val stakeDeposits = serviceHub.vaultService.queryBy<StakeDeposit>()
        val ownDeposit = stakeDeposits.states.filter { it.state.data.account.identifier ==  casinoAccount.state.data.identifier}
        if(ownDeposit.isEmpty()){
            throw FlowException("Missing stake deposit for casino $ourIdentity")
        }

        txBuilder.addCommand(GameContract.RESERVE, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addInputState(game)
        txBuilder.addOutputState(game.state.data.copy(step = GameStep.RESERVED))

        val maxReward = game.state.data.stake*gameConfig.states.single().state.data.maxMultiplier
        val casinoNbs = serviceHub.getAllParticipants().size
        val individualCasinoReserve = (maxReward / casinoNbs).toInt()
        val ownReserve = individualCasinoReserve + maxReward % casinoNbs
        subFlow(MoveTokenFlow(game.state.data.user.account!!.state.data, game.state.data.user.reserveAccount!!.state.data, game.state.data.stake))

        val casinoBalance = serviceHub.vaultService.queryBy(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(casinoAccount.state.data.identifier.id))).states
        val totalBalance = casinoBalance.sumByLong { t -> t.state.data.amount.quantity }
        if (totalBalance < ownReserve){
            throw InsufficientBalanceException(ownReserve.EUR)
        }

        subFlow(MoveTokenFlow(casinoAccount.state.data, casinoReserveAccount.state.data, ownReserve))

        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first())
        val sessions = allOtherParticipants.map { initiateFlow(it) }
        sessions.forEach {
            it.send(individualCasinoReserve)
        }
        subFlow(FinalityFlow(signedTx, sessions))
        return true
    }
}

@InitiatedBy(ReserveTokensForGameFlow::class)
class ReserveTokensForGameResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        checkCasinoStakeDeposit(serviceHub)

        val individualCasinoReserve = counterPartySession.receive<Int>().unwrap { it }
        val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
        val casinoReserveAccount = serviceHub.accountService.accountInfo(CASINO_RESERVE_ACCOUNT).single()
        val casinoBalance = serviceHub.vaultService.queryBy(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria()
                .withExternalIds(listOf(casinoAccount.state.data.identifier.id))).states
        val totalBalance = casinoBalance.sumByLong { t -> t.state.data.amount.quantity }
        if (totalBalance < individualCasinoReserve){
            throw InsufficientBalanceException(individualCasinoReserve.EUR)
        }
        if(individualCasinoReserve > 0){
            subFlow(MoveTokenFlow(casinoAccount.state.data, casinoReserveAccount.state.data, individualCasinoReserve.toLong()))
        }
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}

@StartableByRPC
@InitiatingFlow
class GenerateResultForGameFlow(private val gameId: UniqueIdentifier) : FlowLogic<GameState>() {
    @Suspendable
    override fun call(): GameState {
        checkCasinoStakeDeposit(serviceHub)

        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        val gameConfig = serviceHub.vaultService.queryBy(GameConfigState::class.java).states.single().state.data
        val game = serviceHub.vaultService.queryBy<GameState>(QueryCriteria.LinearStateQueryCriteria(linearId = listOf(gameId))).states.single()
        txBuilder.addCommand(GameContract.RESULT, serviceHub.myInfo.legalIdentities.first().owningKey)
        txBuilder.addInputState(game)
        val result = generateRandomCombinationFromSeed(game.state.data.timestamp)
        var successful = false
        var payout = 0L
        gameConfig.gameCombinations.forEach {
                var rheel1Found = checkRheel(it.rheel1, result[0])
                var rheel2Found = checkRheel(it.rheel2, result[1])
                var rheel3Found = checkRheel(it.rheel3, result[2])

                if(rheel1Found && rheel2Found && rheel3Found){
                    successful = true
                    payout = it.payout
                }
        }
        val updatedGame = game.state.data.copy(
                step = GameStep.FINISHED,
                result = result,
                winningAmount = game.state.data.stake*payout,
                success = successful)
        txBuilder.addOutputState(updatedGame)

        val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
        val casinoReserveAccount = serviceHub.accountService.accountInfo(CASINO_RESERVE_ACCOUNT).single()
        val casinoNbs = serviceHub.getAllParticipants().size
        val individualPayout = (updatedGame.winningAmount / casinoNbs).toInt()

        val maxReward = game.state.data.stake*gameConfig.maxMultiplier
        val individualCasinoReserve = (maxReward / casinoNbs).toInt()

        if(successful){
            val ownPayout = individualPayout + (payout % casinoNbs)
            subFlow(MoveTokenFlow(game.state.data.user.reserveAccount!!.state.data, game.state.data.user.account!!.state.data, game.state.data.stake))
            subFlow(MoveTokenFlow(casinoReserveAccount.state.data, game.state.data.user.account!!.state.data, ownPayout))
            val diff = individualCasinoReserve + (maxReward % casinoNbs) - ownPayout
            subFlow(MoveTokenFlow(casinoReserveAccount.state.data, casinoAccount.state.data, diff))
        } else {
            subFlow(MoveTokenFlow(game.state.data.user.reserveAccount!!.state.data, casinoAccount.state.data, game.state.data.stake))
            subFlow(MoveTokenFlow(casinoReserveAccount.state.data, casinoAccount.state.data, individualCasinoReserve + (maxReward % casinoNbs)))
        }

        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val allOtherParticipants = serviceHub.getAllParticipants().minus(serviceHub.myInfo.legalIdentities.first()).map { initiateFlow(it) }
        allOtherParticipants.forEach {
            it.send(updatedGame)
        }
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, allOtherParticipants))
        subFlow(FinalityFlow(fullySignedTx, allOtherParticipants))
        return updatedGame
    }
}

@InitiatedBy(GenerateResultForGameFlow::class)
class GenerateResultForGameResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        checkCasinoStakeDeposit(serviceHub)

        val gameConfig = serviceHub.vaultService.queryBy(GameConfigState::class.java).states.single().state.data
        val game = counterPartySession.receive<GameState>().unwrap { it }
        requireThat {
            val result = generateRandomCombinationFromSeed(game.timestamp)
            "Game result must be identical" using (game.result!!.contentEquals(result))
            var successful = false
            var payout = 0L
            gameConfig.gameCombinations.forEach {
                var rheel1Found = checkRheel(it.rheel1, result[0])
                var rheel2Found = checkRheel(it.rheel2, result[1])
                var rheel3Found = checkRheel(it.rheel3, result[2])

                if(rheel1Found && rheel2Found && rheel3Found){
                    successful = true
                    payout = it.payout
                }
            }
            "Game success must be identical" using (game.success == successful)
            "Game winningAmount must be identical" using (game.winningAmount == game.stake*payout)
            "Game step must be identical" using (game.step == GameStep.FINISHED)
        }

        val casinoAccount = serviceHub.accountService.accountInfo(CASINO_ACCOUNT).single()
        val casinoReserveAccount = serviceHub.accountService.accountInfo(CASINO_RESERVE_ACCOUNT).single()
        val casinoNbs = serviceHub.getAllParticipants().size
        val individualPayout = (game.winningAmount / casinoNbs).toInt()

        val maxReward = game.stake*gameConfig.maxMultiplier
        val individualCasinoReserve = (maxReward / casinoNbs).toInt()

        if(game.success == true){
            if (individualPayout > 0){
                subFlow(MoveTokenFlow(casinoReserveAccount.state.data, game.user.account!!.state.data, individualPayout.toLong()))
            }
            val diff = individualCasinoReserve - individualPayout
            if(diff > 0){
                subFlow(MoveTokenFlow(casinoReserveAccount.state.data, casinoAccount.state.data, diff.toLong()))
            }
        } else {
            subFlow(MoveTokenFlow(casinoReserveAccount.state.data, casinoAccount.state.data, individualCasinoReserve.toLong()))
        }

        return subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}