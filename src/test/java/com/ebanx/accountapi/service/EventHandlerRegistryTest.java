package com.ebanx.accountapi.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ebanx.accountapi.domain.AccountStore;
import com.ebanx.accountapi.service.handlers.DepositHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventHandlerRegistryTest {

    private final AccountStore store = new AccountStore();

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
