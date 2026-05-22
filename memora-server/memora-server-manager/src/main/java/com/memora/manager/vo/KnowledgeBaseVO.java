package com.memora.manager.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
@Data
public class KnowledgeBaseVO {
    private Long id;

    private Long tenantId;
    
    private String name;

    private String slug;
    
    private String description;
    
    private String cover;
    
    private Long userId;
    
    private Integer status;
    
    private Integer documentCount;
    
    private Integer viewCount;
    
    private Integer sortOrder;

    private Boolean siteEnabled;

    private String siteSlug;

    private String siteTitle;

    private String siteDescription;

    private String siteUrl;

    private String currentRole;

    private Boolean canWrite;

    private Boolean canManage;

    private Boolean permissionRestricted;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Long deletedBy;
}
