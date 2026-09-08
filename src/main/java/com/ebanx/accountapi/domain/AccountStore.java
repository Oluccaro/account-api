package com.ebanx.accountapi.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AccountStore {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public Optional<Account> find(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public void save(Account account) {
        accounts.put(account.id(), account);
    }

    public void clear() {
        accounts.clear();
    }
}
