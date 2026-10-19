package com.rehab.app.service;

import com.rehab.app.vo.CaptchaVO;

/**
 * 图形验证码服务（EasyCaptcha + Redis）。
 *
 * <pre>
 * 生成：验证码文本 + UUID(captchaId) 存入 Redis（键 auth:captcha:{captchaId}，带 TTL），
 *       返回 captchaId 与图片 Data URI；
 * 校验：登录时按 captchaId 取出文本，忽略大小写比对，
 *       无论对错立即删除（一次性消费，防止同一验证码被暴力重放）。
 * </pre>
 */
public interface CaptchaService {

    /**
     * 生成图形验证码并存入 Redis。
     *
     * @return captchaId + base64 图片 + 有效期
     */
    CaptchaVO generate();

    /**
     * 校验验证码。
     *
     * @param captchaId 生成接口返回的 UUID
     * @param captchaCode 用户输入的验证码（为空/过期/不匹配均抛业务异常）
     */
    void verify(String captchaId, String captchaCode);
}
