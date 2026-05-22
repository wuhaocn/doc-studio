package com.memora.manager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseSiteUpdateDTO {
    private Boolean siteEnabled;

    @Size(max = 120, message = "站点标识长度不能超过120个字符")
    private String siteSlug;

    @Size(max = 120, message = "站点标题长度不能超过120个字符")
    private String siteTitle;

    @Size(max = 500, message = "站点描述长度不能超过500个字符")
    private String siteDescription;
}

