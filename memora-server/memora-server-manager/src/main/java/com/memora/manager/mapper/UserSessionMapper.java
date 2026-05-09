package com.memora.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memora.manager.entity.UserSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {
}
