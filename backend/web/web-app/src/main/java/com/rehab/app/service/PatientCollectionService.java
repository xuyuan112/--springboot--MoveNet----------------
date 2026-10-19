package com.rehab.app.service;

import com.rehab.app.vo.PatientCollectionVO;
import com.rehab.common.result.PageResult;

/**
 * 患者内容收藏服务（P5-B3，接口文档 3.5）。
 *
 * <p>重复收藏幂等（唯一约束兜底返回 200）；取消收藏幂等；
 * 收藏的内容必须当前 PUBLISHED 且在有效期内；内容下架后历史收藏保留但不再展示正文。</p>
 */
public interface PatientCollectionService {

    /** 分页查询本人收藏（下架/过期内容 available=false 且不含摘要与封面）。 */
    PageResult<PatientCollectionVO> pageMyCollections(Integer page, Integer size);

    /** 收藏内容（重复请求幂等成功）。 */
    void collect(Long contentId);

    /** 取消收藏（未收藏时幂等成功）。 */
    void uncollect(Long contentId);
}
