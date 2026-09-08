package com.ebanx.accountapi.domain;

import java.math.BigDecimal;

public record Account(String id, BigDecimal balance) {

    public static Account open(String id) {
        return new Account(id, BigDecimal.ZERO);
    }

    public Account deposit(BigDecimal amount) {
        return new Account(id, balance.add(amount));
    }

    public Account withdraw(BigDecimal amount) {
        return new Account(id, balance.subtract(amount));
    }

    public boolean hasAtLeast(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
