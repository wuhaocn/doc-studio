package com.docstudio.manager.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
public class DocumentVO {
    private Long id;
    
    private String title;
    
    private String content;
    
    private String contentText;
    
    private Long knowledgeBaseId;
    
    private Long userId;
    
    private Long parentId;
    
    private Integer status;
    
    private Integer isPublic;
    
    private Integer viewCount;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

