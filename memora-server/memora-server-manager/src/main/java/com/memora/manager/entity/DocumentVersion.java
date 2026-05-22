package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档版本实体
 */
@Data
@TableName("document_version")
public class DocumentVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 文档标题
     */
    private String title;

    private String format;
    
    /**
     * 原始正文内容，实际解释方式由 format 决定
     */
    private String content;
    
    /**
     * 纯文本内容
     */
    private String contentText;
    
    /**
     * 创建人ID
     */
    private Long userId;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
