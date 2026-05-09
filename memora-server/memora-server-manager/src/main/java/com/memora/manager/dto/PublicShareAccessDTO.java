package com.memora.manager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicShareAccessDTO {
    @Size(max = 32, message = "访问码长度不能超过32个字符")
    private String accessCode;
}
