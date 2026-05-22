package com.memora.manager.support;

import com.memora.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;

public enum DocumentFormat {
    RICH_TEXT,
    MARKDOWN,
    HTML;

    public static final DocumentFormat DEFAULT = RICH_TEXT;

    public static DocumentFormat resolve(String rawFormat) {
        if (!StringUtils.hasText(rawFormat)) {
            return DEFAULT;
        }
        try {
            return valueOf(rawFormat.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "不支持的文档格式: " + rawFormat);
        }
    }
}
