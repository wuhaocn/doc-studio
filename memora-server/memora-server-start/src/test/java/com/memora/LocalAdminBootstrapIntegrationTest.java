package com.memora;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = MemoraApplication.class,
    properties = {
        "spring.datasource.url=jdbc:h2:file:./var/test-runtime/local-admin-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "logging.level.com.memora=warn",
    }
)
@AutoConfigureMockMvc
class LocalAdminBootstrapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldBootstrapLocalAdminForFileBasedDefaultRuntime() throws Exception {
        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_account", Long.class);
        Long tenantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tenant", Long.class);
        Long tenantMemberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tenant_member", Long.class);

        assertEquals(1L, userCount == null ? -1L : userCount);
        assertEquals(1L, tenantCount == null ? -1L : tenantCount);
        assertEquals(1L, tenantMemberCount == null ? -1L : tenantMemberCount);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andExpect(jsonPath("$.data.tenantName").value("Memora 默认工作区"));
    }

    @Test
    void shouldExposeLocalAdminLoginHintInRuntimeConfig() throws Exception {
        mockMvc.perform(get("/services/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.auth.seedAccountLoginEnabled").value(true));
    }
}
