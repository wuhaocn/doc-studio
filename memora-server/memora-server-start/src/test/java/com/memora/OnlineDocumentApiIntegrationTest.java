package com.memora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                      "contentText": "巡检手册包含值班检查、升级回退和交接处理。"
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
                      "contentText": "版本一正文"
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
                      "content": "# 审计文档\\n版本二正文",
                      "contentText": "审计文档 版本二正文"
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
                      "contentText": "首次创建正文"
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
                      "content": "# admin登录新建文档\\n已完成正文编辑",
                      "contentText": "admin登录新建文档 已完成正文编辑",
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
                      "contentText": "版本一正文"
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
                      "content": "# admin链路验证文档\\n版本二正文",
                      "contentText": "admin链路验证文档 版本二正文",
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
                      "contentText": "用于验证版本语义收口"
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
                      "contentText": "用于记录交接事项"
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
                      "contentText": "验证文档创建主体始终取当前会话",
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
                      "contentText": "验证文档路径唯一键冲突错误语义"
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
                      "contentText": "验证文档回收站与恢复"
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
                      "contentText": "验证恢复父级约束"
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
                      "contentText": "用于测试批量移动"
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
                      "contentText": "用于测试批量删除"
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
                      "contentText": "viewer should be rejected"
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
                      "contentText": "reviewer with kb editor should pass"
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
                      "contentText": "viewer should be rejected by kb role"
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
            .andExpect(jsonPath("$.data.title").value("项目启动清单"));

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
    void shouldManageApiKeysAndOperateDocumentsThroughOpenApi() throws Exception {
        String ownerAccessToken = loginAndGetAccessToken("admin", "123456");

        String issuedKeyResponse = mockMvc.perform(post("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "ERP 同步写入",
                      "description": "用于外部 ERP 系统写入文档",
                      "keyName": "生产写入 key",
                      "expiresInDays": 180,
                      "accessMode": "WRITE",
                      "knowledgeBaseIds": [1]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.apiKey.accessModes[0]").value("WRITE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode issuedNode = objectMapper.readTree(issuedKeyResponse).path("data");
        long apiKeyId = issuedNode.path("apiKey").path("id").asLong();
        String plainTextKey = issuedNode.path("plainTextKey").asText();

        mockMvc.perform(get("/api/v1/service-accounts")
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].name").value("ERP 同步写入"))
            .andExpect(jsonPath("$.data[0].apiKeys[0].knowledgeBaseIds[0]").value(1));

        mockMvc.perform(get("/api/v1/open/knowledge-bases")
                .header("Authorization", "ApiKey " + plainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[*].id", Matchers.hasItem(1)))
            .andExpect(jsonPath("$.data[*].currentRole", Matchers.hasItem("API_KEY_WRITE")));

        String openDocumentResponse = mockMvc.perform(post("/api/v1/open/documents")
                .header("Authorization", "ApiKey " + plainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "knowledgeBaseId": 1,
                      "parentId": 1,
                      "title": "Open API 写入文档",
                      "contentText": "Open API 第一次写入"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long documentId = objectMapper.readTree(openDocumentResponse).path("data").path("id").asLong();

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
                      "content": "# Open API 写入文档\\n第二版",
                      "contentText": "Open API 写入文档 第二版"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionNo").value(2));

        mockMvc.perform(put("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "expectedVersionNo": 1,
                      "contentText": "过期版本写入"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value("文档版本已变化，请基于最新 versionNo 重试"));

        mockMvc.perform(post("/api/v1/api-keys/{apiKeyId}/disable", rotatedApiKeyId)
                .header("Authorization", "Bearer " + ownerAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/open/documents/{documentId}", documentId)
                .header("Authorization", "ApiKey " + rotatedPlainTextKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("当前 API key 已禁用"));

        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + ownerAccessToken)
                .param("objectType", "DOCUMENT")
                .param("objectId", String.valueOf(documentId))
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[*].actionType", Matchers.hasItems(
                "OPEN_API_CREATE_DOCUMENT",
                "OPEN_API_UPDATE_DOCUMENT"
            )))
            .andExpect(jsonPath("$.data.records[*].resultType", Matchers.hasItems("SUCCESS", "FAILURE")));
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
