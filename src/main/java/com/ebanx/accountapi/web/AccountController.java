package com.ebanx.accountapi.web;

import com.ebanx.accountapi.domain.BalanceQueryResult;
import com.ebanx.accountapi.domain.TransactionResult;
import com.ebanx.accountapi.service.AccountService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/event")
    public ResponseEntity<Object> event(@Valid @RequestBody EventRequest request) {
        return switch (accountService.process(request.toCommand())) {
            case TransactionResult.Success success ->
                    ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(success));
            case TransactionResult.Failure failure -> errorFor(failure);
        };
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> balance(@RequestParam("account_id") String accountId) {
        return switch (accountService.balanceOf(accountId)) {
            case BalanceQueryResult.Found(BigDecimal balance) -> ResponseEntity.ok(balance);
            case BalanceQueryResult.NotFound ignored ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(BigDecimal.ZERO);
        };
    }

    private ResponseEntity<Object> errorFor(TransactionResult.Failure failure) {
        return switch (failure.kind()) {
            case UNSUPPORTED_EVENT_TYPE, INVALID_EVENT -> ResponseEntity.badRequest()
                    .body(new ErrorResponse(failure.kind().name(), failure.message()));
        };
    }
}
