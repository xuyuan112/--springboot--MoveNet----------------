package com.rehab.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.AdminUserCreateRequest;
import com.rehab.model.dto.AdminUserUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.model.entity.UserAccount;
import com.rehab.admin.vo.AdminUserCreateVO;
import com.rehab.admin.vo.AdminUserDetailVO;
import com.rehab.admin.vo.AdminUserVO;
import com.rehab.admin.vo.LoginLogVO;

/**
 * 管理端 - 账号服务：患者/治疗师账号的创建、查询、启停、重置密码与登录日志。
 * 仅 ADMIN 角色可调用（Controller 路径 /admin/** 已由 SecurityConfig 统一拦截）。
 *
 * <p>继承 {@link IService} 获得 user_account 单表通用 CRUD；
 * 患者档案/治疗师档案/审计日志作为协作 Mapper 在实现类中注入，不在此暴露。</p>
 */
public interface AdminUserService extends IService<UserAccount> {

    /**
     * 分页查询账号。
     *
     * @param roleCode 角色筛选（可空）
     * @param status   状态筛选（1 启用 / 0 禁用，可空）
     * @param keyword  登录名/姓名/手机号模糊匹配（可空）
     */
    PageResult<AdminUserVO> pageUsers(Integer page, Integer size,
                                      String roleCode, Integer status, String keyword);

    /**
     * 创建患者/治疗师账号并同步建立角色档案（同事务）。
     * 初始密码使用平台固定初始密码并落 BCrypt 密文，不通过接口回显明文。
     */
    AdminUserCreateVO createUser(AdminUserCreateRequest request);

    /** 查询账号及角色档案详情。 */
    AdminUserDetailVO detail(Long userId);

    /** 修改非敏感账号资料（用户名/角色/密码不在此修改）。 */
    void updateUser(Long userId, AdminUserUpdateRequest request);

    /** 启用账号。 */
    void enable(Long userId);

    /** 禁用账号：更新状态、删除 Redis 会话使令牌立即失效，原因写入审计。 */
    void disable(Long userId, ReasonRequest reason);

    /**
     * 重置密码为平台初始密码并使现有会话失效；不返回明文密码。
     */
    void resetPassword(Long userId);

    /**
     * 分页查询某账号的登录/登出日志（operation_log，module='AUTH'）。
     *
     * @param success true=仅成功 / false=仅失败 / null=全部
     */
    PageResult<LoginLogVO> pageLoginLogs(Long userId, Integer page, Integer size, Boolean success);
}
