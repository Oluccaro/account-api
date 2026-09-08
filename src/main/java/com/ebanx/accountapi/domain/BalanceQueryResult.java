package com.ebanx.accountapi.domain;

import java.math.BigDecimal;

public sealed interface BalanceQueryResult {

    record Found(BigDecimal balance) implements BalanceQueryResult {}

    record NotFound() implements BalanceQueryResult {}
}
