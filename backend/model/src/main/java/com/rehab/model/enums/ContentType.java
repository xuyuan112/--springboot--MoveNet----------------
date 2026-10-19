package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 健康内容类型：对应 health_content.content_type 列。
 */
@Getter
@Schema(description = "健康内容类型")
public enum ContentType implements BaseEnum<String> {
    /** 图文文章 */
    ARTICLE("ARTICLE", "图文文章"),
    /** 视频 */
    VIDEO("VIDEO", "视频"),
    /** 指南 */
    GUIDE("GUIDE", "指南");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    ContentType(String code, String name) {
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
    public static ContentType fromCode(String code) {
        for (ContentType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的健康内容类型码值：" + code);
    }
}
