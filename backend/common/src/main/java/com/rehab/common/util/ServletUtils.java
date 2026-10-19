package com.rehab.common.util;

import com.rehab.common.constant.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求上下文工具：在非 Controller 层（切面/事件监听器）获取当前请求。
 */
public final class ServletUtils {

    private ServletUtils() {
    }

    /**
     * 获取当前 HttpServletRequest，非 Web 线程返回 null。
     */
    public static HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 获取客户端 IP（无请求上下文时返回 unknown）。
     */
    public static String currentIp() {
        HttpServletRequest request = getRequest();
        return request == null ? "unknown" : IpUtils.getClientIp(request);
    }

    /**
     * 获取设备标识（请求头 X-Device-Id）。
     */
    public static String currentDeviceId() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeader(Constants.DEVICE_ID_HEADER);
    }
}
