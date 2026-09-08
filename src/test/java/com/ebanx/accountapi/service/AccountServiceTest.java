package com.ebanx.accountapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebanx.accountapi.domain.Account;
import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.BalanceQueryResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private AccountStore store;
    private AccountService service;

    @BeforeEach
    void setUp() {
        store = new AccountStore();
        service = new AccountService(store);
    }

    @Test
    void reportsTheBalanceOfAnExistingAccount() {
        store.save(new Account("100", new BigDecimal("20")));

        BalanceQueryResult result = service.balanceOf("100");

        assertThat(result).isInstanceOf(BalanceQueryResult.Found.class);
        assertThat(((BalanceQueryResult.Found) result).balance()).isEqualByComparingTo("20");
    }

    @Test
    void reportsNotFoundForAnUnknownAccount() {
        assertThat(service.balanceOf("1234")).isInstanceOf(BalanceQueryResult.NotFound.class);
    }
}
