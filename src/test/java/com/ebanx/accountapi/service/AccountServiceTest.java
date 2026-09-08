package com.ebanx.accountapi.service;

import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.ACCOUNT_NOT_FOUND;
import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.INSUFFICIENT_FUNDS;
import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.INVALID_EVENT;
import static com.ebanx.accountapi.domain.TransactionResult.ErrorKind.UNSUPPORTED_EVENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.ebanx.accountapi.domain.Account;
import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.BalanceQueryResult;
import com.ebanx.accountapi.domain.TransactionResult;
import com.ebanx.accountapi.service.handlers.DepositHandler;
import com.ebanx.accountapi.service.handlers.TransferHandler;
import com.ebanx.accountapi.service.handlers.WithdrawHandler;
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
        service = new AccountService(
                store,
                new EventHandlerRegistry(
                        List.of(
                                new DepositHandler(store),
                                new WithdrawHandler(store),
                                new TransferHandler(store))));
    }

    @Test
    void reportsTheBalanceOfAnExistingAccount() {
        store.save(new Account("acc-a", new BigDecimal("20")));

        BalanceQueryResult result = service.balanceOf("acc-a");

        assertThat(result).isInstanceOf(BalanceQueryResult.Found.class);
        assertThat(((BalanceQueryResult.Found) result).balance()).isEqualByComparingTo("20");
    }

    @Test
    void reportsNotFoundForAnUnknownAccount() {
        assertThat(service.balanceOf("acc-unknown")).isInstanceOf(BalanceQueryResult.NotFound.class);
    }

    @Test
    void depositCreatesTheAccountWhenItDoesNotExist() {
        TransactionResult result = service.process(deposit("acc-a", "10"));

        assertThat(destinationOf(result).balance()).isEqualByComparingTo("10");
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("10");
    }

    @Test
    void depositCreditsAnExistingAccount() {
        service.process(deposit("acc-a", "10"));

        TransactionResult result = service.process(deposit("acc-a", "10"));

        assertThat(destinationOf(result).balance()).isEqualByComparingTo("20");
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("20");
    }

    @Test
    void withdrawDebitsAnExistingAccount() {
        service.process(deposit("acc-a", "20"));

        TransactionResult result = service.process(withdraw("acc-a", "5"));

        assertThat(originOf(result).balance()).isEqualByComparingTo("15");
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("15");
    }

    @Test
    void rejectsAWithdrawalFromAnUnknownAccount() {
        TransactionResult result = service.process(withdraw("acc-missing", "10"));

        assertThat(failureOf(result).kind()).isEqualTo(ACCOUNT_NOT_FOUND);
        assertThat(store.find("acc-missing")).isEmpty();
    }

    @Test
    void rejectsAWithdrawalBeyondTheBalanceAndLeavesItUnchanged() {
        service.process(deposit("acc-a", "10"));

        TransactionResult result = service.process(withdraw("acc-a", "11"));

        assertThat(failureOf(result).kind()).isEqualTo(INSUFFICIENT_FUNDS);
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("10");
    }

    @Test
    void allowsAWithdrawalOfTheEntireBalance() {
        service.process(deposit("acc-a", "10"));

        TransactionResult result = service.process(withdraw("acc-a", "10"));

        assertThat(originOf(result).balance()).isEqualByComparingTo("0");
    }

    @Test
    void transferMovesFundsAndCreatesTheDestination() {
        service.process(deposit("acc-a", "15"));

        TransactionResult result = service.process(transfer("acc-a", "acc-b", "15"));

        assertThat(originOf(result).balance()).isEqualByComparingTo("0");
        assertThat(destinationOf(result).balance()).isEqualByComparingTo("15");
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("0");
        assertThat(balanceOf("acc-b")).isEqualByComparingTo("15");
    }

    @Test
    void rejectsATransferFromAnUnknownAccountWithoutCreatingTheDestination() {
        TransactionResult result = service.process(transfer("acc-missing", "acc-b", "15"));

        assertThat(failureOf(result).kind()).isEqualTo(ACCOUNT_NOT_FOUND);
        assertThat(store.find("acc-missing")).isEmpty();
        assertThat(store.find("acc-b")).isEmpty();
    }

    @Test
    void rejectsATransferBeyondTheBalanceAndLeavesBothSidesUnchanged() {
        service.process(deposit("acc-a", "10"));
        service.process(deposit("acc-b", "5"));

        TransactionResult result = service.process(transfer("acc-a", "acc-b", "11"));

        assertThat(failureOf(result).kind()).isEqualTo(INSUFFICIENT_FUNDS);
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("10");
        assertThat(balanceOf("acc-b")).isEqualByComparingTo("5");
    }

    @Test
    void rejectsATransferToTheSameAccount() {
        service.process(deposit("acc-a", "10"));

        TransactionResult result = service.process(transfer("acc-a", "acc-a", "10"));

        assertThat(failureOf(result).kind()).isEqualTo(INVALID_EVENT);
        assertThat(balanceOf("acc-a")).isEqualByComparingTo("10");
    }

    @Test
    void rejectsATransferWithoutADestination() {
        TransactionResult result =
                service.process(new EventCommand("transfer", "acc-a", null, BigDecimal.TEN));

        assertThat(failureOf(result).kind()).isEqualTo(INVALID_EVENT);
    }

    @Test
    void rejectsAnUnknownEventType() {
        TransactionResult result =
                service.process(new EventCommand("bogus", null, "acc-a", BigDecimal.TEN));

        assertThat(failureOf(result).kind()).isEqualTo(UNSUPPORTED_EVENT_TYPE);
        assertThat(store.find("acc-a")).isEmpty();
    }

    @Test
    void rejectsADepositWithoutADestination() {
        TransactionResult result =
                service.process(new EventCommand("deposit", null, null, BigDecimal.TEN));

        assertThat(failureOf(result).kind()).isEqualTo(INVALID_EVENT);
    }

    @Test
    void rejectsAWithdrawalWithoutAnOrigin() {
        TransactionResult result =
                service.process(new EventCommand("withdraw", null, null, BigDecimal.TEN));

        assertThat(failureOf(result).kind()).isEqualTo(INVALID_EVENT);
    }

    @Test
    void resetClearsEveryAccount() {
        service.process(deposit("acc-a", "10"));
        service.process(deposit("acc-b", "5"));

        service.reset();

        assertThat(store.find("acc-a")).isEmpty();
        assertThat(store.find("acc-b")).isEmpty();
    }

    private BigDecimal balanceOf(String id) {
        return store.find(id).orElseThrow().balance();
    }

    private static EventCommand deposit(String destination, String amount) {
        return new EventCommand("deposit", null, destination, new BigDecimal(amount));
    }

    private static EventCommand withdraw(String origin, String amount) {
        return new EventCommand("withdraw", origin, null, new BigDecimal(amount));
    }

    private static EventCommand transfer(String origin, String destination, String amount) {
        return new EventCommand("transfer", origin, destination, new BigDecimal(amount));
    }

    private static Account originOf(TransactionResult result) {
        assertThat(result).isInstanceOf(TransactionResult.Success.class);
        return ((TransactionResult.Success) result).origin();
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
