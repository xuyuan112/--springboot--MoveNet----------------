package com.rehab.app.controller.therapist;

import com.rehab.app.service.AppAnnouncementService;
import com.rehab.app.vo.AppAnnouncementVO;
import com.rehab.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 治疗师端 - 公告只读接口：/therapist/announcements（仅 THERAPIST）。
 */
@Tag(name = "治疗师端-公告")
@RestController
@RequestMapping("/therapist/announcements")
@RequiredArgsConstructor
public class TherapistAnnouncementController {

    private final AppAnnouncementService appAnnouncementService;

    @Operation(summary = "查询当前有效公告（全部 + 治疗师范围）")
    @GetMapping
    public ApiResult<List<AppAnnouncementVO>> list() {
        return ApiResult.ok(appAnnouncementService.listForTherapist());
    }
}
