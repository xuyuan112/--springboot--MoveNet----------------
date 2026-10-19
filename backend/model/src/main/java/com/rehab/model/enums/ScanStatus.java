package com.rehab.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 文件安全扫描状态：对应 file_object.scan_status 列。
 */
@Getter
@Schema(description = "文件安全扫描状态")
public enum ScanStatus implements BaseEnum<String> {
    /** 待扫描 */
    PENDING("PENDING", "待扫描"),
    /** 扫描通过 */
    PASSED("PASSED", "扫描通过"),
    /** 扫描失败 */
    FAILED("FAILED", "扫描失败");

    /** 枚举码值（落库值 / 接口传输值） */
    @EnumValue
    @JsonValue
    @Schema(description = "枚举码值（落库值 / 接口传输值）")
    private final String code;

    /** 中文名称（用于日志、报告、导出等展示场景） */
    @Schema(description = "中文名称")
    private final String name;

    ScanStatus(String code, String name) {
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
    public static ScanStatus fromCode(String code) {
        for (ScanStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的文件安全扫描状态码值：" + code);
    }
}
