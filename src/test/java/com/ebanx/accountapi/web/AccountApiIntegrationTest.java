package com.ebanx.accountapi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ebanx.accountapi.domain.Account;
import com.ebanx.accountapi.domain.AccountStore;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountStore store;

    @BeforeEach
    void clearState() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void balanceOfAnUnknownAccountIsNotFoundWithZero() throws Exception {
        mockMvc.perform(get("/balance").param("account_id", "1234"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    void balanceOfAnExistingAccountIsReturned() throws Exception {
        store.save(new Account("100", new BigDecimal("20")));

        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("20"));
    }

    @Test
    void depositCreatesTheAccountWithTheInitialBalance() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}")
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"destination\":{\"id\":\"100\",\"balance\":10}}"))
                .andExpect(jsonPath("$.origin").doesNotExist());
    }

    @Test
    void depositIntoAnExistingAccountAccumulates() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}")
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"destination\":{\"id\":\"100\",\"balance\":20}}"));

        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("20"));
    }

    @Test
    void withdrawFromAnUnknownAccountIsNotFoundWithZero() throws Exception {
        event("{\"type\":\"withdraw\",\"origin\":\"200\",\"amount\":10}")
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    void withdrawFromAnExistingAccountDebitsIt() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":20}");

        event("{\"type\":\"withdraw\",\"origin\":\"100\",\"amount\":5}")
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"origin\":{\"id\":\"100\",\"balance\":15}}"))
                .andExpect(jsonPath("$.destination").doesNotExist());
    }

    @Test
    void withdrawBeyondTheBalanceIsUnprocessableAndChangesNothing() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

        event("{\"type\":\"withdraw\",\"origin\":\"100\",\"amount\":11}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));

        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    void transferMovesFundsAndCreatesTheDestination() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":15}");

        event("{\"type\":\"transfer\",\"origin\":\"100\",\"amount\":15,\"destination\":\"300\"}")
                .andExpect(status().isCreated())
                .andExpect(content().json(
                        "{\"origin\":{\"id\":\"100\",\"balance\":0},"
                                + "\"destination\":{\"id\":\"300\",\"balance\":15}}"));
    }

    @Test
    void transferFromAnUnknownAccountIsNotFoundAndChangesNothing() throws Exception {
        event("{\"type\":\"transfer\",\"origin\":\"200\",\"amount\":15,\"destination\":\"300\"}")
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        mockMvc.perform(get("/balance").param("account_id", "300"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resetClearsAllState() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    void anUnknownEventTypeIsRejected() throws Exception {
        event("{\"type\":\"bogus\",\"destination\":\"100\",\"amount\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_EVENT_TYPE"));
    }

    @Test
    void aNonPositiveAmountIsRejected() throws Exception {
        event("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":-10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aMissingAccountIdIsRejected() throws Exception {
        mockMvc.perform(get("/balance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    private org.springframework.test.web.servlet.ResultActions event(String body) throws Exception {
        return mockMvc.perform(post("/event").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
