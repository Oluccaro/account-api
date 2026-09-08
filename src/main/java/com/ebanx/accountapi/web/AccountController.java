package com.ebanx.accountapi.web;

import com.ebanx.accountapi.domain.BalanceQueryResult;
import com.ebanx.accountapi.service.AccountService;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> balance(@RequestParam("account_id") String accountId) {
        return switch (accountService.balanceOf(accountId)) {
            case BalanceQueryResult.Found(BigDecimal balance) -> ResponseEntity.ok(balance);
            case BalanceQueryResult.NotFound ignored ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(BigDecimal.ZERO);
        };
    }
}
