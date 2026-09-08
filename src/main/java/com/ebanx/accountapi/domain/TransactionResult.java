package com.ebanx.accountapi.domain;

public sealed interface TransactionResult {

    enum ErrorKind {
        ACCOUNT_NOT_FOUND,
        INSUFFICIENT_FUNDS,
        UNSUPPORTED_EVENT_TYPE,
        INVALID_EVENT
    }

    record Success(Account origin, Account destination) implements TransactionResult {}

    record Failure(ErrorKind kind, String message) implements TransactionResult {}

    static TransactionResult deposited(Account destination) {
        return new Success(null, destination);
    }

    static TransactionResult withdrawn(Account origin) {
        return new Success(origin, null);
    }

    static TransactionResult failure(ErrorKind kind, String message) {
        return new Failure(kind, message);
    }
}
