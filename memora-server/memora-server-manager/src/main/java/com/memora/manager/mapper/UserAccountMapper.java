package com.memora.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memora.manager.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
