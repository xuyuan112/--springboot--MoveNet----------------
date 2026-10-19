package com.rehab.app.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 康复部位字典项视图对象（enabled 对前端暴露为布尔语义）
 */
@Schema(description = "康复部位字典项视图对象（enabled 对前端暴露为布尔语义）")
@Data
public class RehabPartDictVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部位编码（业务主键） */
    @Schema(description = "部位编码（业务主键）")
    private String code;

    /** 部位名称 */
    @Schema(description = "部位名称")
    private String name;

    /** 部位大类：UPPER / LOWER / SPINE / OVERALL */
    @Schema(description = "部位大类：UPPER / LOWER / SPINE / OVERALL")
    private String groupCode;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortNo;

    /** 是否启用 */
    @Schema(description = "是否启用")
    private Boolean enabled;
}
