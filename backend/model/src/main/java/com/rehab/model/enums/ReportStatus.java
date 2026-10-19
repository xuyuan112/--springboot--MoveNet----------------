package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 评估报告状态：对应 assessment_report.status 列。
 */
@Getter
@Schema(description = "评估报告状态")
public enum ReportStatus implements BaseEnum<String> {
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** AI 已生成 */
    AI_GENERATED("AI_GENERATED", "AI 已生成"),
    /** 治疗师已审核 */
    REVIEWED("REVIEWED", "治疗师已审核"),
    /** 已发布 */
    PUBLISHED("PUBLISHED", "已发布");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    ReportStatus(String code, String name) {
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
    public static ReportStatus fromCode(String code) {
        for (ReportStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的评估报告状态码值：" + code);
    }
}
