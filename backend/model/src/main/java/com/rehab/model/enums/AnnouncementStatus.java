package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 公告状态：对应 announcement.status 列。
 */
@Getter
@Schema(description = "公告状态")
public enum AnnouncementStatus implements BaseEnum<String> {
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已发布 */
    PUBLISHED("PUBLISHED", "已发布"),
    /** 已下架 */
    OFFLINE("OFFLINE", "已下架");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称 */
    @Schema(description = "中文名称")
    private final String name;

    AnnouncementStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static AnnouncementStatus fromCode(String code) {
        for (AnnouncementStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的公告状态码值：" + code);
    }
}
