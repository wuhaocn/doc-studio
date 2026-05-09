package com.memora.manager.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
public class DocumentVO {
    private Long id;

    private Long tenantId;
    
    private String title;

    private String slug;

    private String docType;

    private String format;
    
    private String content;
    
    private String contentText;

    private String summary;
    
    private Long knowledgeBaseId;
    
    private Long userId;
    
    private Long parentId;

    private String path;

    private Integer depth;

    private Integer versionNo;
    
    private Integer status;

    private Integer viewCount;
    
    private Integer sortOrder;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Long deletedBy;
}
