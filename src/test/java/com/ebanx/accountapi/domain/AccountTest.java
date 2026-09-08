package com.ebanx.accountapi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void opensWithAZeroBalance() {
        assertThat(Account.open("100").balance()).isEqualByComparingTo("0");
    }

    @Test
    void depositAddsToTheBalance() {
        Account account = Account.open("100").deposit(new BigDecimal("10"));

        assertThat(account.balance()).isEqualByComparingTo("10");
    }

    @Test
    void withdrawSubtractsFromTheBalance() {
        Account account = new Account("100", new BigDecimal("20")).withdraw(new BigDecimal("5"));

        assertThat(account.balance()).isEqualByComparingTo("15");
    }

    @Test
    void operationsLeaveTheOriginalInstanceUntouched() {
        Account original = new Account("100", new BigDecimal("20"));

        original.deposit(new BigDecimal("10"));
        original.withdraw(new BigDecimal("10"));

        assertThat(original.balance()).isEqualByComparingTo("20");
    }

    @Test
    void hasAtLeastAcceptsTheExactBalance() {
        Account account = new Account("100", new BigDecimal("20"));

        assertThat(account.hasAtLeast(new BigDecimal("20"))).isTrue();
        assertThat(account.hasAtLeast(new BigDecimal("19"))).isTrue();
        assertThat(account.hasAtLeast(new BigDecimal("21"))).isFalse();
    }
}
