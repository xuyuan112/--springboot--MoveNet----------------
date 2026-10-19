package com.rehab.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ActionSaveRequest;
import com.rehab.model.entity.Action;
import com.rehab.admin.vo.AdminActionDetailVO;
import com.rehab.admin.vo.AdminActionVO;

/**
 * 管理端 - 动作基础信息服务。
 */
public interface AdminActionService extends IService<Action> {

    /**
     * 分页查询动作。
     *
     * @param targetPart 目标部位编码筛选（可空）
     * @param status     状态筛选（可空）
     * @param keyword    名称/说明模糊匹配（可空）
     */
    PageResult<AdminActionVO> pageActions(Integer page, Integer size,
                                          String targetPart, String status, String keyword);

    /** 创建动作（默认 DRAFT）。 */
    Long createAction(ActionSaveRequest request);

    /** 查询动作详情及版本列表。 */
    AdminActionDetailVO detail(Long actionId);

    /** 修改动作基础信息（仅 DRAFT 可编辑）。 */
    void updateAction(Long actionId, ActionSaveRequest request);

    /** 动作下线（status → DISABLED）。 */
    void offline(Long actionId);
}
