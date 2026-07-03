package com.allochub.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.allochub.integration.DatabaseCleaner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestorIntegrationTest {

    private static final String TOKEN = "Bearer operator-dev-token";
    private static final int AMOUNT_10_EOK = 100000;
    private static final int AMOUNT_15_EOK = 150000;
    private static final int AMOUNT_25_EOK = 250000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void resetDb() {
        databaseCleaner.clean();
    }

    @Test
    void tc001_createInvestor() throws Exception {
        mockMvc.perform(post("/api/investors")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"출자자 A","investmentAmount":%d,"allocationRatio":20}
                                """
                                        .formatted(AMOUNT_10_EOK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("등록 완료"))
                .andExpect(jsonPath("$.data.name").value("출자자 A"));
    }

    @Test
    void tc002_duplicateInvestor() throws Exception {
        String body =
                """
                {"name":"출자자 A","investmentAmount":%d,"allocationRatio":20}
                """
                        .formatted(AMOUNT_10_EOK);
        mockMvc.perform(post("/api/investors")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        mockMvc.perform(post("/api/investors")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE"));
    }

    @Test
    void tc003_allocationRatioExceeded() throws Exception {
        createInvestor("출자자 A", AMOUNT_10_EOK, 30);
        createInvestor("출자자 B", AMOUNT_15_EOK, 40);
        mockMvc.perform(post("/api/investors")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"출자자 C","investmentAmount":%d,"allocationRatio":35}
                                """
                                        .formatted(AMOUNT_25_EOK)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ALLOCATION_RATIO"));
    }

    @Test
    void tc010_unauthorized() throws Exception {
        mockMvc.perform(get("/api/investors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private void createInvestor(String name, int amount, double ratio) throws Exception {
        mockMvc.perform(post("/api/investors")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                        {"name":"%s","investmentAmount":%d,"allocationRatio":%s}
                        """
                                .formatted(name, amount, ratio)));
    }
}
