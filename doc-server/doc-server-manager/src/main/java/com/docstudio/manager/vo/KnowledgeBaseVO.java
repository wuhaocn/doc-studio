package com.docstudio.manager.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
@Data
public class KnowledgeBaseVO {
    private Long id;
    
    private String name;
    
    private String description;
    
    private String cover;
    
    private Long userId;
    
    private Integer status;
    
    private Integer isPublic;
    
    private Integer documentCount;
    
    private Integer viewCount;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

