package com.rehab.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.model.dto.ActionRuleRequest;
import com.rehab.model.entity.ActionRule;
import com.rehab.admin.vo.AdminActionRuleVO;

import java.util.List;

/**
 * 管理端 - 动作规则服务。
 */
public interface AdminActionRuleService extends IService<ActionRule> {

    /** 查询指定版本下的规则列表。 */
    List<AdminActionRuleVO> listRules(Long versionId);

    /** 创建规则（仅 DRAFT 版本可编辑）。 */
    Long createRule(Long versionId, ActionRuleRequest request);

    /** 修改规则（仅 DRAFT 版本可编辑）。 */
    void updateRule(Long versionId, Long ruleId, ActionRuleRequest request);

    /** 删除规则（仅 DRAFT 版本可编辑，逻辑删除）。 */
    void deleteRule(Long versionId, Long ruleId);
}
