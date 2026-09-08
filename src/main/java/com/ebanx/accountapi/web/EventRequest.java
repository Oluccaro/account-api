package com.ebanx.accountapi.web;

import com.ebanx.accountapi.service.EventCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record EventRequest(
        @NotBlank String type,
        String origin,
        String destination,
        @NotNull @Positive BigDecimal amount) {

    EventCommand toCommand() {
        return new EventCommand(type, origin, destination, amount);
    }
}
