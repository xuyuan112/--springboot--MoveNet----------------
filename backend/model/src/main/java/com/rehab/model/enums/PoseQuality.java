package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 姿态质量等级：对应 training_record.pose_quality 列。
 */
@Getter
@Schema(description = "姿态质量等级")
public enum PoseQuality implements BaseEnum<String> {
    /** 良好 */
    GOOD("GOOD", "良好"),
    /** 一般 */
    FAIR("FAIR", "一般"),
    /** 较差 */
    POOR("POOR", "较差"),
    /**
     * 数据不足（P4-B2）：有效帧占比低于 algo_config.validFrameRatioMin
     * 或修复帧占比超过 repairRatioLimit 时落库为该等级——记录保留，
     * 但不计入任务完成数、趋势与均值（需求 3.3 / 6.3.13）。
     */
    INSUFFICIENT("INSUFFICIENT", "数据不足");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    PoseQuality(String code, String name) {
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
    public static PoseQuality fromCode(String code) {
        for (PoseQuality value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的姿态质量等级码值：" + code);
    }
}
