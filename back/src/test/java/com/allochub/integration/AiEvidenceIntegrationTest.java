package com.allochub.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiEvidenceIntegrationTest {

    private static final String TOKEN = "Bearer operator-dev-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void resetDb() {
        databaseCleaner.clean();
    }

    @Test
    void explainInvestmentEvidenceReturnsGroundedInternalFacts() throws Exception {
        createInvestor("출자자 A", 100000, 60);
        createInvestor("출자자 B", 100000, 40);

        mockMvc.perform(post("/api/investments")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"companyName":"알로테크","investmentAmount":50000}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/ai/investment-evidence")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"companyName":"알로테크"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status")
                        .value("DESIGNED / PROPOSED / PARTIALLY IMPLEMENTED / NOT TESTED IN PRODUCTION"))
                .andExpect(jsonPath("$.data.internalLedgerFacts.totals.investmentCount").value(1))
                .andExpect(jsonPath("$.data.internalLedgerFacts.totals.investmentAmount").value(50000))
                .andExpect(jsonPath("$.data.internalLedgerFacts.investments[0].companyName").value("알로테크"))
                .andExpect(jsonPath("$.data.aiService.evidenceAvailable").value(false))
                .andExpect(jsonPath("$.data.aiService.explanationAvailable").value(false))
                .andExpect(jsonPath("$.data.externalEvidence[0].status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.humanDecisionBoundary[0]").value("투자 여부 추천 또는 자동 의사결정을 하지 않습니다."));
    }

    @Test
    void explainInvestmentEvidenceRequiresQuery() throws Exception {
        mockMvc.perform(post("/api/ai/investment-evidence")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void explainInvestmentEvidenceRequiresAuthorization() throws Exception {
        mockMvc.perform(post("/api/ai/investment-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"companyName":"알로테크"}
                                """))
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
