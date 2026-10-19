package com.rehab.admin.service.file;

import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.model.enums.FilePurpose;

import java.util.Map;
import java.util.Set;

/**
 * 文件上传白名单策略：按 {@link FilePurpose} 约束 MIME 类型、大小上限与对象键前缀。
 *
 * <p>本类承载的是<b>业务规则</b>（哪类用途允许什么文件），不是环境配置；
 * MinIO endpoint/凭证等环境项仍只允许出现在 yml 中。</p>
 *
 * <p>安全说明：首版未接入第三方病毒扫描服务，{@code scan_status} 在哈希校验通过后
 * 直接置 PASSED，依靠 MIME 白名单 + 大小上限 + SHA-256 完整性校验兜底；
 * 后续接入杀毒引擎时改为 PENDING → 异步扫描回写 PASSED/FAILED。</p>
 */
public final class FileUploadPolicy {

    private FileUploadPolicy() {
    }

    /** 单条用途规则：允许的 MIME 集合、大小上限（字节）、MinIO 对象键前缀 */
    private record Rule(Set<String> allowedContentTypes, long maxSizeBytes, String keyPrefix) {
    }

    private static final long MB = 1024L * 1024L;

    private static final Map<FilePurpose, Rule> RULES = Map.of(
            // 头像：仅网页常用图片格式，5MB
            FilePurpose.AVATAR, new Rule(Set.of("image/jpeg", "image/png", "image/webp"), 5 * MB, "avatar/"),
            // 公共内容附件：图片 / GIF / MP4 演示视频 / PDF 文档，100MB
            FilePurpose.ATTACHMENT, new Rule(
                    Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "video/mp4", "application/pdf"),
                    100 * MB, "content/"),
            // 咨询消息附件（M3 预留）：图片 / 短视频，50MB
            FilePurpose.MESSAGE, new Rule(
                    Set.of("image/jpeg", "image/png", "video/mp4"), 50 * MB, "message/"),
            // 动作模板样本视频：MP4 / WebM，200MB
            FilePurpose.TEMPLATE, new Rule(
                    Set.of("video/mp4", "video/webm"), 200 * MB, "action-demo/")
    );

    /**
     * 校验申报的文件类型与大小是否符合用途白名单。
     *
     * @throws BusinessException 超大小 413 FILE_TOO_LARGE；类型不符 415 UNSUPPORTED_MEDIA_TYPE
     */
    public static void validate(FilePurpose purpose, String contentType, long fileSize) {
        Rule rule = RULES.get(purpose);
        // 枚举已约束取值，这里仅作防御性兜底
        if (rule == null) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "不支持的文件用途：" + purpose);
        }
        if (fileSize > rule.maxSizeBytes()) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    String.format("文件大小 %d 字节超过%s用途允许上限 %d 字节",
                            fileSize, purpose.getName(), rule.maxSizeBytes()));
        }
        if (contentType == null || !rule.allowedContentTypes().contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE,
                    String.format("文件类型 %s 不在%s用途允许范围 %s 内",
                            contentType, purpose.getName(), rule.allowedContentTypes()));
        }
    }

    /**
     * 获取用途对应的对象键前缀（如 content/、avatar/）。
     */
    public static String keyPrefixOf(FilePurpose purpose) {
        Rule rule = RULES.get(purpose);
        if (rule == null) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "不支持的文件用途：" + purpose);
        }
        return rule.keyPrefix();
    }
}
