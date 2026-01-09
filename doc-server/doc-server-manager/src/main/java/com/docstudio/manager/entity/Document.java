package com.docstudio.manager.entity;

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
    
    private String title;
    
    private String content; // HTML内容
    
    private String contentText; // 纯文本内容，用于搜索
    
    private Long knowledgeBaseId;
    
    private Long userId;
    
    private Long parentId; // 父文档ID，0表示根目录
    
    private Integer status; // 1:正常 0:删除
    
    private Integer isPublic; // 0:私有 1:公开
    
    private Integer viewCount;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

