package com.rehab.model.dto;

import com.rehab.model.enums.RehabPartGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 康复部位字典保存请求（新增 / 编辑）
 */
@Schema(description = "康复部位字典保存请求（新增 / 编辑）")
@Data
public class RehabPartDictDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部位编码，新增后不可修改；大写字母+下划线 */
    @Schema(description = "部位编码，新增后不可修改；大写字母开头，仅含大写字母/数字/下划线，最长 30 位",
            example = "SHOULDER")
    @NotBlank(message = "部位编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$", message = "编码需为大写字母开头，仅含大写字母/数字/下划线，最长 30 位")
    private String code;

    /** 部位名称 */
    @Schema(description = "部位名称", example = "肩部")
    @NotBlank(message = "部位名称不能为空")
    @Size(max = 50, message = "部位名称最长 50 个字符")
    private String name;

    /** 部位大类：UPPER 上肢 / LOWER 下肢 / SPINE 脊柱 / OVERALL 全身综合 */
    @Schema(description = "部位大类：UPPER 上肢 / LOWER 下肢 / SPINE 脊柱 / OVERALL 全身综合",
            example = "UPPER")
    @NotNull(message = "部位大类不能为空")
    private RehabPartGroup groupCode;

    /** 排序号（升序，数值越小越靠前） */
    @Schema(description = "排序号（升序，数值越小越靠前）", example = "10")
    @NotNull(message = "排序号不能为空")
    private Integer sortNo;

    /** 是否启用（不传默认启用） */
    @Schema(description = "是否启用（不传默认启用）", example = "true")
    private Boolean enabled = Boolean.TRUE;
}
