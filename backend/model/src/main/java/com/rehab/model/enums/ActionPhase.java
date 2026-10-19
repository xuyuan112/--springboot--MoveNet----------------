package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 动作阶段：对应 action_rule.phase 列。
 */
@Getter
@Schema(description = "动作阶段")
public enum ActionPhase implements BaseEnum<String> {
    /** 准备阶段 */
    START("START", "准备阶段"),
    /** 向心阶段 */
    CONCENTRIC("CONCENTRIC", "向心阶段"),
    /** 离心阶段 */
    ECCENTRIC("ECCENTRIC", "离心阶段"),
    /** 保持阶段 */
    HOLD("HOLD", "保持阶段"),
    /** 全程 */
    FULL("FULL", "全程");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    ActionPhase(String code, String name) {
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
    public static ActionPhase fromCode(String code) {
        for (ActionPhase value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的动作阶段码值：" + code);
    }
}
