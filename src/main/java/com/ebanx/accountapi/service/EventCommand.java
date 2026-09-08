package com.ebanx.accountapi.service;

import java.math.BigDecimal;

public record EventCommand(String type, String origin, String destination, BigDecimal amount) {}
