package com.template.contracts

import com.r3.corda.lib.accounts.contracts.AccountInfoContract
import com.r3.corda.lib.accounts.contracts.commands.AccountCommand
import com.r3.corda.lib.accounts.contracts.commands.Create
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.testing.core.dummyCommand
import net.corda.testing.dsl.LedgerDSL
import net.corda.testing.dsl.TestLedgerDSLInterpreter
import net.corda.testing.dsl.TestTransactionDSLInterpreter


fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.mockStateAndRefAccountInfo(party:Party) = issueAccountInfo(party).outRefsOfType<AccountInfo>().first();



private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.issueAccountInfo(accountParty: Party) =
        unverifiedTransaction  {
            val accountInfoId = UniqueIdentifier()
            attachments(AccountInfoContract::class.java.canonicalName)
            input(AccountInfoContract::class.java.canonicalName, AccountInfo("accountInfo",accountParty, accountInfoId))
            output(AccountInfoContract::class.java.canonicalName,
            AccountInfo("accountInfo",accountParty, accountInfoId))
        }