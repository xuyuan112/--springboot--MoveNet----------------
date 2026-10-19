package com.rehab.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ActionVersionSaveRequest;
import com.rehab.model.entity.ActionVersion;
import com.rehab.admin.vo.AdminActionVersionDetailVO;
import com.rehab.admin.vo.AdminActionVersionVO;

/**
 * 管理端 - 动作版本服务。
 */
public interface AdminActionVersionService extends IService<ActionVersion> {

    /**
     * 分页查询动作版本。
     *
     * @param actionId 动作ID筛选（可空）
     * @param status   状态筛选（可空）
     */
    PageResult<AdminActionVersionVO> pageVersions(Integer page, Integer size,
                                                  Long actionId, String status);

    /** 创建动作版本（versionNo 自动递增，默认 DRAFT）。 */
    Long createVersion(Long actionId, ActionVersionSaveRequest request);

    /** 查询版本详情（含规则与模板列表）。 */
    AdminActionVersionDetailVO detail(Long versionId);

    /** 修改版本（仅 DRAFT 可编辑）。 */
    void updateVersion(Long versionId, ActionVersionSaveRequest request);

    /** 发布版本（校验权重和=1、poseEngineId 非空）。 */
    void publish(Long versionId);
}
