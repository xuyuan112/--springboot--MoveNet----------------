package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 每日任务状态：对应 plan_daily_task.status 列。
 */
@Getter
@Schema(description = "每日任务状态")
public enum DailyTaskStatus implements BaseEnum<String> {
    /** 待完成 */
    PENDING("PENDING", "待完成"),
    /** 进行中 */
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    /** 已完成 */
    COMPLETED("COMPLETED", "已完成"),
    /** 已跳过 */
    SKIPPED("SKIPPED", "已跳过"),
    /** 已过期 */
    EXPIRED("EXPIRED", "已过期");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    DailyTaskStatus(String code, String name) {
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
    public static DailyTaskStatus fromCode(String code) {
        for (DailyTaskStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的每日任务状态码值：" + code);
    }
}
