package com.rehab.model.dto;

import com.rehab.model.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准动作基础信息创建/修改请求（POST、PUT /admin/actions[/{actionId}]，仅未发布可改）
 */
@Data
@Schema(description = "标准动作基础信息创建/修改请求（POST、PUT /admin/actions[/{actionId}]，仅未发布可改）")
public class ActionSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 动作名称 */
    @Schema(description = "动作名称", example = "坐位膝关节屈伸")
    @NotBlank(message = "动作名称不能为空")
    @Size(max = 100, message = "动作名称最长 100 个字符")
    private String name;

    /** 目标康复部位编码 */
    @Schema(description = "目标康复部位编码", example = "KNEE")
    @NotBlank(message = "目标部位不能为空")
    @Size(max = 30, message = "部位编码最长 30 个字符")
    private String targetPartCode;

    /** 难度：EASY 简单 / MEDIUM 中等 / HARD 困难 */
    @Schema(description = "难度：EASY / MEDIUM / HARD", example = "EASY")
    @NotNull(message = "难度不能为空")
    private Difficulty difficulty;

    /** 动作说明 */
    @Schema(description = "动作说明")
    @Size(max = 1000, message = "动作说明最长 1000 个字符")
    private String description;

}
