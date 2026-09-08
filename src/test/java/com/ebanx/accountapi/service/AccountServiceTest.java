package com.ebanx.accountapi.service;

import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.INVALID_EVENT;
import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.UNSUPPORTED_EVENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.ebanx.accountapi.domain.Account;
import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.BalanceQueryResult;
import com.ebanx.accountapi.domain.TransactionResult;
import com.ebanx.accountapi.service.handlers.DepositHandler;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private AccountStore store;
    private AccountService service;

    @BeforeEach
    void setUp() {
        store = new AccountStore();
        service = new AccountService(store, new EventHandlerRegistry(List.of(new DepositHandler(store))));
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

    @Test
    void depositCreatesTheAccountWhenItDoesNotExist() {
        TransactionResult result = service.process(deposit("100", "10"));

        assertThat(destinationOf(result).balance()).isEqualByComparingTo("10");
        assertThat(store.find("100")).get().extracting(Account::balance).isEqualTo(new BigDecimal("10"));
    }

    @Test
    void depositCreditsAnExistingAccount() {
        service.process(deposit("100", "10"));

        TransactionResult result = service.process(deposit("100", "10"));

        assertThat(destinationOf(result).balance()).isEqualByComparingTo("20");
        assertThat(store.find("100")).get().extracting(Account::balance).isEqualTo(new BigDecimal("20"));
    }

    @Test
    void rejectsAnUnknownEventType() {
        TransactionResult result = service.process(new EventCommand("bogus", null, "100", BigDecimal.TEN));

        assertThat(failureOf(result).kind()).isEqualTo(UNSUPPORTED_EVENT_TYPE);
        assertThat(store.find("100")).isEmpty();
    }

    @Test
    void rejectsADepositWithoutADestination() {
        TransactionResult result = service.process(new EventCommand("deposit", null, null, BigDecimal.TEN));

        assertThat(failureOf(result).kind()).isEqualTo(INVALID_EVENT);
    }

    private static EventCommand deposit(String destination, String amount) {
        return new EventCommand("deposit", null, destination, new BigDecimal(amount));
    }

    private static Account destinationOf(TransactionResult result) {
        assertThat(result).isInstanceOf(TransactionResult.Success.class);
        return ((TransactionResult.Success) result).destination();
    }

    private static TransactionResult.Failure failureOf(TransactionResult result) {
        assertThat(result).isInstanceOf(TransactionResult.Failure.class);
        return (TransactionResult.Failure) result;
    }
}
