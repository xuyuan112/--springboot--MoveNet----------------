package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 公告可见范围：对应 announcement.scope 列。
 */
@Getter
@Schema(description = "公告可见范围")
public enum AnnouncementScope implements BaseEnum<String> {
    /** 全部用户 */
    ALL("ALL", "全部"),
    /** 仅患者 */
    PATIENT("PATIENT", "患者"),
    /** 仅治疗师 */
    THERAPIST("THERAPIST", "治疗师");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称 */
    @Schema(description = "中文名称")
    private final String name;

    AnnouncementScope(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static AnnouncementScope fromCode(String code) {
        for (AnnouncementScope value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的公告可见范围码值：" + code);
    }
}
