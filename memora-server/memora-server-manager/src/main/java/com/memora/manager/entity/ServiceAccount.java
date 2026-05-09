package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_account")
public class ServiceAccount {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String description;

    private Integer status;

    private Long createdByUserId;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
