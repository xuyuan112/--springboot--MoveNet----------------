package com.rehab.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ActionTemplateSaveRequest;
import com.rehab.model.entity.ActionTemplate;
import com.rehab.admin.vo.AdminActionTemplateVO;

/**
 * 管理端 - 动作模板服务。
 */
public interface AdminActionTemplateService extends IService<ActionTemplate> {

    /**
     * 分页查询指定版本下的模板。
     */
    PageResult<AdminActionTemplateVO> pageTemplates(Long versionId, Integer page, Integer size);

    /** 创建模板（默认 ACTIVE）。 */
    Long createTemplate(Long versionId, ActionTemplateSaveRequest request);

    /** 发布模板（status → ACTIVE）。 */
    void publish(Long templateId);

    /** 删除模板（未被引用时逻辑删除）。 */
    void deleteTemplate(Long templateId);
}
