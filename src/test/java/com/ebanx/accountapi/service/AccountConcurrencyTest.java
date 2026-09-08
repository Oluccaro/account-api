package com.ebanx.accountapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.service.handlers.DepositHandler;
import com.ebanx.accountapi.service.handlers.TransferHandler;
import com.ebanx.accountapi.service.handlers.WithdrawHandler;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountConcurrencyTest {

    private static final int THREADS = 100;
    private static final int OPERATIONS_PER_THREAD = 100;

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
    void everyConcurrentDepositLands() throws Exception {
        runConcurrently(() -> service.process(deposit("acc-a", BigDecimal.ONE)));

        assertThat(balanceOf("acc-a"))
                .isEqualByComparingTo(BigDecimal.valueOf((long) THREADS * OPERATIONS_PER_THREAD));
    }

    @Test
    void concurrentTransfersInBothDirectionsConserveTheTotal() throws Exception {
        service.process(deposit("A", new BigDecimal("10000")));
        service.process(deposit("B", new BigDecimal("10000")));

        runConcurrently(() -> {
            service.process(transfer("A", "B"));
            service.process(transfer("B", "A"));
        });

        assertThat(balanceOf("A").add(balanceOf("B"))).isEqualByComparingTo("20000");
    }

    private BigDecimal balanceOf(String id) {
        return store.find(id).orElseThrow().balance();
    }

    private static EventCommand deposit(String destination, BigDecimal amount) {
        return new EventCommand("deposit", null, destination, amount);
    }

    private static EventCommand transfer(String origin, String destination) {
        return new EventCommand("transfer", origin, destination, BigDecimal.ONE);
    }

    private void runConcurrently(Runnable operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);

        for (int thread = 0; thread < THREADS; thread++) {
            pool.execute(() -> {
                try {
                    start.await();
                    for (int operationIndex = 0; operationIndex < OPERATIONS_PER_THREAD; operationIndex++) {
                        operation.run();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        start.countDown();
        assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
    }
}
