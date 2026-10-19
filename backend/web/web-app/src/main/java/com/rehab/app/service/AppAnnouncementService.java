package com.rehab.app.service;

import com.rehab.app.vo.AppAnnouncementVO;

import java.util.List;

/**
 * 患者/治疗师端 - 公告只读查询。
 * 仅返回 status=PUBLISHED 且当前时间落在 [startTime, endTime] 内、
 * 可见范围匹配的公告。
 */
public interface AppAnnouncementService {

    /** 查询患者可见公告（scope = ALL / PATIENT） */
    List<AppAnnouncementVO> listForPatient();

    /** 查询治疗师可见公告（scope = ALL / THERAPIST） */
    List<AppAnnouncementVO> listForTherapist();
}
