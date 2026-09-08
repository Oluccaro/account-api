package com.ebanx.accountapi.service.handlers;

import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.INVALID_EVENT;

import com.ebanx.accountapi.domain.Account;
import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.EventType;
import com.ebanx.accountapi.domain.TransactionResult;
import com.ebanx.accountapi.service.EventCommand;
import org.springframework.stereotype.Component;

@Component
public class DepositHandler implements EventHandler {

    private final AccountStore store;

    public DepositHandler(AccountStore store) {
        this.store = store;
    }

    @Override
    public EventType type() {
        return EventType.DEPOSIT;
    }

    @Override
    public TransactionResult handle(EventCommand command) {
        if (command.destination() == null) {
            return TransactionResult.failure(INVALID_EVENT, "destination is required for a deposit");
        }

        Account credited = store.find(command.destination())
                .orElseGet(() -> Account.open(command.destination()))
                .deposit(command.amount());

        store.save(credited);

        return TransactionResult.deposited(credited);
    }
}
