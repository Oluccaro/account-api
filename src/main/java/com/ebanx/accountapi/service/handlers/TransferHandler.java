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

/**
 * Both new balances are computed before either is written, so a rejected transfer cannot leave
 * the origin debited or the destination created.
 */
@Component
public class TransferHandler implements EventHandler {

    private final AccountStore store;

    public TransferHandler(AccountStore store) {
        this.store = store;
    }

    @Override
    public EventType type() {
        return EventType.TRANSFER;
    }

    @Override
    public TransactionResult handle(EventCommand command) {
        if (command.origin() == null || command.destination() == null) {
            return TransactionResult.failure(
                    INVALID_EVENT, "origin and destination are required for a transfer");
        }
        if (command.origin().equals(command.destination())) {
            return TransactionResult.failure(
                    INVALID_EVENT, "origin and destination must differ");
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
        Account credited = store.find(command.destination())
                .orElseGet(() -> Account.open(command.destination()))
                .deposit(command.amount());

        store.save(debited);
        store.save(credited);

        return TransactionResult.transferred(debited, credited);
    }
}
