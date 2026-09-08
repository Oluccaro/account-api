package com.ebanx.accountapi.service.handlers;

import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.ACCOUNT_NOT_FOUND;
import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.INSUFFICIENT_FUNDS;
import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.INVALID_EVENT;

import com.ebanx.accountapi.domain.Account;
import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.EventType;
import com.ebanx.accountapi.domain.TransactionResult;
import com.ebanx.accountapi.service.EventCommand;
import org.springframework.stereotype.Component;

@Component
public class WithdrawHandler implements EventHandler {

    private final AccountStore store;

    public WithdrawHandler(AccountStore store) {
        this.store = store;
    }

    @Override
    public EventType type() {
        return EventType.WITHDRAW;
    }

    @Override
    public TransactionResult handle(EventCommand command) {
        if (command.origin() == null) {
            return TransactionResult.failure(INVALID_EVENT, "origin is required for a withdrawal");
        }

        Account origin = store.find(command.origin()).orElse(null);
        if (origin == null) {
            return TransactionResult.failure(
                    ACCOUNT_NOT_FOUND, "no such account: " + command.origin());
        }
        if (!origin.hasAtLeast(command.amount())) {
            return TransactionResult.failure(
                    INSUFFICIENT_FUNDS, "balance is lower than " + command.amount());
        }

        Account debited = origin.withdraw(command.amount());

        store.save(debited);

        return TransactionResult.withdrawn(debited);
    }
}
