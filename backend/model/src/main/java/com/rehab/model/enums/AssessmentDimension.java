package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 动作评估维度：对应 action_rule.dimension 列。
 */
@Getter
@Schema(description = "动作评估维度")
public enum AssessmentDimension implements BaseEnum<String> {
    /** 角度 */
    ANGLE("ANGLE", "角度"),
    /** 稳定性 */
    STABILITY("STABILITY", "稳定性"),
    /** 对称性 */
    SYMMETRY("SYMMETRY", "对称性"),
    /** 保持 */
    HOLD("HOLD", "保持"),
    /** 速度 */
    SPEED("SPEED", "速度");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    AssessmentDimension(String code, String name) {
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
    public static AssessmentDimension fromCode(String code) {
        for (AssessmentDimension value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的动作评估维度码值：" + code);
    }
}
