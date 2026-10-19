package com.rehab.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.RehabPartDictDTO;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.app.vo.RehabPartDictVO;

import java.util.List;

/**
 * 康复部位字典服务。
 * 各业务表（患者档案 / rehab_plan / action / content）统一引用本字典编码，禁止自由文本。
 */
public interface RehabPartDictService extends IService<RehabPartDict> {

    /**
     * 管理端分页查询（可按部位大类与名称/编码关键词筛选）。
     */
    PageResult<RehabPartDictVO> pageDict(Integer page, Integer size, String groupCode, String keyword);

    /**
     * 查询全部启用字典项（公共下拉接口）。
     */
    List<RehabPartDictVO> listEnabled();

    /**
     * 新增字典项。
     */
    void create(RehabPartDictDTO dto);

    /**
     * 修改字典项（编码不可变）。
     */
    void update(String code, RehabPartDictDTO dto);

    /**
     * 删除字典项（被业务数据引用时拒绝，引用计数校验在业务迭代中补充）。
     */
    void removeDict(String code);
}
