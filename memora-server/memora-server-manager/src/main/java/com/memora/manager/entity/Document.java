package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    
    private String title;

    private String slug;

    private String docType;

    private String format;

    private String content; // 原始正文内容，实际解释方式由 format 决定

    private String contentText; // 服务端归一后的纯文本内容，用于搜索与摘要

    private String summary;
    
    private Long knowledgeBaseId;
    
    private Long userId;
    
    private Long parentId; // 父文档ID，0表示根目录

    private String path;

    private Integer depth;

    private Integer versionNo;
    
    private Integer status; // 1:正常 0:删除
    
    private Integer viewCount;
    
    private Integer sortOrder;

    private String publishStatus;

    private String publicSlug;

    private LocalDateTime publishedAt;

    private String renderedHtml;

    private String renderChecksum;

    private String sourceExternalId;

    private String sourceRevision;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Long deletedBy;
}
