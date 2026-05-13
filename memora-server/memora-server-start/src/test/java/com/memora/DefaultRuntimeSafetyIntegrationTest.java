package com.memora;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = MemoraApplication.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:memora_runtime_default;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "logging.level.com.memora=warn"
    }
)
@AutoConfigureMockMvc
class DefaultRuntimeSafetyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectDemoTokenInDefaultRuntimeMode() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/current/dashboard")
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("当前请求未携带有效会话"));
    }

    @Test
    void shouldNotSeedDemoAccountsInDefaultRuntimeMode() {
        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_account", Long.class);
        Long tenantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tenant", Long.class);

        assertEquals(0L, userCount == null ? -1L : userCount);
        assertEquals(0L, tenantCount == null ? -1L : tenantCount);
    }

    @Test
    void shouldReturnHttp404ForMissingStaticResourceRequest() throws Exception {
        mockMvc.perform(get("/services/config"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("资源不存在"));
    }
}
