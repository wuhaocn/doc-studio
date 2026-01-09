package com.docstudio.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docstudio.manager.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}

