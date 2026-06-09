package com.memora.manager.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentContentSupportTest {
    @Test
    void shouldNormalizeMarkdownStoredDocumentIntoPlainText() {
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            "DOC",
            "MARKDOWN",
            "# 项目启动清单\n- 确认客户信息\n- 确认硬件版本",
            "项目启动清单 确认客户信息 确认硬件版本",
            "适用于制造项目实施启动阶段的标准清单。"
        );

        assertThat(normalized.format()).isEqualTo("MARKDOWN");
        assertThat(normalized.contentText()).isEqualTo("项目启动清单 确认客户信息 确认硬件版本");
        assertThat(normalized.summary()).isEqualTo("适用于制造项目实施启动阶段的标准清单。");
    }

    @Test
    void shouldRenderMarkdownIntoCanonicalHtml() {
        DocumentContentSupport.NormalizedDocumentContent normalized = DocumentContentSupport.normalizeDocument(
            "DOC",
            "MARKDOWN",
            "# 发布说明\n\n- 第一项\n- 第二项"
        );

        assertThat(normalized.renderedHtml()).contains("<h1>发布说明</h1>");
        assertThat(normalized.renderedHtml()).contains("<ul>");
        assertThat(normalized.renderedHtml()).contains("<li>第一项</li>");
        assertThat(normalized.renderChecksum()).isNotBlank();
    }

    @Test
    void shouldSanitizeBlockedMarkupWhenRenderingHtml() {
        DocumentContentSupport.NormalizedDocumentContent normalized = DocumentContentSupport.normalizeDocument(
            "DOC",
            "HTML",
            "<article><h1>标题</h1><script>alert(1)</script><p onclick=\"x()\">正文</p></article>"
        );

        assertThat(normalized.renderedHtml()).contains("<h1>标题</h1>");
        assertThat(normalized.renderedHtml()).contains("<p>正文</p>");
        assertThat(normalized.renderedHtml()).doesNotContain("script");
        assertThat(normalized.renderedHtml()).doesNotContain("onclick");
    }
}
