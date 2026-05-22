package com.memora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memora.manager.service.AuditLogService;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.OpaqueTokenCodec;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MemoraApplication.class)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class OnlineDocumentApiIntegrationTest {
    private static final String WEB_CLIENT_HEADER = "X-Memora-Client";
    private static final String WEB_CLIENT_VALUE = "memora-web-app";
    private static final String SESSION_COOKIE_NAME = "MEMORA_SESSION";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OpaqueTokenCodec opaqueTokenCodec;

    @Autowired
    private AuditLogService auditLogService;

    @Test
    void shouldRejectWorkspaceDashboardWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/current/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("当前请求未携带有效会话"));
    }

    @Test
    void shouldReturnWorkspaceDashboardForBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/current/dashboard")
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.workspace.id").value(1))
            .andExpect(jsonPath("$.data.workspace.name").value("华东制造知识中台"))
            .andExpect(jsonPath("$.data.knowledgeBases").isArray());
    }

    @Test
    void shouldReturnCurrentAuthSessionForBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer demo:1:3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(3))
            .andExpect(jsonPath("$.data.tenantId").value(1))
            .andExpect(jsonPath("$.data.displayName").value("陈立"))
            .andExpect(jsonPath("$.data.role").value("REVIEWER"))
            .andExpect(jsonPath("$.data.tenantName").value("华东制造知识中台"))
            .andExpect(jsonPath("$.data.accessToken").value("demo:1:3"));
    }

    @Test
    void shouldLoginAsAdminWithDemoPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andExpect(jsonPath("$.data.email").value("admin@memora.local"))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.role").value("OWNER"))
            .andExpect(jsonPath("$.data.accessToken").value(Matchers.startsWith("session:")));
    }

    @Test
    void shouldUseHttpOnlyCookieSessionForWebClient() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .header(WEB_CLIENT_HEADER, WEB_CLIENT_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andExpect(jsonPath("$.data.accessToken").doesNotExist())
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");

        assertTrue(loginResponse != null && loginResponse.contains(SESSION_COOKIE_NAME + "="));
        assertTrue(loginResponse.contains("HttpOnly"));

        String sessionCookieValue = extractCookieValue(loginResponse, SESSION_COOKIE_NAME);
        mockMvc.perform(get("/api/v1/auth/session")
                .header(WEB_CLIENT_HEADER, WEB_CLIENT_VALUE)
                .cookie(new Cookie(SESSION_COOKIE_NAME, sessionCookieValue)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.role").value("OWNER"))
            .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    void shouldStoreSessionTokenAsHash() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = objectMapper.readTree(response).path("data").path("accessToken").asText();
        String storedToken = jdbcTemplate.queryForObject(
            "SELECT access_token FROM user_session WHERE user_id = ? ORDER BY id DESC LIMIT 1",
            String.class,
            1L
        );

        assertNotEquals(accessToken, storedToken);
        assertEquals(opaqueTokenCodec.hash(accessToken), storedToken);
    }

    @Test
    void shouldRegisterOwnerAndAccessNewWorkspaceDashboard() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/v1/auth/register-owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tenantName": "华北质检中心",
                      "tenantSlug": "north-quality-center",
                      "displayName": "李工",
                      "username": "north-owner",
                      "email": "north.owner@memora.local",
                      "password": "12345678"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("north-owner"))
            .andExpect(jsonPath("$.data.email").value("north.owner@memora.local"))
            .andExpect(jsonPath("$.data.displayName").value("李工"))
            .andExpect(jsonPath("$.data.role").value("OWNER"))
            .andExpect(jsonPath("$.data.tenantName").value("华北质检中心"))
            .andExpect(jsonPath("$.data.accessToken").value(Matchers.startsWith("session:")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = objectMapper.readTree(registerResponse).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/workspaces/current/dashboard")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.workspace.name").value("华北质检中心"))
            .andExpect(jsonPath("$.data.members.length()").value(1))
            .andExpect(jsonPath("$.data.knowledgeBases.length()").value(0));
    }

    @Test
    void shouldCreateInviteAcceptItAndCreateKnowledgeBaseWithAcceptedMember() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        String inviteResponse = mockMvc.perform(post("/api/v1/tenants/current/invites")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "invited.editor@memora.local",
                      "displayName": "受邀编辑",
                      "role": "EDITOR",
                      "expiresInDays": 14
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tenantName").value("华东制造知识中台"))
            .andExpect(jsonPath("$.data.role").value("EDITOR"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String inviteToken = objectMapper.readTree(inviteResponse).path("data").path("inviteToken").asText();

        mockMvc.perform(get("/api/v1/invites/{token}", inviteToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.inviteeEmail").value("invited.editor@memora.local"))
            .andExpect(jsonPath("$.data.role").value("EDITOR"))
            .andExpect(jsonPath("$.data.inviteToken").doesNotExist());

        String acceptResponse = mockMvc.perform(post("/api/v1/invites/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s",
                      "displayName": "受邀编辑",
                      "username": "invited-editor",
                      "email": "invited.editor@memora.local",
                      "password": "12345678"
                    }
                    """.formatted(inviteToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("invited-editor"))
            .andExpect(jsonPath("$.data.role").value("EDITOR"))
            .andExpect(jsonPath("$.data.accessToken").value(Matchers.startsWith("session:")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String invitedAccessToken = objectMapper.readTree(acceptResponse).path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + invitedAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "受邀编辑创建的知识库",
                      "description": "验证邀请接受后的真实会话可继续主链路"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("受邀编辑创建的知识库"));
    }

    @Test
    void shouldListAndRevokeTenantInvites() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        String inviteResponse = mockMvc.perform(post("/api/v1/tenants/current/invites")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "revoke.editor@memora.local",
                      "displayName": "待撤销成员",
                      "role": "EDITOR",
                      "expiresInDays": 7
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode inviteNode = objectMapper.readTree(inviteResponse).path("data");
        long inviteId = inviteNode.path("id").asLong();
        String inviteToken = inviteNode.path("inviteToken").asText();
        String storedInviteToken = jdbcTemplate.queryForObject(
            "SELECT invite_token FROM tenant_invite WHERE id = ?",
            String.class,
            inviteId
        );

        assertNotEquals(inviteToken, storedInviteToken);
        assertEquals(opaqueTokenCodec.hash(inviteToken), storedInviteToken);

        mockMvc.perform(get("/api/v1/tenants/current/invites")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value((int) inviteId))
            .andExpect(jsonPath("$.data[0].inviteeEmail").value("revoke.editor@memora.local"))
            .andExpect(jsonPath("$.data[0].status").value(1))
            .andExpect(jsonPath("$.data[0].inviteToken").doesNotExist());

        mockMvc.perform(post("/api/v1/tenants/current/invites/{id}/revoke", inviteId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) inviteId))
            .andExpect(jsonPath("$.data.status").value(3))
            .andExpect(jsonPath("$.data.revokedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.inviteToken").doesNotExist());

        mockMvc.perform(get("/api/v1/invites/{token}", inviteToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("当前邀请不存在、已失效或已被使用"));
    }

    @Test
    void shouldRedactPublicShareTokenFromAuditRequestPath() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");
        String shareResponse = mockMvc.perform(post("/api/v1/document-shares")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "documentId": 2,
                      "expiresInDays": 7
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String shareToken = objectMapper.readTree(shareResponse).path("data").path("shareToken").asText();

        mockMvc.perform(post("/api/v1/public-shares/{token}/access", shareToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        String requestPath = jdbcTemplate.queryForObject(
            "SELECT request_path FROM audit_log WHERE action_type = ? ORDER BY id DESC LIMIT 1",
            String.class,
            AuditLogConstants.ACTION_ACCESS_DOCUMENT_SHARE
        );
        assertEquals("/api/v1/public-shares/[token]/access", requestPath);
    }

    @Test
    void shouldInvalidateSessionAfterLogout() throws Exception {
        String accessToken = loginAndGetAccessToken("admin", "123456");

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("当前请求未携带有效会话"));
    }

    @Test
    void shouldListAndRevokeOtherSessions() throws Exception {
        String currentAccessToken = loginAndGetAccessToken("admin", "123456");
        String otherAccessToken = loginAndGetAccessToken("admin", "123456");

        String sessionsResponse = mockMvc.perform(get("/api/v1/auth/sessions")
                .header("Authorization", "Bearer " + currentAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()", Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data[0].clientType").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode sessionNodes = objectMapper.readTree(sessionsResponse).path("data");
        long otherSessionId = 0L;
        for (JsonNode sessionNode : sessionNodes) {
            if (!sessionNode.path("current").asBoolean(false)) {
                otherSessionId = sessionNode.path("id").asLong();
                break;
            }
        }
        assertTrue(otherSessionId > 0);

        mockMvc.perform(post("/api/v1/auth/sessions/{sessionId}/revoke", otherSessionId)
                .header("Authorization", "Bearer " + currentAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.revokedCount").value(1))
            .andExpect(jsonPath("$.data.currentSessionRevoked").value(false));

        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer " + otherAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));

        String thirdAccessToken = loginAndGetAccessToken("admin", "123456");

        mockMvc.perform(post("/api/v1/auth/sessions/revoke-others")
                .header("Authorization", "Bearer " + currentAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.revokedCount").value(Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.currentSessionRevoked").value(false));

        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer " + thirdAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + currentAccessToken)
                .param("size", "20")
                .param("objectType", "USER_SESSION")
                .param("resultType", "SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "REVOKE_USER_SESSION",
                "REVOKE_OTHER_USER_SESSIONS"
            )));
    }

    @Test
    void shouldAllowTenantManagerToGovernWorkspaceSessions() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");
        String viewerAccessToken = loginAndGetAccessToken("viewer", "123456");
        String reviewerAccessToken = loginAndGetAccessToken("reviewer", "123456");
        String reviewerSecondAccessToken = loginAndGetAccessToken("reviewer", "123456");

        String tenantSessionsResponse = mockMvc.perform(get("/api/v1/auth/tenant-sessions")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].username", Matchers.hasItems("admin", "viewer", "reviewer")))
            .andExpect(jsonPath("$.data[*].role", Matchers.hasItems("OWNER", "VIEWER", "REVIEWER")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        mockMvc.perform(get("/api/v1/auth/tenant-sessions")
                .header("Authorization", "Bearer " + viewerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前角色无管理权限"));

        JsonNode sessionNodes = objectMapper.readTree(tenantSessionsResponse).path("data");
        long viewerSessionId = 0L;
        for (JsonNode sessionNode : sessionNodes) {
            if ("viewer".equals(sessionNode.path("username").asText())) {
                viewerSessionId = sessionNode.path("id").asLong();
                break;
            }
        }
        assertTrue(viewerSessionId > 0);

        mockMvc.perform(post("/api/v1/auth/tenant-sessions/{sessionId}/revoke", viewerSessionId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.revokedCount").value(1))
            .andExpect(jsonPath("$.data.currentSessionRevoked").value(false));

        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer " + viewerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/v1/auth/tenant-sessions/users/{userId}/revoke", 3L)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.revokedCount").value(Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.currentSessionRevoked").value(false));

        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer " + reviewerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer " + reviewerSecondAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "20")
                .param("objectType", "USER_SESSION")
                .param("resultType", "SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "REVOKE_TENANT_USER_SESSION",
                "REVOKE_TENANT_MEMBER_SESSIONS"
            )));
    }

    @Test
    void shouldListJoinedWorkspacesAndSwitchCurrentWorkspace() throws Exception {
        String accessToken = loginAndGetAccessToken("admin", "123456");

        mockMvc.perform(get("/api/v1/workspaces/joined")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data[0].tenantId").value(1))
            .andExpect(jsonPath("$.data[*].tenantId", Matchers.hasItem(2)))
            .andExpect(jsonPath("$.data[*].tenantName", Matchers.hasItem("华南售后协同中心")));

        String switchResponse = mockMvc.perform(post("/api/v1/workspaces/{tenantId}/switch", 2L)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tenantId").value(2))
            .andExpect(jsonPath("$.data.tenantName").value("华南售后协同中心"))
            .andExpect(jsonPath("$.data.role").value("OWNER"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String switchedAccessToken = objectMapper.readTree(switchResponse).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/workspaces/current/dashboard")
                .header("Authorization", "Bearer " + switchedAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.workspace.id").value(2))
            .andExpect(jsonPath("$.data.workspace.name").value("华南售后协同中心"))
            .andExpect(jsonPath("$.data.knowledgeBases[*].name", Matchers.hasItem("区域服务值班手册")));
    }

    @Test
    void shouldOnlyReturnDocumentNodesForUnifiedSearch() throws Exception {
        String accessToken = loginAndGetAccessToken("admin", "123456");

        String knowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "统一搜索验证知识库",
                      "description": "验证全局搜索只返回正文文档"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(knowledgeBaseResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "巡检手册目录",
                      "docType": "FOLDER"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "巡检手册正文",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>巡检手册包含值班检查、升级回退和交接处理。</p>"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .param("keyword", "巡检手册"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].docType", Matchers.everyItem(Matchers.equalTo("DOC"))))
            .andExpect(jsonPath("$.data.records[*].title", Matchers.hasItem("巡检手册正文")))
            .andExpect(jsonPath("$.data.records[*].title", Matchers.not(Matchers.hasItem("巡检手册目录"))));
    }

    @Test
    void shouldSortUnifiedSearchByRelevanceBeforeRecency() throws Exception {
        String accessToken = loginAndGetAccessToken("admin", "123456");

        String knowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "搜索排序验证知识库",
                      "description": "验证搜索按相关度优先"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(knowledgeBaseResponse).path("data").path("id").asLong();

        String exactTitleResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "API key",
                      "docType": "DOC",
                      "format": "MARKDOWN",
                      "content": "# API key\\n精确标题命中"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String contentOnlyResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "轮换策略",
                      "docType": "DOC",
                      "format": "MARKDOWN",
                      "content": "# 轮换策略\\n这里介绍 API key 的轮换和失效处理。"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long exactTitleDocumentId = objectMapper.readTree(exactTitleResponse).path("data").path("id").asLong();
        long contentOnlyDocumentId = objectMapper.readTree(contentOnlyResponse).path("data").path("id").asLong();

        jdbcTemplate.update("UPDATE document SET updated_at = ? WHERE id = ?", LocalDateTime.now().minusDays(2), exactTitleDocumentId);
        jdbcTemplate.update("UPDATE document SET updated_at = ? WHERE id = ?", LocalDateTime.now(), contentOnlyDocumentId);

        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .param("keyword", "API key")
                .param("knowledgeBaseId", String.valueOf(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].title").value("API key"));
    }

    @Test
    void shouldWriteAuditLogsForAuthAndInviteLifecycle() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/v1/auth/register-owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tenantName": "华中审计中心",
                      "tenantSlug": "central-audit-center",
                      "displayName": "周岚",
                      "username": "audit-owner",
                      "email": "audit.owner@memora.local",
                      "password": "12345678"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String registerAccessToken = objectMapper.readTree(registerResponse).path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + registerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "audit-owner",
                      "password": "12345678"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String ownerAccessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        String inviteResponse = mockMvc.perform(post("/api/v1/tenants/current/invites")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "audit.invited@memora.local",
                      "displayName": "被审计成员",
                      "role": "EDITOR",
                      "expiresInDays": 10
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String inviteToken = objectMapper.readTree(inviteResponse).path("data").path("inviteToken").asText();

        mockMvc.perform(post("/api/v1/invites/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s",
                      "displayName": "被审计成员",
                      "username": "audit-invited",
                      "email": "audit.invited@memora.local",
                      "password": "12345678"
                    }
                    """.formatted(inviteToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "REGISTER_OWNER",
                "LOGOUT",
                "LOGIN",
                "CREATE_INVITE",
                "ACCEPT_INVITE"
            )));
    }

    @Test
    void shouldWriteAndQueryAuditLogsForKnowledgeBaseAndDocumentActions() throws Exception {
        String accessToken = loginAndGetAccessToken("admin", "123456");

        String knowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "审计链路知识库",
                      "description": "验证知识库与文档关键动作审计"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(knowledgeBaseResponse).path("data").path("id").asLong();

        String folderResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "审计目录",
                      "docType": "FOLDER"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long folderId = objectMapper.readTree(folderResponse).path("data").path("id").asLong();

        String documentResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "审计文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>版本一正文</p>"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(documentResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "审计文档",
                      "format": "MARKDOWN",
                      "content": "# 审计文档\\n版本二正文"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": %d
                    }
                    """.formatted(folderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        String versionsResponse = mockMvc.perform(get("/api/v1/documents/{id}/versions", documentId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long rollbackVersionId = objectMapper.readTree(versionsResponse)
            .path("data")
            .get(0)
            .path("id")
            .asLong();

        mockMvc.perform(post("/api/v1/documents/{id}/rollback/{versionId}", documentId, rollbackVersionId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(delete("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/v1/documents/{id}/restore", documentId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(put("/api/v1/knowledge-bases/{id}/members", knowledgeBaseId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "members": [
                        { "userId": 1, "role": "OWNER" },
                        { "userId": 2, "role": "EDITOR" }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + accessToken)
                .param("knowledgeBaseId", String.valueOf(knowledgeBaseId))
                .param("size", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].knowledgeBaseId", Matchers.everyItem(Matchers.equalTo((int) knowledgeBaseId))))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "CREATE_KNOWLEDGE_BASE",
                "CREATE_DOCUMENT",
                "UPDATE_DOCUMENT",
                "MOVE_DOCUMENT",
                "ROLLBACK_DOCUMENT",
                "DELETE_DOCUMENT",
                "RESTORE_DOCUMENT",
                "UPDATE_KNOWLEDGE_BASE_MEMBERS"
            )));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + accessToken)
                .param("objectType", "DOCUMENT")
                .param("objectId", String.valueOf(documentId))
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].objectId", Matchers.everyItem(Matchers.equalTo((int) documentId))))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "CREATE_DOCUMENT",
                "UPDATE_DOCUMENT",
                "MOVE_DOCUMENT",
                "ROLLBACK_DOCUMENT",
                "DELETE_DOCUMENT",
                "RESTORE_DOCUMENT"
            )));
    }

    @Test
    void shouldRejectAuditQueryForViewer() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer demo:1:4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前角色无管理权限"));
    }

    @Test
    void shouldAllowAdminLoginFlowToCreateKnowledgeBaseAndEditDocument() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        String knowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "admin登录新建知识库",
                      "description": "验证 admin 登录后可创建知识库"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.tenantId").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(knowledgeBaseResponse).path("data").path("id").asLong();

        String documentResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "admin登录新建文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>首次创建正文</p>"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.knowledgeBaseId").value((int) knowledgeBaseId))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(documentResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "format": "MARKDOWN",
                      "content": "# admin登录新建文档\\n已完成正文编辑",
                      "summary": "验证 admin 登录后可编辑文档"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId))
            .andExpect(jsonPath("$.data.versionNo").value(2))
            .andExpect(jsonPath("$.data.summary").value("验证 admin 登录后可编辑文档"));
    }

    @Test
    void shouldExposeAdminCreatedKnowledgeBaseAndEditedDocumentAcrossMainQueries() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        String knowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "admin链路验证知识库",
                      "description": "验证 admin 主链路查询闭环"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.currentRole").value("OWNER"))
            .andExpect(jsonPath("$.data.canWrite").value(true))
            .andExpect(jsonPath("$.data.canManage").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(knowledgeBaseResponse).path("data").path("id").asLong();

        String documentResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "admin链路验证文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>版本一正文</p>"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(documentResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "format": "MARKDOWN",
                      "content": "# admin链路验证文档\\n版本二正文",
                      "summary": "主链路编辑后摘要"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(2))
            .andExpect(jsonPath("$.data.summary").value("主链路编辑后摘要"));

        mockMvc.perform(get("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId))
            .andExpect(jsonPath("$.data.knowledgeBaseId").value((int) knowledgeBaseId))
            .andExpect(jsonPath("$.data.content").value("# admin链路验证文档\n版本二正文"))
            .andExpect(jsonPath("$.data.contentText").value("admin链路验证文档 版本二正文"))
            .andExpect(jsonPath("$.data.summary").value("主链路编辑后摘要"))
            .andExpect(jsonPath("$.data.versionNo").value(2));

        mockMvc.perform(get("/api/v1/documents/{id}/versions", documentId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].documentId").value((int) documentId))
            .andExpect(jsonPath("$.data[0].version").value(1))
            .andExpect(jsonPath("$.data[0].contentText").value("版本一正文"))
            .andExpect(jsonPath("$.data[0].remark").value("自动版本快照"));

        mockMvc.perform(get("/api/v1/knowledge-bases/{id}", knowledgeBaseId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) knowledgeBaseId))
            .andExpect(jsonPath("$.data.documentCount").value(1))
            .andExpect(jsonPath("$.data.currentRole").value("OWNER"))
            .andExpect(jsonPath("$.data.canWrite").value(true))
            .andExpect(jsonPath("$.data.canManage").value(true));

        mockMvc.perform(get("/api/v1/knowledge-bases/{id}/document-tree", knowledgeBaseId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.hasItem((int) documentId)))
            .andExpect(jsonPath("$.data[*].title", Matchers.hasItem("admin链路验证文档")));

        mockMvc.perform(get("/api/v1/workspaces/current/dashboard")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.workspace.id").value(1))
            .andExpect(jsonPath("$.data.knowledgeBases[*].id", Matchers.hasItem((int) knowledgeBaseId)))
            .andExpect(jsonPath("$.data.knowledgeBases[*].name", Matchers.hasItem("admin链路验证知识库")));
    }

    @Test
    void shouldNotCreateVersionForMetadataOnlyDocumentUpdate() throws Exception {
        String documentResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "仅元数据更新文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>用于验证版本语义收口</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(documentResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "summary": "仅更新摘要，不生成正文版本"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.summary").value("仅更新摘要，不生成正文版本"));

        mockMvc.perform(get("/api/v1/documents/{id}/versions", documentId)
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldNormalizeDocumentFormatContentAndRejectUnsupportedFormat() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "格式收口文档",
                      "docType": "DOC",
                      "format": "HTML",
                      "content": "<article><h1>格式收口文档</h1><p>支持直接展示 HTML 内容。</p></article>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.format").value("HTML"))
            .andExpect(jsonPath("$.data.contentText").value("格式收口文档 支持直接展示 HTML 内容。"))
            .andExpect(jsonPath("$.data.summary").value("格式收口文档 支持直接展示 HTML 内容。"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "format": "MARKDOWN",
                      "content": "# 格式收口文档\\n\\n支持切换到 Markdown 正文。"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.format").value("MARKDOWN"))
            .andExpect(jsonPath("$.data.contentText").value("格式收口文档 支持切换到 Markdown 正文。"))
            .andExpect(jsonPath("$.data.summary").value("格式收口文档 支持切换到 Markdown 正文。"));

        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "非法格式文档",
                      "docType": "DOC",
                      "format": "PDF",
                      "content": "不应该被接受"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("不支持的文档格式: PDF"));
    }

    @Test
    void shouldRepairLegacyDocumentFormatDataThroughMaintenanceEndpoint() throws Exception {
        String knowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "历史格式治理知识库",
                      "description": "用于验证维护接口"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(knowledgeBaseResponse).path("data").path("id").asLong();

        String documentResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "parentId": 0,
                      "title": "历史格式治理文档",
                      "docType": "DOC",
                      "format": "MARKDOWN",
                      "content": "# 历史格式治理文档\\n\\n旧格式正文"
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(documentResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "# 历史格式治理文档\\n\\n第二版正文"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(2));

        jdbcTemplate.update("""
            UPDATE document
            SET doc_type = ?, format = NULL, content_text = NULL, summary = ?
            WHERE id = ?
            """, "doc", "   ", documentId);
        jdbcTemplate.update("""
            UPDATE document_version
            SET format = NULL, content_text = NULL
            WHERE document_id = ?
            """, documentId);

        mockMvc.perform(get("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.docType").value("DOC"))
            .andExpect(jsonPath("$.data.format").value("MARKDOWN"))
            .andExpect(jsonPath("$.data.contentText").value("历史格式治理文档 第二版正文"))
            .andExpect(jsonPath("$.data.summary").value("历史格式治理文档 第二版正文"));

        mockMvc.perform(get("/api/v1/documents/{id}/versions", documentId)
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].format").value("MARKDOWN"))
            .andExpect(jsonPath("$.data[0].contentText").value("历史格式治理文档 旧格式正文"));

        mockMvc.perform(post("/api/v1/documents/maintenance/normalize-content")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "dryRun": true
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.dryRun").value(true))
            .andExpect(jsonPath("$.data.updatedDocumentCount").value(1))
            .andExpect(jsonPath("$.data.documentDocTypeUpdatedCount").value(1))
            .andExpect(jsonPath("$.data.documentFormatUpdatedCount").value(1))
            .andExpect(jsonPath("$.data.documentContentTextUpdatedCount").value(1))
            .andExpect(jsonPath("$.data.documentSummaryUpdatedCount").value(1))
            .andExpect(jsonPath("$.data.updatedVersionCount").value(1))
            .andExpect(jsonPath("$.data.versionFormatUpdatedCount").value(1))
            .andExpect(jsonPath("$.data.versionContentTextUpdatedCount").value(1));

        assertEquals("doc", jdbcTemplate.queryForObject("SELECT doc_type FROM document WHERE id = ?", String.class, documentId));
        assertEquals(null, jdbcTemplate.queryForObject("SELECT format FROM document WHERE id = ?", String.class, documentId));

        mockMvc.perform(post("/api/v1/documents/maintenance/normalize-content")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": %d,
                      "dryRun": false
                    }
                    """.formatted(knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.dryRun").value(false))
            .andExpect(jsonPath("$.data.updatedDocumentCount").value(1))
            .andExpect(jsonPath("$.data.updatedVersionCount").value(1));

        assertEquals("DOC", jdbcTemplate.queryForObject("SELECT doc_type FROM document WHERE id = ?", String.class, documentId));
        assertEquals("MARKDOWN", jdbcTemplate.queryForObject("SELECT format FROM document WHERE id = ?", String.class, documentId));
        assertEquals("历史格式治理文档 第二版正文", jdbcTemplate.queryForObject("SELECT content_text FROM document WHERE id = ?", String.class, documentId));
        assertEquals("历史格式治理文档 第二版正文", jdbcTemplate.queryForObject("SELECT summary FROM document WHERE id = ?", String.class, documentId));
        assertEquals("MARKDOWN", jdbcTemplate.queryForObject("SELECT format FROM document_version WHERE document_id = ?", String.class, documentId));
        assertEquals("历史格式治理文档 旧格式正文", jdbcTemplate.queryForObject("SELECT content_text FROM document_version WHERE document_id = ?", String.class, documentId));
    }

    @Test
    void shouldCreateKnowledgeBaseUsingAccessContextWhenBodyOmitsTenantAndUser() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "现场培训手册",
                      "description": "用于验证会话上下文注入"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tenantId").value(1))
            .andExpect(jsonPath("$.data.userId").value(2))
            .andExpect(jsonPath("$.data.name").value("现场培训手册"));
    }

    @Test
    void shouldIgnoreBodyUserIdWhenCreatingKnowledgeBase() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "忽略主体归属的知识库",
                      "description": "验证创建主体始终取当前会话",
                      "userId": 4
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tenantId").value(1))
            .andExpect(jsonPath("$.data.userId").value(2))
            .andExpect(jsonPath("$.data.name").value("忽略主体归属的知识库"));
    }

    @Test
    void shouldReturnConflictWhenCreatingDuplicateKnowledgeBaseSlug() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "设备交付知识库冲突测试",
                      "slug": "delivery-playbook",
                      "description": "验证知识库唯一键冲突错误语义"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value("同一租户下已存在同名或同标识知识库"));
    }

    @Test
    void shouldListDeletedKnowledgeBaseInTrashAndRestoreIt() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "待恢复知识库",
                      "description": "验证知识库回收站与恢复"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long knowledgeBaseId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/knowledge-bases/{id}", knowledgeBaseId)
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/knowledge-bases/trash")
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.hasItem((int) knowledgeBaseId)))
            .andExpect(jsonPath("$.data[*].deletedBy", Matchers.hasItem(1)));

        mockMvc.perform(post("/api/v1/knowledge-bases/{id}/restore", knowledgeBaseId)
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) knowledgeBaseId))
            .andExpect(jsonPath("$.data.status").value(1));

        mockMvc.perform(get("/api/v1/knowledge-bases/{id}", knowledgeBaseId)
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) knowledgeBaseId));
    }

    @Test
    void shouldReturnDocumentTreeForKnowledgeBase() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge-bases/1/document-tree")
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].knowledgeBaseId").value(1))
            .andExpect(jsonPath("$.data[0].path").exists());
    }

    @Test
    void shouldCreateDocumentUsingAccessContextWhenBodyOmitsUser() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "现场交接记录",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>用于记录交接事项</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(2))
            .andExpect(jsonPath("$.data.parentId").value(1))
            .andExpect(jsonPath("$.data.path").value("/delivery-overview/现场交接记录"));
    }

    @Test
    void shouldIgnoreBodyUserIdWhenCreatingDocument() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "忽略主体归属的文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>验证文档创建主体始终取当前会话</p>",
                      "userId": 4
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(2))
            .andExpect(jsonPath("$.data.parentId").value(1))
            .andExpect(jsonPath("$.data.path").value("/delivery-overview/忽略主体归属的文档"));
    }

    @Test
    void shouldReturnConflictWhenCreatingDuplicateDocumentPath() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "项目启动清单冲突测试",
                      "slug": "kickoff-checklist",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>验证文档路径唯一键冲突错误语义</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value("当前目录下已存在同名文档或目录"));
    }

    @Test
    void shouldFilterRestrictedKnowledgeBaseDocumentsFromGlobalDocumentList() throws Exception {
        jdbcTemplate.update("""
            INSERT INTO knowledge_base (
              id, tenant_id, name, slug, description, user_id, status, document_count, view_count, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, 102L, 1L, "全局列表受限知识库", "restricted-global-kb", "用于测试全局文档列表权限过滤", 1L, 1, 1, 0, 9);
        jdbcTemplate.update("""
            INSERT INTO knowledge_base_member (knowledge_base_id, tenant_id, user_id, role, status)
            VALUES (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)
            """, 102L, 1L, 3L, "EDITOR", 1, 102L, 1L, 4L, "VIEWER", 1);
        jdbcTemplate.update("""
            INSERT INTO document (
              id, tenant_id, title, slug, doc_type, format, content, content_text, summary,
              knowledge_base_id, user_id, parent_id, path, depth, version_no, status, view_count, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, 202L, 1L, "受限文档", "restricted-global-doc", "DOC", "MARKDOWN", "# 受限文档", "受限文档", "用于测试全局文档列表权限过滤", 102L, 3L, 0L, "/restricted-global-doc", 0, 1, 1, 0, 0);

        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .param("page", "1")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].id", Matchers.not(Matchers.hasItem(202))));

        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:3")
                .param("page", "1")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].id", Matchers.hasItem(202)));
    }

    @Test
    void shouldListDeletedDocumentInTrashAndRestoreIt() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "待恢复文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>验证文档回收站与恢复</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/documents/trash")
                .header("Authorization", "Bearer demo:1:2")
                .param("knowledgeBaseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.hasItem((int) documentId)))
            .andExpect(jsonPath("$.data[*].deletedBy", Matchers.hasItem(2)));

        mockMvc.perform(post("/api/v1/documents/{id}/restore", documentId)
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId))
            .andExpect(jsonPath("$.data.status").value(1));

        mockMvc.perform(get("/api/v1/documents/{id}", documentId)
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId));
    }

    @Test
    void shouldRequireRestoringParentFolderBeforeChildDocument() throws Exception {
        String folderResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "待恢复目录",
                      "docType": "FOLDER"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long folderId = objectMapper.readTree(folderResponse).path("data").path("id").asLong();

        String childResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": %d,
                      "title": "待恢复子文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>验证恢复父级约束</p>"
                    }
                    """.formatted(folderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long childId = objectMapper.readTree(childResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/documents/batch-delete")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "documentIds": [%d, %d]
                    }
                    """.formatted(folderId, childId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/v1/documents/{id}/restore", childId)
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("父级目录已删除，请先恢复父级目录"));
    }

    @Test
    void shouldRejectMovingFolderIntoItsOwnDescendant() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "交付附录",
                      "docType": "FOLDER"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(createResponse);
        long childFolderId = root.path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/1")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": %d
                    }
                    """.formatted(childFolderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("目录不能移动到自己的子级目录下"));
    }

    @Test
    void shouldBatchMoveTopLevelSelectionWithoutMovingChildTwice() throws Exception {
        String folderResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "批量移动目录",
                      "docType": "FOLDER"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long folderId = objectMapper.readTree(folderResponse).path("data").path("id").asLong();

        String childResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": %d,
                      "title": "批量移动子文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>用于测试批量移动</p>"
                    }
                    """.formatted(folderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long childId = objectMapper.readTree(childResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/documents/batch-move")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "documentIds": [%d, %d],
                      "parentId": 0
                    }
                    """.formatted(folderId, childId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/documents/%d".formatted(folderId))
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.parentId").value(0))
            .andExpect(jsonPath("$.data.path").value("/批量移动目录"));

        mockMvc.perform(get("/api/v1/documents/%d".formatted(childId))
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.parentId").value(folderId))
            .andExpect(jsonPath("$.data.path").value("/批量移动目录/批量移动子文档"));
    }

    @Test
    void shouldRejectBatchDeleteWhenFolderStillHasUnselectedDescendant() throws Exception {
        String folderResponse = mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "批量删除目录",
                      "docType": "FOLDER"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long folderId = objectMapper.readTree(folderResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": %d,
                      "title": "批量删除子文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>用于测试批量删除</p>"
                    }
                    """.formatted(folderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/v1/documents/batch-delete")
                .header("Authorization", "Bearer demo:1:2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "documentIds": [%d]
                    }
                    """.formatted(folderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("目录“批量删除目录”仍有未选中的子节点，不能批量删除"));
    }

    @Test
    void shouldRejectCrossTenantKnowledgeBaseAndDocumentAccess() throws Exception {
        long foreignTenantId = 20L;

        jdbcTemplate.update("""
            INSERT INTO tenant (id, name, slug, industry, plan_name, owner_user_id, status)
            VALUES (?, '华北交付中心', 'north-delivery-center', '工业制造', 'ENTERPRISE', 9, 1)
            """, foreignTenantId);
        jdbcTemplate.update("""
            INSERT INTO knowledge_base (
              id, tenant_id, name, slug, description, user_id, status, document_count, view_count, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, 101L, foreignTenantId, "北区交付库", "north-delivery", "租户 2 的知识库", 9L, 1, 1, 0, 0);
        jdbcTemplate.update("""
            INSERT INTO document (
              id, tenant_id, title, slug, doc_type, format, content, content_text, summary,
              knowledge_base_id, user_id, parent_id, path, depth, version_no, status, view_count, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, 201L, foreignTenantId, "北区说明", "north-guide", "DOC", "MARKDOWN", "# 北区说明", "北区说明", "租户 2 文档", 101L, 9L, 0L, "/north-guide", 0, 1, 1, 0, 0);

        mockMvc.perform(get("/api/v1/knowledge-bases/101")
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权访问该知识库"));

        mockMvc.perform(get("/api/v1/documents/201")
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权访问该文档"));
    }

    @Test
    void shouldRejectCrossTenantKnowledgeBaseListOverride() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer demo:1:1")
                .param("tenantId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权访问当前租户数据"));
    }

    @Test
    void shouldRejectViewerWriteOperations() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "只读用户新文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>viewer should be rejected</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前知识库角色无写权限"));
    }

    @Test
    void shouldRejectReviewerKnowledgeBaseManagementAndNonMemberAccess() throws Exception {
        mockMvc.perform(delete("/api/v1/knowledge-bases/1")
                .header("Authorization", "Bearer demo:1:3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前知识库角色无管理权限"));

        mockMvc.perform(get("/api/v1/workspaces/current/dashboard")
                .header("Authorization", "Bearer demo:1:99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前用户不属于该租户"));
    }

    @Test
    void shouldApplyKnowledgeBaseLevelPermissionsWhenConfigured() throws Exception {
        jdbcTemplate.update("""
            INSERT INTO knowledge_base (
              id, tenant_id, name, slug, description, user_id, status, document_count, view_count, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, 102L, 1L, "受限知识库", "restricted-kb", "用于测试知识库级权限", 1L, 1, 0, 0, 9);
        jdbcTemplate.update("""
            INSERT INTO knowledge_base_member (knowledge_base_id, tenant_id, user_id, role, status)
            VALUES (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)
            """, 102L, 1L, 3L, "EDITOR", 1, 102L, 1L, 4L, "VIEWER", 1);

        mockMvc.perform(get("/api/v1/knowledge-bases/102")
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前用户无权访问该知识库"));

        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 102,
                      "parentId": 0,
                      "title": "知识库级授权文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>reviewer with kb editor should pass</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.knowledgeBaseId").value(102))
            .andExpect(jsonPath("$.data.userId").value(3));

        mockMvc.perform(post("/api/v1/documents")
                .header("Authorization", "Bearer demo:1:4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 102,
                      "parentId": 0,
                      "title": "知识库级只读文档",
                      "docType": "DOC",
                      "format": "RICH_TEXT",
                      "content": "<p>viewer should be rejected by kb role</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前知识库角色无写权限"));

        mockMvc.perform(get("/api/v1/knowledge-bases/tenant/1")
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.not(Matchers.hasItem(102))));
    }

    @Test
    void shouldManageKnowledgeBaseMembersAndExposePermissionFlags() throws Exception {
        mockMvc.perform(put("/api/v1/knowledge-bases/1/members")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "members": [
                        { "userId": 1, "role": "OWNER" },
                        { "userId": 4, "role": "VIEWER" }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].userId").value(1))
            .andExpect(jsonPath("$.data[0].displayName").value("王晨"))
            .andExpect(jsonPath("$.data[1].userId").value(4))
            .andExpect(jsonPath("$.data[1].displayName").value("赵敏"));

        mockMvc.perform(get("/api/v1/knowledge-bases/1/members")
                .header("Authorization", "Bearer demo:1:1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/knowledge-bases/1")
                .header("Authorization", "Bearer demo:1:4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.currentRole").value("VIEWER"))
            .andExpect(jsonPath("$.data.canWrite").value(false))
            .andExpect(jsonPath("$.data.canManage").value(false))
            .andExpect(jsonPath("$.data.permissionRestricted").value(true));

        mockMvc.perform(get("/api/v1/knowledge-bases/1")
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前用户无权访问该知识库"));
    }

    @Test
    void shouldRejectKnowledgeBaseMemberUpdateByNonManager() throws Exception {
        mockMvc.perform(put("/api/v1/knowledge-bases/1/members")
                .header("Authorization", "Bearer demo:1:3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "members": [
                        { "userId": 3, "role": "ADMIN" }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前知识库角色无管理权限"));
    }

    @Test
    void shouldClearKnowledgeBaseMemberRestrictions() throws Exception {
        jdbcTemplate.update("""
            INSERT INTO knowledge_base (
              id, tenant_id, name, slug, description, user_id, status, document_count, view_count, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, 103L, 1L, "待清理权限知识库", "permission-reset-kb", "用于测试清空知识库权限限制", 1L, 1, 0, 0, 10);
        jdbcTemplate.update("""
            INSERT INTO knowledge_base_member (knowledge_base_id, tenant_id, user_id, role, status)
            VALUES (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)
            """, 103L, 1L, 1L, "OWNER", 1, 103L, 1L, 4L, "VIEWER", 1);

        mockMvc.perform(put("/api/v1/knowledge-bases/103/members")
                .header("Authorization", "Bearer demo:1:1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "members": []
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/knowledge-bases/103")
                .header("Authorization", "Bearer demo:1:2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.currentRole").value("EDITOR"))
            .andExpect(jsonPath("$.data.canWrite").value(true))
            .andExpect(jsonPath("$.data.canManage").value(false))
            .andExpect(jsonPath("$.data.permissionRestricted").value(false));
    }

    @Test
    void shouldRecordFailedLoginAndExportFailureAuditLogs() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "wrong-password",
                      "tenantSlug": "east-manufacturing-docs"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("resultType", "FAILURE")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItem("LOGIN")))
            .andExpect(jsonPath("$.data.records[*].resultType", Matchers.everyItem(Matchers.equalTo("FAILURE"))));

        mockMvc.perform(get("/api/v1/audit-logs/summary")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.configuredRetentionDays").value(3650))
            .andExpect(jsonPath("$.data.exportMaxSize").value(1000))
            .andExpect(jsonPath("$.data.failureCount", Matchers.greaterThanOrEqualTo(1)));

        String csv = mockMvc.perform(get("/api/v1/audit-logs/export")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("resultType", "FAILURE"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(csv.contains("FAILURE"));
        assertTrue(csv.contains("LOGIN"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItem("EXPORT_AUDIT_LOG")));
    }

    @Test
    void shouldArchiveExpiredAuditLogsAndExportArchivedCsv() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");
        LocalDateTime firstExpiredAt = LocalDateTime.now().minusDays(4100);
        LocalDateTime secondExpiredAt = LocalDateTime.now().minusDays(3900);

        jdbcTemplate.update("""
            INSERT INTO audit_log (
              id, tenant_id, knowledge_base_id, knowledge_base_name, actor_type, actor_user_id,
              actor_display_name, actor_role, object_type, object_id, object_title, action_type,
              result_type, detail, source_type, request_method, request_path, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            9001L, 1L, null, null, "USER", 1L,
            "管理员", "OWNER", "TENANT", 1L, "旧成功审计", "LOGIN",
            "SUCCESS", "过期成功记录", "DIRECT_API", "POST", "/api/v1/auth/login", firstExpiredAt
        );
        jdbcTemplate.update("""
            INSERT INTO audit_log (
              id, tenant_id, knowledge_base_id, knowledge_base_name, actor_type, actor_user_id,
              actor_display_name, actor_role, object_type, object_id, object_title, action_type,
              result_type, detail, source_type, request_method, request_path, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            9002L, 1L, null, null, "USER", 1L,
            "管理员", "OWNER", "TENANT", 1L, "旧失败审计", "LOGIN",
            "FAILURE", "过期失败记录", "DIRECT_API", "POST", "/api/v1/auth/login", secondExpiredAt
        );

        mockMvc.perform(get("/api/v1/audit-logs/summary")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.archiveBatchSize").value(200))
            .andExpect(jsonPath("$.data.archivedCount").value(0))
            .andExpect(jsonPath("$.data.pendingArchiveCount").value(2));

        mockMvc.perform(post("/api/v1/audit-logs/retention/run")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eligibleCount").value(2))
            .andExpect(jsonPath("$.data.archivedCount").value(2))
            .andExpect(jsonPath("$.data.remainingPendingArchiveCount").value(0))
            .andExpect(jsonPath("$.data.archivedTotalCount").value(2));

        Long activeExpiredCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE id IN (9001, 9002)",
            Long.class
        );
        Long archivedExpiredCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log_archive WHERE id IN (9001, 9002)",
            Long.class
        );

        assertTrue(activeExpiredCount != null && activeExpiredCount == 0L);
        assertTrue(archivedExpiredCount != null && archivedExpiredCount == 2L);

        mockMvc.perform(get("/api/v1/audit-logs/summary")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.archivedCount").value(2))
            .andExpect(jsonPath("$.data.pendingArchiveCount").value(0))
            .andExpect(jsonPath("$.data.lastArchivedAt").isNotEmpty());

        String archivedCsv = mockMvc.perform(get("/api/v1/audit-logs/export")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("storageScope", "ARCHIVED"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(archivedCsv.contains("旧成功审计"));
        assertTrue(archivedCsv.contains("旧失败审计"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItem("APPLY_AUDIT_RETENTION")));
    }

    @Test
    void shouldApplyAutomaticAuditRetentionAcrossTenants() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(4200);
        jdbcTemplate.update("""
            INSERT INTO audit_log (
              id, tenant_id, knowledge_base_id, knowledge_base_name, actor_type, actor_user_id,
              actor_display_name, actor_role, object_type, object_id, object_title, action_type,
              result_type, detail, source_type, request_method, request_path, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            9101L, 1L, null, null, "USER", 1L,
            "管理员", "OWNER", "TENANT", 1L, "租户一旧审计", "LOGIN",
            "SUCCESS", "租户一过期记录", "DIRECT_API", "POST", "/api/v1/auth/login", expiredAt
        );
        jdbcTemplate.update("""
            INSERT INTO audit_log (
              id, tenant_id, knowledge_base_id, knowledge_base_name, actor_type, actor_user_id,
              actor_display_name, actor_role, object_type, object_id, object_title, action_type,
              result_type, detail, source_type, request_method, request_path, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            9102L, 2L, null, null, "USER", 5L,
            "华南管理员", "OWNER", "TENANT", 2L, "租户二旧审计", "LOGIN",
            "SUCCESS", "租户二过期记录", "DIRECT_API", "POST", "/api/v1/auth/login", expiredAt
        );

        long archivedCount = auditLogService.runSystemRetentionBatch(List.of(1L, 2L));
        assertEquals(2L, archivedCount);

        Long activeExpiredCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE id IN (9101, 9102)",
            Long.class
        );
        Long archivedExpiredCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log_archive WHERE id IN (9101, 9102)",
            Long.class
        );
        Long automaticAuditEntries = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE source_type = ? AND action_type = ?",
            Long.class,
            "SYSTEM_JOB",
            "APPLY_AUDIT_RETENTION"
        );

        assertEquals(0L, activeExpiredCount);
        assertEquals(2L, archivedExpiredCount);
        assertTrue(automaticAuditEntries != null && automaticAuditEntries >= 2L);
    }

    @Test
    void shouldCreateAccessAndRevokeControlledDocumentShare() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        String shareResponse = mockMvc.perform(post("/api/v1/document-shares")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "documentId": 2,
                      "expiresInDays": 5,
                      "accessCode": "2468"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentId").value(2))
            .andExpect(jsonPath("$.data.accessCodeProtected").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode shareNode = objectMapper.readTree(shareResponse).path("data");
        long shareId = shareNode.path("id").asLong();
        String shareToken = shareNode.path("shareToken").asText();
        String storedShareToken = jdbcTemplate.queryForObject(
            "SELECT share_token FROM document_share_link WHERE id = ?",
            String.class,
            shareId
        );

        assertNotEquals(shareToken, storedShareToken);
        assertEquals(opaqueTokenCodec.hash(shareToken), storedShareToken);

        mockMvc.perform(get("/api/v1/documents/2/shares")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value((int) shareId))
            .andExpect(jsonPath("$.data[0].shareToken").doesNotExist())
            .andExpect(jsonPath("$.data[0].shareUrl").doesNotExist());

        mockMvc.perform(get("/api/v1/public-shares/{token}", shareToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessCodeRequired").value(true))
            .andExpect(jsonPath("$.data.documentTitle").value("项目启动清单"));

        mockMvc.perform(post("/api/v1/public-shares/{token}/access", shareToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accessCode": "0000"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前分享访问码错误"));

        mockMvc.perform(post("/api/v1/public-shares/{token}/access", shareToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accessCode": "2468"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentId").value(2))
            .andExpect(jsonPath("$.data.title").value("项目启动清单"))
            .andExpect(jsonPath("$.data.format").value("MARKDOWN"))
            .andExpect(jsonPath("$.data.contentText").value("项目启动清单 确认客户信息 确认硬件版本"));

        mockMvc.perform(post("/api/v1/document-shares/{shareId}/revoke", shareId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/v1/public-shares/{token}/access", shareToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accessCode": "2468"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(410))
            .andExpect(jsonPath("$.message").value("当前分享已撤销"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("objectType", "DOCUMENT_SHARE_LINK")
                .param("objectId", String.valueOf(shareId))
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "CREATE_DOCUMENT_SHARE",
                "ACCESS_DOCUMENT_SHARE",
                "REVOKE_DOCUMENT_SHARE"
            )))
            .andExpect(jsonPath("$.data.records[*].resultType", Matchers.hasItems("SUCCESS", "FAILURE")));
    }

    @Test
    void shouldManageServiceAccountsAndOperateDocumentsThroughExpandedOpenApi() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        String extraKnowledgeBaseResponse = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Open API 范围切换知识库",
                      "description": "用于验证 API key 作用域重配"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long extraKnowledgeBaseId = objectMapper.readTree(extraKnowledgeBaseResponse).path("data").path("id").asLong();

        String issuedKeyResponse = mockMvc.perform(post("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "ERP 同步写入",
                      "description": "用于外部 ERP 系统写入文档",
                      "keyName": "生产写入 key",
                      "expiresInDays": 180,
                      "scopes": [
                        { "knowledgeBaseId": 1, "accessMode": "WRITE" }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.apiKey.scopes[0].knowledgeBaseId").value(1))
            .andExpect(jsonPath("$.data.apiKey.scopes[0].accessMode").value("WRITE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode issuedNode = objectMapper.readTree(issuedKeyResponse).path("data");
        long serviceAccountId = issuedNode.path("apiKey").path("serviceAccountId").asLong();
        long apiKeyId = issuedNode.path("apiKey").path("id").asLong();
        String plainTextKey = issuedNode.path("plainTextKey").asText();

        mockMvc.perform(get("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].name").value("ERP 同步写入"))
            .andExpect(jsonPath("$.data[0].status").value(1))
            .andExpect(jsonPath("$.data[0].apiKeys[0].scopes[0].knowledgeBaseId").value(1))
            .andExpect(jsonPath("$.data[0].apiKeys[0].plainTextKey").value(plainTextKey));

        mockMvc.perform(post("/api/v1/api-keys/{apiKeyId}/reveal", apiKeyId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.apiKey.id").value((int) apiKeyId))
            .andExpect(jsonPath("$.data.plainTextKey").value(plainTextKey));

        String readOnlyKeyResponse = mockMvc.perform(post("/api/v1/service-accounts/{serviceAccountId}/api-keys", serviceAccountId)
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "只读分析 key",
                      "expiresInDays": 45,
                      "scopes": [
                        { "knowledgeBaseId": 1, "accessMode": "READ" }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.apiKey.scopes[0].accessMode").value("READ"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode readOnlyKeyNode = objectMapper.readTree(readOnlyKeyResponse).path("data");
        long readOnlyApiKeyId = readOnlyKeyNode.path("apiKey").path("id").asLong();
        String readOnlyPlainTextKey = readOnlyKeyNode.path("plainTextKey").asText();
        jdbcTemplate.update("UPDATE api_key SET secret_ciphertext = NULL WHERE id = ?", readOnlyApiKeyId);

        mockMvc.perform(get("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].apiKeys[0].id").value((int) readOnlyApiKeyId))
            .andExpect(jsonPath("$.data[0].apiKeys[0].secretRevealAvailable").value(false))
            .andExpect(jsonPath("$.data[0].apiKeys[0].plainTextKey").doesNotExist())
            .andExpect(jsonPath("$.data[0].apiKeys[1].id").value((int) apiKeyId))
            .andExpect(jsonPath("$.data[0].apiKeys[1].plainTextKey").value(plainTextKey));

        mockMvc.perform(get("/api/v1/open/knowledge-bases")
                .header("Authorization", "ApiKey " + readOnlyPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.hasItem(1)))
            .andExpect(jsonPath("$.data[*].currentRole", Matchers.hasItem("API_KEY_READ")));

        mockMvc.perform(post("/api/v1/open/documents")
                .header("Authorization", "ApiKey " + readOnlyPlainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "读 key 尝试写入",
                      "format": "MARKDOWN",
                      "content": "# 无法写入"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 无权写入该知识库"));

        String openDocumentResponse = mockMvc.perform(post("/api/v1/open/documents")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "Open API 写入文档",
                      "format": "HTML",
                      "content": "<article><h1>Open API 写入文档</h1><p>Open API 第一次写入</p></article>",
                      "sourceExternalId": "legacy-open-api-001",
                      "sourceRevision": "1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.format").value("HTML"))
            .andExpect(jsonPath("$.data.contentText").value("Open API 写入文档 Open API 第一次写入"))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.sourceExternalId").value("legacy-open-api-001"))
            .andExpect(jsonPath("$.data.sourceRevision").value("1"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(openDocumentResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/open/documents/search")
                .header("Authorization", "ApiKey " + readOnlyPlainTextKey)
                .param("keyword", "Open API 写入文档")
                .param("knowledgeBaseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value((int) documentId))
            .andExpect(jsonPath("$.data[0].title").value("Open API 写入文档"));

        mockMvc.perform(put("/api/v1/api-keys/{apiKeyId}/scope", readOnlyApiKeyId)
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scopes": [
                        { "knowledgeBaseId": %d, "accessMode": "READ" }
                      ]
                    }
                    """.formatted(extraKnowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/knowledge-bases")
                .header("Authorization", "ApiKey " + readOnlyPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.hasItem((int) extraKnowledgeBaseId)))
            .andExpect(jsonPath("$.data[*].id", Matchers.not(Matchers.hasItem(1))));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + readOnlyPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 无权读取该知识库"));

        String rotatedKeyResponse = mockMvc.perform(post("/api/v1/api-keys/{apiKeyId}/rotate", apiKeyId)
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode rotatedNode = objectMapper.readTree(rotatedKeyResponse).path("data");
        long rotatedApiKeyId = rotatedNode.path("apiKey").path("id").asLong();
        String rotatedPlainTextKey = rotatedNode.path("plainTextKey").asText();

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + plainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 已吊销"));

        mockMvc.perform(put("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "expectedVersionNo": 1,
                      "format": "MARKDOWN",
                      "content": "# Open API 写入文档\\n第二版"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.format").value("MARKDOWN"))
            .andExpect(jsonPath("$.data.contentText").value("Open API 写入文档 第二版"))
            .andExpect(jsonPath("$.data.versionNo").value(2));

        String versionsResponse = mockMvc.perform(get("/api/v1/open/documents/{documentId}/versions", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long rollbackVersionId = objectMapper.readTree(versionsResponse).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId))
            .andExpect(jsonPath("$.data.format").value("MARKDOWN"))
            .andExpect(jsonPath("$.data.contentText").value("Open API 写入文档 第二版"));

        mockMvc.perform(put("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "expectedVersionNo": 1,
                      "content": "<p>过期版本写入</p>"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value("文档版本已变化，请基于最新 versionNo 重试"));

        mockMvc.perform(delete("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("文档不存在"));

        mockMvc.perform(post("/api/v1/open/documents/{documentId}/restore", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId))
            .andExpect(jsonPath("$.data.status").value(1));

        mockMvc.perform(post("/api/v1/open/documents/{documentId}/rollback/{versionId}", documentId, rollbackVersionId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(3))
            .andExpect(jsonPath("$.data.contentText").value("Open API 写入文档 Open API 第一次写入"));

        mockMvc.perform(post("/api/v1/service-accounts/{serviceAccountId}/disable", serviceAccountId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 对应的机器主体已停用"));

        mockMvc.perform(post("/api/v1/service-accounts/{serviceAccountId}/enable", serviceAccountId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value((int) documentId));

        mockMvc.perform(post("/api/v1/api-keys/{apiKeyId}/disable", rotatedApiKeyId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 已禁用"));

        mockMvc.perform(post("/api/v1/api-keys/{apiKeyId}/revoke", readOnlyApiKeyId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/knowledge-bases")
                .header("Authorization", "ApiKey " + readOnlyPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 已吊销"));

        mockMvc.perform(get("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].apiKeys[*].id", Matchers.not(Matchers.hasItem((int) readOnlyApiKeyId))))
            .andExpect(jsonPath("$.data[0].apiKeys[*].id", Matchers.hasItem((int) rotatedApiKeyId)));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "80"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "CREATE_SERVICE_ACCOUNT",
                "ENABLE_SERVICE_ACCOUNT",
                "DISABLE_SERVICE_ACCOUNT",
                "CREATE_API_KEY",
                "REVEAL_API_KEY",
                "UPDATE_API_KEY_SCOPE",
                "ROTATE_API_KEY",
                "OPEN_API_SEARCH_DOCUMENTS",
                "OPEN_API_CREATE_DOCUMENT",
                "OPEN_API_UPDATE_DOCUMENT",
                "OPEN_API_DELETE_DOCUMENT",
                "OPEN_API_RESTORE_DOCUMENT",
                "OPEN_API_ROLLBACK_DOCUMENT"
            )))
            .andExpect(jsonPath("$.data.records[*].resultType", Matchers.hasItems("SUCCESS", "FAILURE")));
    }

    @Test
    void shouldUpsertDocumentsBySourceExternalIdAndReturnBatchResults() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");
        String plainTextKey = issueWriteApiKey(ownerAccessToken, 1L, "Upsert 同步主体", "Upsert 写入 key");

        String upsertCreateResponse = mockMvc.perform(post("/api/v1/open/documents/upsert")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "外部同步文档",
                      "format": "MARKDOWN",
                      "content": "# 外部同步文档\\n第一版",
                      "sourceExternalId": "erp-asset-001",
                      "sourceRevision": "1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("CREATED"))
            .andExpect(jsonPath("$.data.document.versionNo").value(1))
            .andExpect(jsonPath("$.data.document.sourceExternalId").value("erp-asset-001"))
            .andExpect(jsonPath("$.data.document.sourceRevision").value("1"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(upsertCreateResponse).path("data").path("document").path("id").asLong();

        mockMvc.perform(post("/api/v1/open/documents/upsert")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "外部同步文档",
                      "format": "MARKDOWN",
                      "content": "# 外部同步文档\\n第一版",
                      "sourceExternalId": "erp-asset-001",
                      "sourceRevision": "1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("SKIPPED"))
            .andExpect(jsonPath("$.data.document.id").value((int) documentId));

        mockMvc.perform(post("/api/v1/open/documents/upsert")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "外部同步文档",
                      "format": "MARKDOWN",
                      "content": "# 外部同步文档\\n过期版本",
                      "sourceExternalId": "erp-asset-001",
                      "sourceRevision": "0"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("CONFLICTED"))
            .andExpect(jsonPath("$.data.message").value(Matchers.containsString("sourceRevision")));

        mockMvc.perform(post("/api/v1/open/documents/upsert")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "外部同步文档",
                      "format": "MARKDOWN",
                      "content": "# 外部同步文档\\n第二版",
                      "sourceExternalId": "erp-asset-001",
                      "sourceRevision": "2"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("UPDATED"))
            .andExpect(jsonPath("$.data.document.versionNo").value(2))
            .andExpect(jsonPath("$.data.document.sourceRevision").value("2"));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + plainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.contentText").value("外部同步文档 第二版"))
            .andExpect(jsonPath("$.data.sourceExternalId").value("erp-asset-001"))
            .andExpect(jsonPath("$.data.sourceRevision").value("2"));

        mockMvc.perform(post("/api/v1/open/documents/batch-upsert")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "documents": [
                        {
                          "knowledgeBaseId": 1,
                          "parentId": 1,
                          "title": "外部同步文档",
                          "format": "MARKDOWN",
                          "content": "# 外部同步文档\\n第三版",
                          "sourceExternalId": "erp-asset-001",
                          "sourceRevision": "3"
                        },
                        {
                          "knowledgeBaseId": 1,
                          "parentId": 1,
                          "title": "第二篇同步文档",
                          "format": "HTML",
                          "content": "<article><h1>第二篇同步文档</h1><p>批量新增</p></article>",
                          "sourceExternalId": "erp-asset-002",
                          "sourceRevision": "1"
                        },
                        {
                          "knowledgeBaseId": 1,
                          "parentId": 1,
                          "title": "外部同步文档",
                          "format": "MARKDOWN",
                          "content": "# 外部同步文档\\n第三版",
                          "sourceExternalId": "erp-asset-001",
                          "sourceRevision": "3"
                        },
                        {
                          "knowledgeBaseId": 1,
                          "parentId": 1,
                          "title": "外部同步文档",
                          "format": "MARKDOWN",
                          "content": "# 外部同步文档\\n过期第二版",
                          "sourceExternalId": "erp-asset-001",
                          "sourceRevision": "2"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalCount").value(4))
            .andExpect(jsonPath("$.data.createdCount").value(1))
            .andExpect(jsonPath("$.data.updatedCount").value(1))
            .andExpect(jsonPath("$.data.skippedCount").value(1))
            .andExpect(jsonPath("$.data.conflictedCount").value(1))
            .andExpect(jsonPath("$.data.items[0].itemIndex").value(0))
            .andExpect(jsonPath("$.data.items[0].status").value("UPDATED"))
            .andExpect(jsonPath("$.data.items[1].status").value("CREATED"))
            .andExpect(jsonPath("$.data.items[2].status").value("SKIPPED"))
            .andExpect(jsonPath("$.data.items[3].status").value("CONFLICTED"));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + plainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(3))
            .andExpect(jsonPath("$.data.contentText").value("外部同步文档 第三版"))
            .andExpect(jsonPath("$.data.sourceRevision").value("3"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "80"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "OPEN_API_UPSERT_DOCUMENT",
                "OPEN_API_BATCH_UPSERT_DOCUMENTS"
            )))
            .andExpect(jsonPath("$.data.records[*].resultType", Matchers.hasItems("SUCCESS", "FAILURE")));
    }

    @Test
    void shouldConsumeOpenApiDocumentByViewAndResolveDocumentBySource() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");
        String plainTextKey = issueWriteApiKey(ownerAccessToken, 1L, "消费接入主体", "消费读取 key");

        mockMvc.perform(put("/api/v1/knowledge-bases/1/site")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "siteEnabled": true,
                      "siteSlug": "consume-open-site",
                      "siteTitle": "开放消费站点",
                      "siteDescription": "用于验证开放消费视图"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        String upsertResponse = mockMvc.perform(post("/api/v1/open/documents/upsert")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "开放消费文档",
                      "format": "MARKDOWN",
                      "content": "# 开放消费文档\\n这是给浏览器和对话框读取的正文。",
                      "summary": "开放消费摘要",
                      "sourceExternalId": "browser-clip-001",
                      "sourceRevision": "2026-05-18T10:30:00Z"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("CREATED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(upsertResponse).path("data").path("document").path("id").asLong();

        mockMvc.perform(put("/api/v1/documents/{documentId}/publish", documentId)
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "publicSlug": "consume-open-doc"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}/consume", documentId)
                .header("Authorization", "ApiKey " + plainTextKey)
                .param("view", "rendered"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentId").value((int) documentId))
            .andExpect(jsonPath("$.data.representation").value("rendered"))
            .andExpect(jsonPath("$.data.mimeType").value("text/html"))
            .andExpect(jsonPath("$.data.payload").value(Matchers.containsString("<h1>开放消费文档</h1>")))
            .andExpect(jsonPath("$.data.readerUrl").value("/docs/" + documentId))
            .andExpect(jsonPath("$.data.publicUrl").value("/site/consume-open-site/consume-open-doc"));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}/consume", documentId)
                .header("Authorization", "ApiKey " + plainTextKey)
                .param("view", "source"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.representation").value("source"))
            .andExpect(jsonPath("$.data.mimeType").value("text/markdown"))
            .andExpect(jsonPath("$.data.payload").value("# 开放消费文档\n这是给浏览器和对话框读取的正文。"));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}/consume", documentId)
                .header("Authorization", "ApiKey " + plainTextKey)
                .param("view", "plain"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.representation").value("plain"))
            .andExpect(jsonPath("$.data.mimeType").value("text/plain"))
            .andExpect(jsonPath("$.data.payload").value("开放消费文档 这是给浏览器和对话框读取的正文。"));

        mockMvc.perform(get("/api/v1/open/knowledge-bases/{knowledgeBaseId}/documents/by-source", 1L)
                .header("Authorization", "ApiKey " + plainTextKey)
                .param("sourceExternalId", "browser-clip-001")
                .param("view", "metadata"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentId").value((int) documentId))
            .andExpect(jsonPath("$.data.sourceExternalId").value("browser-clip-001"))
            .andExpect(jsonPath("$.data.sourceRevision").value("2026-05-18T10:30:00Z"))
            .andExpect(jsonPath("$.data.representation").value("metadata"))
            .andExpect(jsonPath("$.data.mimeType").value("application/json"))
            .andExpect(jsonPath("$.data.payload").doesNotExist())
            .andExpect(jsonPath("$.data.publicUrl").value("/site/consume-open-site/consume-open-doc"))
            .andExpect(jsonPath("$.data.readerUrl").value("/docs/" + documentId));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}/consume", documentId)
                .header("Authorization", "ApiKey " + plainTextKey)
                .param("view", "unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("当前 view 仅支持 metadata / rendered / source / plain / summary"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("size", "80"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "OPEN_API_GET_DOCUMENT_BY_SOURCE",
                "OPEN_API_CONSUME_DOCUMENT"
            )))
            .andExpect(jsonPath("$.data.records[*].resultType", Matchers.hasItems("SUCCESS", "FAILURE")));
    }

    @Test
    void shouldEnablePublicSiteAndPublishDocument() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        mockMvc.perform(put("/api/v1/knowledge-bases/1/site")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "siteEnabled": true,
                      "siteSlug": "east-public-site",
                      "siteTitle": "华东公开文档",
                      "siteDescription": "对外交付和实施文档"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.siteEnabled").value(true))
            .andExpect(jsonPath("$.data.siteSlug").value("east-public-site"))
            .andExpect(jsonPath("$.data.siteUrl").value("/site/east-public-site"));

        mockMvc.perform(put("/api/v1/documents/2/publish")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "publicSlug": "project-kickoff-checklist"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.publicSlug").value("project-kickoff-checklist"))
            .andExpect(jsonPath("$.data.renderedHtml", Matchers.containsString("<h1>项目启动清单</h1>")));

        mockMvc.perform(get("/api/public/sites/east-public-site"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.siteTitle").value("华东公开文档"))
            .andExpect(jsonPath("$.data.documents[*].publicSlug", Matchers.hasItem("project-kickoff-checklist")));

        mockMvc.perform(get("/api/public/sites/east-public-site/project-kickoff-checklist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("项目启动清单"))
            .andExpect(jsonPath("$.data.renderedHtml", Matchers.containsString("<h1>项目启动清单</h1>")));

        mockMvc.perform(delete("/api/v1/documents/2/publish")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.publishStatus").value("DRAFT"));

        mockMvc.perform(get("/api/public/sites/east-public-site/project-kickoff-checklist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("公开文档不存在"));
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").value(Matchers.startsWith("session:")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
    }

    private String issueWriteApiKey(String ownerAccessToken, Long knowledgeBaseId, String serviceAccountName, String keyName) throws Exception {
        String issuedKeyResponse = mockMvc.perform(post("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "description": "用于验证 upsert 持续同步语义",
                      "keyName": "%s",
                      "expiresInDays": 180,
                      "scopes": [
                        { "knowledgeBaseId": %d, "accessMode": "WRITE" }
                      ]
                    }
                    """.formatted(serviceAccountName, keyName, knowledgeBaseId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.apiKey.scopes[0].knowledgeBaseId").value(knowledgeBaseId.intValue()))
            .andExpect(jsonPath("$.data.apiKey.scopes[0].accessMode").value("WRITE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(issuedKeyResponse).path("data").path("plainTextKey").asText();
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        String cookiePrefix = cookieName + "=";
        if (setCookieHeader == null || !setCookieHeader.startsWith(cookiePrefix)) {
            return "";
        }
        int delimiterIndex = setCookieHeader.indexOf(';');
        if (delimiterIndex < 0) {
            return setCookieHeader.substring(cookiePrefix.length());
        }
        return setCookieHeader.substring(cookiePrefix.length(), delimiterIndex);
    }
}
