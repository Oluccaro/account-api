package com.ebanx.accountapi.domain;

import java.util.Arrays;
import java.util.Optional;

public enum EventType {
    DEPOSIT,
    WITHDRAW;

    public static Optional<EventType> parse(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
