package com.template.contracts

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.states.GameCombination
import com.template.states.GameState
import com.template.states.GameStep
import com.template.states.UserState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.transaction
import org.junit.Test


class TokenContractMoveTests {



    @Test
    fun `Move Test`() {
//todo the TokenContract.MOVE command never seems to be used atm, we should either remove it or come up with it's behaviour in a flow

    }






}

