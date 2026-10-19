package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 角色编码：对应 user_account.role_code 列。
 */
@Getter
@Schema(description = "角色编码")
public enum RoleCode implements BaseEnum<String> {
    /** 患者 */
    PATIENT("PATIENT", "患者"),
    /** 治疗师 */
    THERAPIST("THERAPIST", "治疗师"),
    /** 管理员 */
    ADMIN("ADMIN", "管理员");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    RoleCode(String code, String name) {
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
    public static RoleCode fromCode(String code) {
        for (RoleCode value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的角色编码码值：" + code);
    }
}
