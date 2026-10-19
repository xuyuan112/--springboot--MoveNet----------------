package com.rehab.app.service;

import com.rehab.app.vo.LoginLogVO;
import com.rehab.common.result.PageResult;

/**
 * 本人登录日志查询服务（三端共用）。
 */
public interface UserLoginLogService {

    /**
     * 分页查询当前登录用户的登录/登出日志。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<LoginLogVO> pageMyLoginLogs(Integer page, Integer size);
}
