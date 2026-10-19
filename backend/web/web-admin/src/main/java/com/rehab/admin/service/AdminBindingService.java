package com.rehab.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.BindingCreateRequest;
import com.rehab.model.dto.BindingUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.admin.vo.BindingVO;

/**
 * 管理端 - 医患绑定关系服务。
 * 继承 {@link IService} 获得 patient_therapist 单表通用 CRUD。
 */
public interface AdminBindingService extends IService<PatientTherapist> {

    /**
     * 分页查询绑定关系。
     */
    PageResult<BindingVO> pageBindings(Integer page, Integer size,
                                       Long patientId, Long therapistId, String status);

    /**
     * 建立主责/协作绑定：校验双方账号存在、启用、角色正确及主责唯一性/有效期冲突。
     *
     * @return 新建绑定关系ID
     */
    Long createBinding(BindingCreateRequest request);

    /**
     * 变更关系类型或生效期（仅 ACTIVE 绑定可变更）。
     */
    void updateBinding(Long bindingId, BindingUpdateRequest request);

    /**
     * 解除绑定（status=ENDED，结束时间置为当前时刻）；历史业务数据保留只读，不物理删除。
     */
    void unbind(Long bindingId, ReasonRequest reason);
}
