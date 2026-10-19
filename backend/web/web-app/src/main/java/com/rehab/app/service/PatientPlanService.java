package com.rehab.app.service;

import com.rehab.app.vo.PatientPlanItemVO;
import com.rehab.app.vo.PatientPlanVO;
import com.rehab.common.result.PageResult;

import java.util.List;

/**
 * 患者端康复计划服务。
 */
public interface PatientPlanService {

    /**
     * 分页查询本人计划。
     *
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @param status 状态过滤（可选）
     * @return 分页结果
     */
    PageResult<PatientPlanVO> listMyPlans(Integer page, Integer size, String status);

    /**
     * 查询本人计划详情。
     *
     * @param planId 计划ID
     * @return 计划详情
     */
    PatientPlanVO getMyPlanDetail(Long planId);

    /**
     * 查询本人计划动作项列表。
     *
     * @param planId 计划ID
     * @return 动作项列表
     */
    List<PatientPlanItemVO> listMyPlanItems(Long planId);
}
