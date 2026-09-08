package com.ebanx.accountapi.service.handlers;

import com.ebanx.accountapi.domain.EventType;
import com.ebanx.accountapi.domain.TransactionResult;
import com.ebanx.accountapi.service.EventCommand;

public interface EventHandler {

    EventType type();

    TransactionResult handle(EventCommand command);
}
