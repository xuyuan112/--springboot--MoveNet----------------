package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 康复部位大类：对应 rehab_part_dict.group_code 列。
 */
@Getter
@Schema(description = "康复部位大类")
public enum RehabPartGroup implements BaseEnum<String> {
    /** 上肢 */
    UPPER("UPPER", "上肢"),
    /** 下肢 */
    LOWER("LOWER", "下肢"),
    /** 脊柱 */
    SPINE("SPINE", "脊柱"),
    /** 全身综合 */
    OVERALL("OVERALL", "全身综合");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    RehabPartGroup(String code, String name) {
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
    public static RehabPartGroup fromCode(String code) {
        for (RehabPartGroup value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的康复部位大类码值：" + code);
    }
}
