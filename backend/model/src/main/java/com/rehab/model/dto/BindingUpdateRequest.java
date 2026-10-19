package com.rehab.model.dto;

import com.rehab.model.enums.RelationType;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 变更医患绑定请求（PUT /admin/bindings/{bindingId}，字段可选）
 */
@Data
@Schema(description = "变更医患绑定请求（PUT /admin/bindings/{bindingId}，字段可选）")
public class BindingUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关系类型：PRIMARY 主责 / SECONDARY 协作 */
    @Schema(description = "关系类型：PRIMARY 主责 / SECONDARY 协作")
    private RelationType relationType;

    /** 绑定生效时间 */
    @Schema(description = "绑定生效时间")
    private LocalDateTime startAt;

    /** 绑定失效时间 */
    @Schema(description = "绑定失效时间")
    private LocalDateTime endAt;

}
