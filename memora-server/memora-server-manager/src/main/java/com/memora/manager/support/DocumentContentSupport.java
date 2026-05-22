package com.memora.manager.support;

import com.memora.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DocumentContentSupport {
    private static final int SUMMARY_LIMIT = 180;
    private static final Pattern BLOCKED_HTML_PATTERN = Pattern.compile("(?is)<(script|style|iframe|object|embed|form|input|button|link|meta)\\b[^>]*>.*?</\\1\\s*>");
    private static final Pattern LINE_BREAK_TAG_PATTERN = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern BLOCK_TAG_PATTERN = Pattern.compile("(?i)</?(p|div|section|article|header|footer|aside|nav|blockquote|ul|ol|li|h[1-6]|pre|table|thead|tbody|tfoot|tr|td|th|hr)\\b[^>]*>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern LEGACY_HTML_DOCUMENT_PATTERN = Pattern.compile("(?is)<\\s*(!doctype|html|body|head|article|section|table|thead|tbody|tfoot|tr|td|th|figure|figcaption)\\b");
    private static final Pattern LEGACY_MARKDOWN_PATTERN = Pattern.compile(
        "(?m)(^#{1,6}\\s+\\S+|^>+\\s+\\S+|^\\s*([-*+]\\s+|\\d+\\.\\s+)\\S+|^```.*$|!\\[[^\\]]*]\\([^)]*\\)|\\[[^\\]]+]\\([^)]*\\)|\\*\\*[^*]+\\*\\*|__[^_]+__|`[^`]+`)"
    );
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\(([^)]*)\\)");
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("(?m)^#{1,6}\\s+");
    private static final Pattern MARKDOWN_BLOCKQUOTE_PATTERN = Pattern.compile("(?m)^>+\\s?");
    private static final Pattern MARKDOWN_LIST_PATTERN = Pattern.compile("(?m)^\\s*([-*+]\\s+|\\d+\\.\\s+)");
    private static final Pattern MARKDOWN_FENCE_PATTERN = Pattern.compile("(?m)^```.*$");
    private static final Pattern MARKDOWN_INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern MARKDOWN_EMPHASIS_PATTERN = Pattern.compile("(\\*\\*|__|~~|\\*|_)");
    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private DocumentContentSupport() {
    }

    public static String normalizeDocType(String rawDocType, String fallbackDocType) {
        String candidate = StringUtils.hasText(rawDocType) ? rawDocType.trim() : fallbackDocType;
        if (!StringUtils.hasText(candidate)) {
            candidate = "DOC";
        }
        String normalized = candidate.toUpperCase(Locale.ROOT);
        if (!"DOC".equals(normalized) && !"FOLDER".equals(normalized)) {
            throw new BusinessException(400, "不支持的节点类型: " + rawDocType);
        }
        return normalized;
    }

    public static NormalizedDocumentContent normalizeDocument(String docType, String rawFormat, String rawContent) {
        String normalizedDocType = normalizeDocType(docType, "DOC");
        if (isFolder(normalizedDocType)) {
            return new NormalizedDocumentContent(null, null, null, null, null);
        }
        DocumentFormat format = DocumentFormat.resolve(rawFormat);
        String contentText = summarizeSearchText(extractPlainText(format, rawContent));
        DocumentRenderSupport.RenderedDocument renderedDocument = DocumentRenderSupport.render(normalizedDocType, format.name(), rawContent);
        return new NormalizedDocumentContent(
            format.name(),
            rawContent,
            contentText,
            renderedDocument.renderedHtml(),
            renderedDocument.renderChecksum()
        );
    }

    public static String resolveSummary(String requestedSummary, String currentSummary, String contentText, boolean contentChanged) {
        String normalizedRequestedSummary = normalizeSummary(requestedSummary);
        if (normalizedRequestedSummary != null) {
            return normalizedRequestedSummary;
        }
        if (!contentChanged) {
            return normalizeSummary(currentSummary);
        }
        return summarizeContent(contentText);
    }

    public static String summarizeContent(String contentText) {
        if (!StringUtils.hasText(contentText)) {
            return null;
        }
        String normalized = summarizeSearchText(contentText);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() > SUMMARY_LIMIT ? normalized.substring(0, SUMMARY_LIMIT) : normalized;
    }

    public static String extractPlainText(String rawFormat, String rawContent) {
        return summarizeSearchText(extractPlainText(DocumentFormat.resolve(rawFormat), rawContent));
    }

    public static String resolveStoredFormat(String rawFormat, String rawContent) {
        if (StringUtils.hasText(rawFormat)) {
            String normalized = rawFormat.trim().toUpperCase(Locale.ROOT);
            for (DocumentFormat value : DocumentFormat.values()) {
                if (value.name().equals(normalized)) {
                    return normalized;
                }
            }
        }
        return inferLegacyFormat(rawContent).name();
    }

    public static NormalizedStoredDocument normalizeStoredDocument(
        String rawDocType,
        String rawFormat,
        String rawContent,
        String rawContentText,
        String rawSummary
    ) {
        String normalizedDocType = normalizeDocType(rawDocType, "DOC");
        if (isFolder(normalizedDocType)) {
            return new NormalizedStoredDocument(normalizedDocType, null, null, null, null);
        }

        String resolvedFormat = resolveStoredFormat(rawFormat, rawContent);
        String derivedContentText = normalizeReturnedContentText(resolvedFormat, extractPlainText(resolvedFormat, rawContent));
        String normalizedContentText = StringUtils.hasText(derivedContentText)
            ? derivedContentText
            : normalizeReturnedContentText(resolvedFormat, rawContentText);
        String normalizedSummary = resolveSummary(rawSummary, rawSummary, normalizedContentText, true);
        return new NormalizedStoredDocument(
            normalizedDocType,
            resolvedFormat,
            rawContent,
            normalizedContentText,
            normalizedSummary
        );
    }

    public static boolean isFolder(String docType) {
        return "FOLDER".equals(docType);
    }

    private static String normalizeReturnedContentText(String rawFormat, String rawContentText) {
        if (!StringUtils.hasText(rawContentText)) {
            return null;
        }
        String normalized = extractPlainText(rawFormat, rawContentText);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return summarizeSearchText(rawContentText);
    }

    private static String extractPlainText(DocumentFormat format, String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return null;
        }
        String normalizedRawContent = normalizeEscapedLineBreaks(rawContent);
        return switch (format) {
            case MARKDOWN -> extractMarkdownPlainText(normalizedRawContent);
            case HTML, RICH_TEXT -> extractHtmlPlainText(normalizedRawContent);
        };
    }

    private static String extractHtmlPlainText(String rawContent) {
        String normalized = BLOCKED_HTML_PATTERN.matcher(rawContent).replaceAll(" ");
        normalized = LINE_BREAK_TAG_PATTERN.matcher(normalized).replaceAll("\n");
        normalized = BLOCK_TAG_PATTERN.matcher(normalized).replaceAll("\n");
        normalized = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ");
        return decodeHtmlEntities(normalized);
    }

    private static String extractMarkdownPlainText(String rawContent) {
        String normalized = rawContent.replace("\r", "");
        normalized = MARKDOWN_IMAGE_PATTERN.matcher(normalized).replaceAll("$1");
        normalized = MARKDOWN_LINK_PATTERN.matcher(normalized).replaceAll("$1");
        normalized = MARKDOWN_INLINE_CODE_PATTERN.matcher(normalized).replaceAll("$1");
        normalized = MARKDOWN_EMPHASIS_PATTERN.matcher(normalized).replaceAll("");
        StringBuilder plainText = new StringBuilder(normalized.length());
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (MARKDOWN_FENCE_PATTERN.matcher(line).matches()) {
                continue;
            }
            String cleanedLine = MARKDOWN_HEADING_PATTERN.matcher(line).replaceFirst("");
            cleanedLine = MARKDOWN_BLOCKQUOTE_PATTERN.matcher(cleanedLine).replaceFirst("");
            cleanedLine = MARKDOWN_LIST_PATTERN.matcher(cleanedLine).replaceFirst("");
            cleanedLine = cleanedLine.replace("|", " ");
            if (plainText.length() > 0) {
                plainText.append('\n');
            }
            plainText.append(cleanedLine);
        }
        return extractHtmlPlainText(plainText.toString());
    }

    private static String decodeHtmlEntities(String rawText) {
        return rawText
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
    }

    private static String normalizeSummary(String rawSummary) {
        if (!StringUtils.hasText(rawSummary)) {
            return null;
        }
        String normalized = compactWhitespace(rawSummary);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private static String summarizeSearchText(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return null;
        }
        String normalized = compactWhitespace(rawText);
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private static String compactWhitespace(String rawText) {
        String normalized = rawText.trim()
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ');
        return MULTI_WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
    }

    private static String normalizeEscapedLineBreaks(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return rawText;
        }
        String normalized = rawText.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.contains("\n") && (normalized.contains("\\n") || normalized.contains("\\r\\n"))) {
            normalized = normalized
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", "\t");
        }
        return normalized;
    }

    private static DocumentFormat inferLegacyFormat(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return DocumentFormat.DEFAULT;
        }
        if (LEGACY_HTML_DOCUMENT_PATTERN.matcher(rawContent).find()) {
            return DocumentFormat.HTML;
        }
        if (LEGACY_MARKDOWN_PATTERN.matcher(rawContent).find() && !HTML_TAG_PATTERN.matcher(rawContent).find()) {
            return DocumentFormat.MARKDOWN;
        }
        return DocumentFormat.DEFAULT;
    }

    public record NormalizedDocumentContent(
        String format,
        String content,
        String contentText,
        String renderedHtml,
        String renderChecksum
    ) {
    }

    public record NormalizedStoredDocument(
        String docType,
        String format,
        String content,
        String contentText,
        String summary
    ) {
    }
}
