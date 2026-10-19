package com.rehab.app.service;

import com.rehab.app.vo.PatientActionDetailVO;
import com.rehab.app.vo.PatientActionVO;
import com.rehab.common.result.PageResult;

/**
 * 患者端动作库服务（P4-B1）：仅 PUBLISHED 动作及其 PUBLISHED 版本。
 */
public interface PatientActionService {

    /** 分页浏览已发布动作（partCode/difficulty/keyword 筛选）；已下架动作不出现在列表 */
    PageResult<PatientActionVO> listActions(Integer page, Integer size, String partCode, String difficulty, String keyword);

    /** 动作版本快照详情（算法参数/运动签名/评估规则/引擎标识；发布版本只读） */
    PatientActionDetailVO getActionDetail(Long actionVersionId);
}
