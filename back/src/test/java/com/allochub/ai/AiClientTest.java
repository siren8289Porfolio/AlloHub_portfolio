package com.allochub.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiClientTest {

    private MockRestServiceServer server;
    private AiClient aiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AiClientProperties properties = new AiClientProperties("http://ai.test", 1000, 3000, true);
        aiClient = new AiClient(restClient, properties);
    }

    @Test
    void queryEvidenceReturnsFastApiPayload() {
        server.expect(requestTo("http://ai.test/ai/evidence/query"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"status":"UNAVAILABLE","available":false,"evidence":[{"source":"OpenDART","status":"UNAVAILABLE","message":"no key","scope":"corp"}]}
                        """,
                        MediaType.APPLICATION_JSON));

        Map<String, Object> response = aiClient.queryEvidence("알로테크", null);

        assertThat(response.get("status")).isEqualTo("UNAVAILABLE");
        assertThat(response.get("available")).isEqualTo(false);
        server.verify();
    }

    @Test
    void queryEvidenceDegradesOnServerError() {
        server.expect(requestTo("http://ai.test/ai/evidence/query"))
                .andRespond(withServerError());

        Map<String, Object> response = aiClient.queryEvidence("알로테크", null);

        assertThat(response.get("status")).isEqualTo("UNAVAILABLE");
        assertThat(response.get("available")).isEqualTo(false);
        server.verify();
    }

    @Test
    void explainReturnsFastApiPayload() {
        server.expect(requestTo("http://ai.test/ai/explain"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"status":"OK","available":true,"explanation":"grounded","grounded":true,"caveats":["no ledger mutation"]}
                        """,
                        MediaType.APPLICATION_JSON));

        Map<String, Object> response =
                aiClient.explain("알로테크", null, Map.of("totals", Map.of("investmentCount", 1)), List.of());

        assertThat(response.get("explanation")).isEqualTo("grounded");
        assertThat(response.get("available")).isEqualTo(true);
        server.verify();
    }

    @Test
    void disabledClientSkipsHttp() {
        AiClientProperties disabled = new AiClientProperties("http://ai.test", 1000, 3000, false);
        AiClient client = new AiClient(RestClient.create(), disabled);

        Map<String, Object> evidence = client.queryEvidence("x", null);
        Map<String, Object> explanation = client.explain("x", null, Map.of(), List.of());

        assertThat(evidence.get("available")).isEqualTo(false);
        assertThat(explanation.get("available")).isEqualTo(false);
    }
}
