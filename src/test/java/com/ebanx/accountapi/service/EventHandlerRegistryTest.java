package com.ebanx.accountapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.domain.EventType;
import com.ebanx.accountapi.service.handlers.DepositHandler;
import com.ebanx.accountapi.service.handlers.TransferHandler;
import com.ebanx.accountapi.service.handlers.WithdrawHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventHandlerRegistryTest {

    private final AccountStore store = new AccountStore();

    @Test
    void resolvesEachEventTypeToItsHandler() {
        DepositHandler deposit = new DepositHandler(store);
        WithdrawHandler withdraw = new WithdrawHandler(store);
        TransferHandler transfer = new TransferHandler(store);

        EventHandlerRegistry registry =
                new EventHandlerRegistry(List.of(deposit, withdraw, transfer));

        assertThat(registry.resolve(EventType.DEPOSIT)).isSameAs(deposit);
        assertThat(registry.resolve(EventType.WITHDRAW)).isSameAs(withdraw);
        assertThat(registry.resolve(EventType.TRANSFER)).isSameAs(transfer);
    }

    @Test
    void failsWhenAnEventTypeHasNoHandler() {
        assertThatThrownBy(() -> new EventHandlerRegistry(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No handler for");
    }

    @Test
    void failsWhenAnEventTypeHasMoreThanOneHandler() {
        assertThatThrownBy(() -> new EventHandlerRegistry(
                        List.of(new DepositHandler(store), new DepositHandler(store))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("More than one handler");
    }
}
