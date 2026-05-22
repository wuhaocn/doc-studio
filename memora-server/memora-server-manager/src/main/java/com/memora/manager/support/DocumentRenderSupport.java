package com.memora.manager.support;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocumentRenderSupport {
    private static final Set<String> ALLOWED_HTML_TAGS = Set.of(
        "a", "blockquote", "br", "code", "del", "div", "em",
        "h1", "h2", "h3", "h4", "h5", "h6", "hr", "img", "li", "ol", "p", "pre",
        "span", "strong", "sub", "sup", "table", "tbody", "td", "th", "thead", "tr", "ul"
    );
    private static final Set<String> BLOCKED_HTML_TAGS = Set.of(
        "script", "style", "iframe", "object", "embed", "form", "input", "button", "link", "meta"
    );
    private static final Map<String, Set<String>> TAG_ALLOWED_ATTRS = Map.of(
        "a", Set.of("href", "title", "target", "rel"),
        "img", Set.of("src", "alt", "title")
    );
    private static final Set<String> SELF_CLOSING_HTML_TAGS = Set.of("br", "hr", "img");
    private static final Pattern CODE_SPAN_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\(([^)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern STRIKE_PATTERN = Pattern.compile("~~([^~]+)~~");
    private static final Pattern BOLD_PATTERN = Pattern.compile("(\\*\\*|__)(.+?)\\1");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)|(?<!_)_([^_]+)_(?!_)");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s+(.*)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^\\s*[-*+]\\s+(.*)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^\\s*>\\s?(.*)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$");

    private DocumentRenderSupport() {
    }

    public static RenderedDocument render(String docType, String format, String content) {
        if (DocumentContentSupport.isFolder(docType)) {
            return new RenderedDocument(null, null);
        }
        String normalizedContent = normalizeEscapedLineBreaks(content);
        if (normalizedContent == null || normalizedContent.isBlank()) {
            return new RenderedDocument(null, null);
        }

        String renderedHtml = switch (DocumentFormat.resolve(format)) {
            case MARKDOWN -> sanitizeHtml(renderMarkdown(normalizedContent));
            case HTML, RICH_TEXT -> sanitizeHtml(normalizedContent);
        };
        if (renderedHtml == null || renderedHtml.isBlank()) {
            return new RenderedDocument(null, null);
        }
        return new RenderedDocument(renderedHtml, checksum(renderedHtml));
    }

    public static String resolveStoredRenderedHtml(String docType, String format, String content, String storedRenderedHtml) {
        if (DocumentContentSupport.isFolder(docType)) {
            return null;
        }
        if (storedRenderedHtml != null && !storedRenderedHtml.isBlank()) {
            return storedRenderedHtml;
        }
        return render(docType, format, content).renderedHtml();
    }

    public static String checksum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte current : hashed) {
                builder.append(Character.forDigit((current >> 4) & 0xF, 16));
                builder.append(Character.forDigit(current & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String renderMarkdown(String markdown) {
        List<String> lines = List.of(markdown.replace("\r", "").split("\n", -1));
        StringBuilder html = new StringBuilder(markdown.length() + 64);
        List<String> paragraphLines = new ArrayList<>();
        List<String> blockquoteLines = new ArrayList<>();
        List<String> codeLines = new ArrayList<>();
        ListContext listContext = ListContext.none();

        boolean inCodeBlock = false;
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();

            if (inCodeBlock) {
                if (trimmed.startsWith("```")) {
                    appendCodeBlock(html, codeLines);
                    codeLines.clear();
                    inCodeBlock = false;
                } else {
                    codeLines.add(line);
                }
                continue;
            }

            if (trimmed.startsWith("```")) {
                flushParagraph(html, paragraphLines);
                flushBlockquote(html, blockquoteLines);
                listContext = flushList(html, listContext);
                inCodeBlock = true;
                continue;
            }

            if (trimmed.isEmpty()) {
                flushParagraph(html, paragraphLines);
                flushBlockquote(html, blockquoteLines);
                listContext = flushList(html, listContext);
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            if (headingMatcher.matches()) {
                flushParagraph(html, paragraphLines);
                flushBlockquote(html, blockquoteLines);
                listContext = flushList(html, listContext);
                int level = headingMatcher.group(1).length();
                html.append("<h").append(level).append(">")
                    .append(renderInlineMarkdown(headingMatcher.group(2).trim()))
                    .append("</h").append(level).append(">");
                continue;
            }

            if (HORIZONTAL_RULE_PATTERN.matcher(trimmed).matches()) {
                flushParagraph(html, paragraphLines);
                flushBlockquote(html, blockquoteLines);
                listContext = flushList(html, listContext);
                html.append("<hr>");
                continue;
            }

            Matcher blockquoteMatcher = BLOCKQUOTE_PATTERN.matcher(line);
            if (blockquoteMatcher.matches()) {
                flushParagraph(html, paragraphLines);
                listContext = flushList(html, listContext);
                blockquoteLines.add(blockquoteMatcher.group(1));
                continue;
            }

            Matcher orderedMatcher = ORDERED_LIST_PATTERN.matcher(line);
            if (orderedMatcher.matches()) {
                flushParagraph(html, paragraphLines);
                flushBlockquote(html, blockquoteLines);
                listContext = ensureList(html, listContext, true);
                html.append("<li>").append(renderInlineMarkdown(orderedMatcher.group(1).trim())).append("</li>");
                continue;
            }

            Matcher unorderedMatcher = UNORDERED_LIST_PATTERN.matcher(line);
            if (unorderedMatcher.matches()) {
                flushParagraph(html, paragraphLines);
                flushBlockquote(html, blockquoteLines);
                listContext = ensureList(html, listContext, false);
                html.append("<li>").append(renderInlineMarkdown(unorderedMatcher.group(1).trim())).append("</li>");
                continue;
            }

            flushBlockquote(html, blockquoteLines);
            listContext = flushList(html, listContext);
            paragraphLines.add(line.trim());
        }

        if (inCodeBlock) {
            appendCodeBlock(html, codeLines);
        }
        flushParagraph(html, paragraphLines);
        flushBlockquote(html, blockquoteLines);
        flushList(html, listContext);
        return html.toString();
    }

    private static ListContext ensureList(StringBuilder html, ListContext current, boolean ordered) {
        if (current.active && current.ordered == ordered) {
            return current;
        }
        ListContext next = flushList(html, current);
        html.append(ordered ? "<ol>" : "<ul>");
        return new ListContext(true, ordered);
    }

    private static ListContext flushList(StringBuilder html, ListContext current) {
        if (!current.active) {
            return current;
        }
        html.append(current.ordered ? "</ol>" : "</ul>");
        return ListContext.none();
    }

    private static void flushParagraph(StringBuilder html, List<String> paragraphLines) {
        if (paragraphLines.isEmpty()) {
            return;
        }
        html.append("<p>").append(renderInlineMarkdown(String.join(" ", paragraphLines))).append("</p>");
        paragraphLines.clear();
    }

    private static void flushBlockquote(StringBuilder html, List<String> blockquoteLines) {
        if (blockquoteLines.isEmpty()) {
            return;
        }
        String renderedLines = blockquoteLines.stream()
            .map(String::trim)
            .map(DocumentRenderSupport::renderInlineMarkdown)
            .reduce((left, right) -> left + "<br>" + right)
            .orElse("");
        html.append("<blockquote><p>")
            .append(renderedLines)
            .append("</p></blockquote>");
        blockquoteLines.clear();
    }

    private static void appendCodeBlock(StringBuilder html, List<String> codeLines) {
        html.append("<pre><code>")
            .append(escapeHtml(String.join("\n", codeLines)))
            .append("</code></pre>");
    }

    private static String renderInlineMarkdown(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        List<String> placeholders = new ArrayList<>();
        String working = replaceWithPlaceholders(value, CODE_SPAN_PATTERN, placeholders, matcher ->
            "<code>" + escapeHtml(matcher.group(1)) + "</code>");
        working = replaceWithPlaceholders(working, IMAGE_PATTERN, placeholders, matcher -> {
            String alt = escapeHtml(matcher.group(1).trim());
            String url = matcher.group(2).trim();
            if (!isSafeUrl(url, true)) {
                return alt;
            }
            return "<img src=\"" + escapeHtmlAttribute(url) + "\" alt=\"" + alt + "\">";
        });
        working = replaceWithPlaceholders(working, LINK_PATTERN, placeholders, matcher -> {
            String text = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            if (!isSafeUrl(url, false)) {
                return escapeHtml(text);
            }
            return "<a href=\"" + escapeHtmlAttribute(url) + "\" target=\"_blank\" rel=\"noopener noreferrer\">"
                + escapeHtml(text)
                + "</a>";
        });
        String escaped = escapeHtml(working);

        escaped = replaceAll(escaped, STRIKE_PATTERN, "<del>$1</del>");
        escaped = replaceAll(escaped, BOLD_PATTERN, "<strong>$2</strong>");
        escaped = replaceItalic(escaped);
        return restorePlaceholders(escaped, placeholders);
    }

    private static String sanitizeHtml(String rawHtml) {
        String normalizedHtml = normalizeEscapedLineBreaks(rawHtml);
        if (normalizedHtml == null || normalizedHtml.isBlank()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(normalizedHtml.length() + 32);
        Deque<String> blockedTags = new ArrayDeque<>();
        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            @Override
            public void handleText(char[] data, int pos) {
                if (!blockedTags.isEmpty()) {
                    return;
                }
                sanitized.append(escapeHtml(new String(data)));
            }

            @Override
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int pos) {
                appendTag(tag, attributes, false);
            }

            @Override
            public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int pos) {
                appendTag(tag, attributes, true);
            }

            @Override
            public void handleEndTag(HTML.Tag tag, int pos) {
                String tagName = normalizeTagName(tag);
                if (BLOCKED_HTML_TAGS.contains(tagName)) {
                    if (!blockedTags.isEmpty() && blockedTags.peek().equals(tagName)) {
                        blockedTags.pop();
                    }
                    return;
                }
                if (!blockedTags.isEmpty() || !ALLOWED_HTML_TAGS.contains(tagName) || SELF_CLOSING_HTML_TAGS.contains(tagName)) {
                    return;
                }
                sanitized.append("</").append(tagName).append(">");
            }

            private void appendTag(HTML.Tag tag, MutableAttributeSet attributes, boolean simpleTag) {
                String tagName = normalizeTagName(tag);
                if (BLOCKED_HTML_TAGS.contains(tagName)) {
                    if (!simpleTag) {
                        blockedTags.push(tagName);
                    }
                    return;
                }
                if (!blockedTags.isEmpty() || !ALLOWED_HTML_TAGS.contains(tagName)) {
                    return;
                }

                sanitized.append("<").append(tagName);
                appendAllowedAttributes(sanitized, tagName, attributes);
                if (simpleTag || SELF_CLOSING_HTML_TAGS.contains(tagName)) {
                    sanitized.append(">");
                    return;
                }
                sanitized.append(">");
            }
        };

        try {
            new ParserDelegator().parse(new StringReader(normalizedHtml), callback, true);
        } catch (IOException ex) {
            return escapeHtml(normalizedHtml);
        }
        return sanitized.toString();
    }

    private static void appendAllowedAttributes(StringBuilder target, String tagName, MutableAttributeSet attributes) {
        if (attributes == null) {
            return;
        }
        Set<String> allowedAttrs = TAG_ALLOWED_ATTRS.getOrDefault(tagName, Set.of());
        Set<String> emitted = new HashSet<>();
        Map<String, String> values = new HashMap<>();

        var attributeNames = attributes.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            Object key = attributeNames.nextElement();
            if (!(key instanceof HTML.Attribute attribute)) {
                continue;
            }
            String attrName = attribute.toString().toLowerCase(Locale.ROOT);
            if (!allowedAttrs.contains(attrName)) {
                continue;
            }
            Object rawValue = attributes.getAttribute(attribute);
            if (rawValue == null) {
                continue;
            }
            String attrValue = String.valueOf(rawValue).trim();
            if (attrValue.isEmpty()) {
                continue;
            }
            if (("href".equals(attrName) && !isSafeUrl(attrValue, false))
                || ("src".equals(attrName) && !isSafeUrl(attrValue, true))) {
                continue;
            }
            values.put(attrName, attrValue);
            emitted.add(attrName);
        }

        emitted.stream().sorted().forEach(attrName -> {
            String value = values.get(attrName);
            if (value == null) {
                return;
            }
            target.append(" ").append(attrName).append("=\"").append(escapeHtmlAttribute(value)).append("\"");
        });

        if ("a".equals(tagName) && emitted.contains("href")) {
            target.append(" target=\"_blank\" rel=\"noopener noreferrer\"");
        }
    }

    private static String replaceItalic(String value) {
        Matcher matcher = ITALIC_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String content = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(builder, Matcher.quoteReplacement("<em>" + content + "</em>"));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String replaceWithPlaceholders(String value, Pattern pattern, List<String> placeholders, PlaceholderResolver resolver) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String placeholder = "\u0000MEMORA_" + placeholders.size() + "\u0000";
            placeholders.add(resolver.resolve(matcher));
            matcher.appendReplacement(builder, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String restorePlaceholders(String value, List<String> placeholders) {
        String restored = value;
        for (int i = 0; i < placeholders.size(); i++) {
            restored = restored.replace("\u0000MEMORA_" + i + "\u0000", placeholders.get(i));
        }
        return restored;
    }

    private static String replaceAll(String value, Pattern pattern, String replacement) {
        return pattern.matcher(value).replaceAll(replacement);
    }

    private static String normalizeEscapedLineBreaks(String rawText) {
        if (rawText == null || rawText.isBlank()) {
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

    private static boolean isSafeUrl(String url, boolean allowDataImage) {
        String normalized = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith("javascript:") || normalized.startsWith("vbscript:")) {
            return false;
        }
        if (normalized.startsWith("data:")) {
            return allowDataImage && normalized.startsWith("data:image/");
        }
        return true;
    }

    private static String normalizeTagName(HTML.Tag tag) {
        return tag == null ? "" : tag.toString().toLowerCase(Locale.ROOT);
    }

    private static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static String escapeHtmlAttribute(String value) {
        return escapeHtml(value);
    }

    public record RenderedDocument(String renderedHtml, String renderChecksum) {
    }

    private record ListContext(boolean active, boolean ordered) {
        private static ListContext none() {
            return new ListContext(false, false);
        }
    }

    @FunctionalInterface
    private interface PlaceholderResolver {
        String resolve(Matcher matcher);
    }
}
