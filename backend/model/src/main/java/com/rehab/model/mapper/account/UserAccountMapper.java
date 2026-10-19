package com.rehab.model.mapper.account;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * user_account 数据访问层。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}