package com.ebanx.accountapi.service;

import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.BalanceQueryResult;
import org.springframework.stereotype.Service;

/**
 * Every entry point shares this instance's monitor. A multi-step operation such as a transfer
 * therefore cannot interleave with another event or with a balance read, which is what keeps
 * transfers atomic without a database transaction.
 */
@Service
public class AccountService {

    private final AccountStore store;

    public AccountService(AccountStore store) {
        this.store = store;
    }

    public synchronized BalanceQueryResult balanceOf(String accountId) {
        return store.find(accountId)
                .<BalanceQueryResult>map(account -> new BalanceQueryResult.Found(account.balance()))
                .orElseGet(BalanceQueryResult.NotFound::new);
    }
}
