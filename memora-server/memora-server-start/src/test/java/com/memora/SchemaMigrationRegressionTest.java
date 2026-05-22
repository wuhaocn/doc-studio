package com.memora;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

class SchemaMigrationRegressionTest {

    @Test
    void shouldApplySchemaOnLegacyKnowledgeBaseAndDocumentTables() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:schema_regression_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
            CREATE TABLE tenant (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              name VARCHAR(120) NOT NULL,
              slug VARCHAR(120) NOT NULL,
              industry VARCHAR(120),
              plan_name VARCHAR(60),
              owner_user_id BIGINT NOT NULL,
              status TINYINT DEFAULT 1,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE knowledge_base (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              tenant_id BIGINT NOT NULL,
              name VARCHAR(100) NOT NULL,
              slug VARCHAR(120) NOT NULL,
              description VARCHAR(500),
              cover VARCHAR(255),
              user_id BIGINT NOT NULL,
              status TINYINT DEFAULT 1,
              document_count INT DEFAULT 0,
              view_count INT DEFAULT 0,
              sort_order INT DEFAULT 0,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              deleted_at TIMESTAMP NULL,
              deleted_by BIGINT,
              FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE document (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              knowledge_base_id BIGINT NOT NULL,
              tenant_id BIGINT NOT NULL,
              parent_id BIGINT,
              path VARCHAR(500) NOT NULL,
              title VARCHAR(200) NOT NULL,
              summary VARCHAR(500),
              format VARCHAR(30) DEFAULT 'MARKDOWN',
              content CLOB,
              content_text CLOB,
              user_id BIGINT NOT NULL,
              version_no INT DEFAULT 1,
              status TINYINT DEFAULT 1,
              view_count INT DEFAULT 0,
              sort_order INT DEFAULT 0,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              deleted_at TIMESTAMP NULL,
              deleted_by BIGINT,
              FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
              FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
            )
            """);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            false,
            false,
            StandardCharsets.UTF_8.name(),
            new ClassPathResource("db/schema.sql")
        );
        DatabasePopulatorUtils.execute(populator, dataSource);

        jdbcTemplate.execute("SELECT site_slug, site_title, site_description FROM knowledge_base");
        jdbcTemplate.execute("""
            SELECT publish_status, public_slug, published_at, rendered_html, render_checksum,
                   source_external_id, source_revision
            FROM document
            """);
    }
}
