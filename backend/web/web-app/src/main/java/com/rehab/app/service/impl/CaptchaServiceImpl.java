package com.rehab.app.service.impl;

import com.rehab.app.service.CaptchaService;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.app.vo.CaptchaVO;
import com.wf.captcha.SpecCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码服务实现（EasyCaptcha SpecCaptcha，PNG 静态图）。
 *
 * <p>安全要点：</p>
 * <ul>
 *   <li>验证码文本仅存 Redis（键含一次性 UUID），不返回服务端、不落日志；</li>
 *   <li>固定 2 分钟 TTL，过期自动清理；</li>
 *   <li>登录校验时无论比对结果如何都立即删除键，杜绝同一验证码反复试错；</li>
 *   <li>比对忽略大小写，对用户更友好（EasyCaptcha 字符集本身已剔除 0/O、1/l 等易混字符）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 验证码有效期 */
    private static final Duration EXPIRE = Duration.ofMinutes(2);

    /** 图片尺寸与字符数 */
    private static final int WIDTH = 130;
    private static final int HEIGHT = 38;
    private static final int CODE_LENGTH = 4;

    @Override
    public CaptchaVO generate() {
        // 每次请求独立实例，SpecCaptcha 为即用即弃对象，无线程安全问题
        SpecCaptcha captcha = new SpecCaptcha(WIDTH, HEIGHT, CODE_LENGTH);
        String code = captcha.text().toLowerCase();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.CAPTCHA + captchaId, code, EXPIRE);

        // SpecCaptcha.out 输出 PNG（内部已处理 IO 异常），自行拼装 Data URI，
        // 避免依赖其内部硬编码的 gif 前缀
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        captcha.out(bos);
        String image = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(bos.toByteArray());

        return CaptchaVO.builder()
                .captchaId(captchaId)
                .image(image)
                .expireSeconds(EXPIRE.toSeconds())
                .build();
    }

    @Override
    public void verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(ResultCode.CAPTCHA_INVALID);
        }

        String key = RedisKeyConstants.CAPTCHA + captchaId.trim();
        String cached = stringRedisTemplate.opsForValue().get(key);
        // 先删后比：一次性消费，验证码只能使用一次（成功失败都删）
        stringRedisTemplate.delete(key);

        if (cached == null || !cached.equalsIgnoreCase(captchaCode.trim())) {
            log.debug("图形验证码校验失败 captchaId={}", captchaId);
            throw new BusinessException(ResultCode.CAPTCHA_INVALID);
        }
    }
}
