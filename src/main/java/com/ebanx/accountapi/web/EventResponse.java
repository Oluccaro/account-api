package com.ebanx.accountapi.web;

import com.ebanx.accountapi.domain.TransactionResult;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventResponse(AccountView origin, AccountView destination) {

    static EventResponse from(TransactionResult.Success success) {
        return new EventResponse(
                AccountView.of(success.origin()), AccountView.of(success.destination()));
    }
}
