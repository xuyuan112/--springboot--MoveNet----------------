package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 文件用途：对应 file_object.purpose 列。
 */
@Getter
@Schema(description = "文件用途")
public enum FilePurpose implements BaseEnum<String> {
    /** 动作模板样本 */
    TEMPLATE("TEMPLATE", "动作模板样本"),
    /** 业务附件 */
    ATTACHMENT("ATTACHMENT", "业务附件"),
    /** 咨询消息 */
    MESSAGE("MESSAGE", "咨询消息"),
    /** 用户头像 */
    AVATAR("AVATAR", "用户头像");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    FilePurpose(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据码值解析枚举（同时作为 Jackson 反序列化入口）。
     *
     * @param code 枚举码值
     * @return 对应的枚举值
     * @throws IllegalArgumentException 找不到对应码值时抛出
     */
    @JsonCreator
    public static FilePurpose fromCode(String code) {
        for (FilePurpose value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的文件用途码值：" + code);
    }
}
