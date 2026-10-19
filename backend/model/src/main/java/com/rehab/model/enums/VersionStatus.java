package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 动作版本状态：对应 action_version.status 列。
 */
@Getter
@Schema(description = "动作版本状态")
public enum VersionStatus implements BaseEnum<String> {
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已发布 */
    PUBLISHED("PUBLISHED", "已发布"),
    /** 已废弃 */
    DEPRECATED("DEPRECATED", "已废弃");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    VersionStatus(String code, String name) {
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
    public static VersionStatus fromCode(String code) {
        for (VersionStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的动作版本状态码值：" + code);
    }
}
