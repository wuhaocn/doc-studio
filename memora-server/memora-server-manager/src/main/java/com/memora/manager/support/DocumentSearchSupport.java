package com.memora.manager.support;

import com.memora.manager.entity.Document;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DocumentSearchSupport {
    private DocumentSearchSupport() {
    }

    public static List<String> tokenizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        String normalizedKeyword = normalizeText(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            tokens.add(normalizedKeyword);
        }
        for (String part : keyword.trim().split("\\s+")) {
            String normalizedPart = normalizeText(part);
            if (StringUtils.hasText(normalizedPart)) {
                tokens.add(normalizedPart);
            }
        }
        return tokens.stream().toList();
    }

    public static boolean matches(Document document, String keyword) {
        List<String> tokens = tokenizeKeyword(keyword);
        if (tokens.isEmpty()) {
            return true;
        }
        String searchableText = buildSearchableText(document);
        return tokens.stream().allMatch(searchableText::contains);
    }

    public static int calculateScore(Document document, String keyword) {
        List<String> tokens = tokenizeKeyword(keyword);
        if (tokens.isEmpty()) {
            return 0;
        }

        String normalizedKeyword = tokens.get(0);
        String title = normalizeText(document == null ? null : document.getTitle());
        String summary = normalizeText(document == null ? null : document.getSummary());
        String content = normalizeText(document == null ? null : document.getContentText());
        String path = normalizeText(document == null ? null : document.getPath());
        int score = 0;

        if (title.equals(normalizedKeyword)) {
            score += 240;
        } else if (title.startsWith(normalizedKeyword)) {
            score += 180;
        } else if (title.contains(normalizedKeyword)) {
            score += 120;
        }

        if (content.contains(normalizedKeyword)) {
            score += 44;
        }
        if (summary.contains(normalizedKeyword)) {
            score += 24;
        }
        if (path.contains(normalizedKeyword)) {
            score += 18;
        }

        int tokenHits = 0;
        for (String token : tokens) {
            boolean tokenMatched = false;
            if (title.equals(token)) {
                score += 90;
                tokenMatched = true;
            } else if (title.startsWith(token)) {
                score += 56;
                tokenMatched = true;
            } else if (title.contains(token)) {
                score += 28;
                tokenMatched = true;
            }

            if (summary.contains(token)) {
                score += 12;
                tokenMatched = true;
            }
            if (content.contains(token)) {
                score += 10;
                tokenMatched = true;
            }
            if (path.contains(token)) {
                score += 6;
                tokenMatched = true;
            }
            if (tokenMatched) {
                tokenHits++;
            }
        }

        if (tokenHits == tokens.size()) {
            score += 20;
        }
        return score;
    }

    public static Comparator<Document> relevanceComparator(String keyword) {
        return Comparator
            .comparingInt((Document document) -> calculateScore(document, keyword))
            .reversed()
            .thenComparing(Document::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Document::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static String buildSearchableText(Document document) {
        return normalizeText(String.join(" ",
            document == null ? "" : safeText(document.getTitle()),
            document == null ? "" : safeText(document.getSummary()),
            document == null ? "" : safeText(document.getContentText()),
            document == null ? "" : safeText(document.getPath())
        ));
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}
