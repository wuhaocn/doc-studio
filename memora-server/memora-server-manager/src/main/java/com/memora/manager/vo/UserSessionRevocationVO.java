package com.memora.manager.vo;

import lombok.Data;

@Data
public class UserSessionRevocationVO {
    private Long revokedCount;

    private Boolean currentSessionRevoked;
}
