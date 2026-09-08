package com.ebanx.accountapi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The scenarios from the challenge statement, in order, asserting the exact response bodies.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpecScenarioTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void replaysTheDocumentedScenariosInOrder() throws Exception {
        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/balance").param("account_id", "1234"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        event("{\"type\":\"deposit\", \"destination\":\"100\", \"amount\":10}")
                .andExpect(status().isCreated())
                .andExpect(content().string("{\"destination\":{\"id\":\"100\",\"balance\":10}}"));

        event("{\"type\":\"deposit\", \"destination\":\"100\", \"amount\":10}")
                .andExpect(status().isCreated())
                .andExpect(content().string("{\"destination\":{\"id\":\"100\",\"balance\":20}}"));

        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("20"));

        event("{\"type\":\"withdraw\", \"origin\":\"200\", \"amount\":10}")
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        event("{\"type\":\"withdraw\", \"origin\":\"100\", \"amount\":5}")
                .andExpect(status().isCreated())
                .andExpect(content().string("{\"origin\":{\"id\":\"100\",\"balance\":15}}"));

        event("{\"type\":\"transfer\", \"origin\":\"100\", \"amount\":15, \"destination\":\"300\"}")
                .andExpect(status().isCreated())
                .andExpect(content().string(
                        "{\"origin\":{\"id\":\"100\",\"balance\":0},"
                                + "\"destination\":{\"id\":\"300\",\"balance\":15}}"));

        event("{\"type\":\"transfer\", \"origin\":\"200\", \"amount\":15, \"destination\":\"300\"}")
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    private org.springframework.test.web.servlet.ResultActions event(String body) throws Exception {
        return mockMvc.perform(post("/event").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
