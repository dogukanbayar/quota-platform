package com.saasplatform.quota.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** End-to-end flow through controller, service, JPA and the H2 (PostgreSQL mode) test database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class QuotaPlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private long createUser(String plan) throws Exception {
        String planJson = plan == null ? "" : ",\"planType\":\"" + plan + "\"";
        String body = "{\"fullName\":\"Test User\",\"email\":\"" + UUID.randomUUID() + "@example.com\"" + planJson + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void consume(long userId, int units) throws Exception {
        mockMvc.perform(post("/api/v1/users/" + userId + "/usage").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"/v1/test\",\"units\":" + units + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void plansAreSeededWithMonthlyLimits() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("FREE"))
                .andExpect(jsonPath("$[0].monthlyLimit").value(100))
                .andExpect(jsonPath("$[1].monthlyLimit").value(10000))
                .andExpect(jsonPath("$[2].type").value("ENTERPRISE"));
    }

    @Test
    void registrationDefaultsToFreePlan() throws Exception {
        long id = createUser(null);
        mockMvc.perform(get("/api/v1/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planType").value("FREE"))
                .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isOk());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String body = "{\"fullName\":\"Dup\",\"email\":\"dup-" + UUID.randomUUID() + "@example.com\"}";
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.fullName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void malformedJsonAndUnknownPlanReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"a@b.com\",\"planType\":\"GOLD\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/users/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void freeQuotaIsEnforcedAndRejectedWithQuotaExceeded() throws Exception {
        long id = createUser("FREE");
        consume(id, 100);
        mockMvc.perform(post("/api/v1/users/" + id + "/usage").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"/v1/test\",\"units\":1}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"));
        mockMvc.perform(get("/api/v1/users/" + id + "/subscription"))
                .andExpect(jsonPath("$.usedThisMonth").value(100))
                .andExpect(jsonPath("$.remaining").value(0));
    }

    @Test
    void usageIsDeductedFromRemainingQuota() throws Exception {
        long id = createUser("FREE");
        consume(id, 30);
        mockMvc.perform(get("/api/v1/users/" + id + "/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedThisMonth").value(30))
                .andExpect(jsonPath("$.remaining").value(70))
                .andExpect(jsonPath("$.monthlyLimit").value(100));
    }

    @Test
    void downgradeIsBlockedWhenUsageExceedsLowerPlanLimit() throws Exception {
        long id = createUser("PRO");
        consume(id, 150);
        mockMvc.perform(put("/api/v1/users/" + id + "/subscription/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planType\":\"FREE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_DOWNGRADE_NOT_ALLOWED"));
        mockMvc.perform(get("/api/v1/users/" + id + "/subscription"))
                .andExpect(jsonPath("$.planType").value("PRO"));
    }

    @Test
    void downgradeSucceedsWhenUsageFitsLowerPlan() throws Exception {
        long id = createUser("PRO");
        consume(id, 50);
        mockMvc.perform(put("/api/v1/users/" + id + "/subscription/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planType\":\"FREE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planType").value("FREE"))
                .andExpect(jsonPath("$.remaining").value(50));
    }

    @Test
    void upgradeRaisesTheLimitAndSamePlanIsRejected() throws Exception {
        long id = createUser("FREE");
        consume(id, 100);
        mockMvc.perform(put("/api/v1/users/" + id + "/subscription/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planType\":\"ENTERPRISE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").value(1000000));
        mockMvc.perform(put("/api/v1/users/" + id + "/subscription/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planType\":\"ENTERPRISE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OPERATION"));
    }

    @Test
    void expiredSubscriptionRejectsUsageUntilRenewed() throws Exception {
        long id = createUser("FREE");
        mockMvc.perform(post("/api/v1/users/" + id + "/subscription/renew")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/" + id + "/subscription/expire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        mockMvc.perform(post("/api/v1/users/" + id + "/usage").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"/v1/test\",\"units\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_EXPIRED"));
        mockMvc.perform(put("/api/v1/users/" + id + "/subscription/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planType\":\"PRO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users/" + id + "/subscription/renew"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        consume(id, 1);
    }

    @Test
    void usageHistoryIsPagedNewestFirst() throws Exception {
        long id = createUser("FREE");
        consume(id, 1);
        consume(id, 2);
        consume(id, 3);
        mockMvc.perform(get("/api/v1/users/" + id + "/usage/logs").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].quotaUsed").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/users/" + id + "/usage/logs").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void unknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/users/999999/usage").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"/v1/test\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/users/999999/subscription")).andExpect(status().isNotFound());
    }

    @Test
    void unknownPathAndWrongMethodUseStandardErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/plans"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void openApiDocumentIsPublished() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Quota Platform API"));
    }

    @Test
    void dailyUsageReturnsUnitsPerDay() throws Exception {
        long id = createUser("FREE");
        consume(id, 4);
        consume(id, 6);
        mockMvc.perform(get("/api/v1/users/" + id + "/usage/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].units").exists());
    }
}
