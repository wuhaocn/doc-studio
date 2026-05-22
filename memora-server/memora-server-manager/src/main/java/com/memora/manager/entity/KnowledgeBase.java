package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库实体
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    
    private String name;

    private String slug;
    
    private String description;
    
    private String cover;
    
    private Long userId;
    
    private Integer status; // 1:正常 0:删除 2:归档
    
    private Integer documentCount;
    
    private Integer viewCount;
    
    private Integer sortOrder;

    private Integer siteEnabled;

    private String siteSlug;

    private String siteTitle;

    private String siteDescription;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Long deletedBy;
}
