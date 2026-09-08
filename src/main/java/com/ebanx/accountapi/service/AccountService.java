package com.ebanx.accountapi.service;

import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.UNSUPPORTED_EVENT_TYPE;

import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.BalanceQueryResult;
import com.ebanx.accountapi.domain.EventType;
import com.ebanx.accountapi.domain.TransactionResult;
import org.springframework.stereotype.Service;

/**
 * Every entry point shares this instance's monitor. A multi-step operation such as a transfer
 * therefore cannot interleave with another event or with a balance read, which is what keeps
 * transfers atomic without a database transaction. Handlers are written as if single-threaded.
 */
@Service
public class AccountService {

    private final AccountStore store;
    private final EventHandlerRegistry registry;

    public AccountService(AccountStore store, EventHandlerRegistry registry) {
        this.store = store;
        this.registry = registry;
    }

    public synchronized TransactionResult process(EventCommand command) {
        return EventType.parse(command.type())
                .map(type -> registry.resolve(type).handle(command))
                .orElseGet(() -> TransactionResult.failure(
                        UNSUPPORTED_EVENT_TYPE, "unknown event type: " + command.type()));
    }

    public synchronized BalanceQueryResult balanceOf(String accountId) {
        return store.find(accountId)
                .<BalanceQueryResult>map(account -> new BalanceQueryResult.Found(account.balance()))
                .orElseGet(BalanceQueryResult.NotFound::new);
    }
}
