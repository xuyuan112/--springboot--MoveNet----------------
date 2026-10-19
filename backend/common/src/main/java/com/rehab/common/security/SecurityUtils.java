package com.rehab.common.security;

import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户。
 * 业务代码严禁信任前端传入的 patientId / therapistId，一律以本工具解析结果为准。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录抛 401 业务异常。
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.AUTH_REQUIRED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ResultCode.AUTH_REQUIRED);
    }

    /**
     * 获取当前用户 ID（未登录抛 401）
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 获取当前用户角色编码
     */
    public static String getRoleCode() {
        return getLoginUser().getRoleCode();
    }

    /**
     * 当前是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser;
    }
}
