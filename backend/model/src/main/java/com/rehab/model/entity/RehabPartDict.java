package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 康复部位字典
 */
@Schema(description = "康复部位字典")
@Data
@TableName("rehab_part_dict")
public class RehabPartDict {

    /** 康复部位编码 */
    @Schema(description = "康复部位编码")
    @TableId(value = "code", type = IdType.INPUT)
    private String code;

    /** 康复部位名称 */
    @Schema(description = "康复部位名称")
    private String name;

    /** 部位大类：UPPER上肢/LOWER下肢/SPINE脊柱/OVERALL全身综合 */
    @Schema(description = "部位大类：UPPER上肢/LOWER下肢/SPINE脊柱/OVERALL全身综合")
    private String groupCode;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortNo;

    /** 是否启用：1是0否 */
    @Schema(description = "是否启用：1是0否")
    private Integer enabled;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
