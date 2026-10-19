package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 身体侧别：对应 patient_rehab_part.side 列。
 */
@Getter
@Schema(description = "身体侧别")
public enum BodySide implements BaseEnum<String> {
    /** 左侧 */
    LEFT("LEFT", "左侧"),
    /** 右侧 */
    RIGHT("RIGHT", "右侧"),
    /** 双侧 */
    BILATERAL("BILATERAL", "双侧");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    BodySide(String code, String name) {
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
    public static BodySide fromCode(String code) {
        for (BodySide value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的身体侧别码值：" + code);
    }
}
