package com.ebanx.accountapi.web;

import com.ebanx.accountapi.domain.Account;
import java.math.BigDecimal;

public record AccountView(String id, BigDecimal balance) {

    static AccountView of(Account account) {
        return account == null ? null : new AccountView(account.id(), account.balance());
    }
}
