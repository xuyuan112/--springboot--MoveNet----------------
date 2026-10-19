package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 康复计划状态：对应 rehab_plan.status 列（P6 状态机对齐《接口文档》5.2）。
 *
 * <p>状态机：{@code DRAFT → PUBLISHED → PAUSED/COMPLETED/ARCHIVED}；</p>
 * <ul>
 *   <li>仅 DRAFT 可编辑基本信息/动作项/日程与删除；</li>
 *   <li>仅 PUBLISHED 状态由每日调度生成 plan_daily_task；</li>
 *   <li>PAUSED 可恢复（resume）回 PUBLISHED；</li>
 *   <li>ARCHIVED 为终态（结束/作废），停止生成新任务；</li>
 *   <li>COMPLETED 为终态（预留到期自动完成）。</li>
 * </ul>
 */
@Getter
@Schema(description = "康复计划状态")
public enum PlanStatus implements BaseEnum<String> {
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已发布（进行中，调度生成任务） */
    PUBLISHED("PUBLISHED", "已发布"),
    /** 已暂停 */
    PAUSED("PAUSED", "已暂停"),
    /** 已完成 */
    COMPLETED("COMPLETED", "已完成"),
    /** 已归档（结束/作废，终态） */
    ARCHIVED("ARCHIVED", "已归档");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    PlanStatus(String code, String name) {
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
    public static PlanStatus fromCode(String code) {
        for (PlanStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的康复计划状态码值：" + code);
    }
}
