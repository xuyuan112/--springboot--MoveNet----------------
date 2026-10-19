package com.rehab.app.controller.common;

import com.rehab.app.service.RehabPartDictService;
import com.rehab.common.result.ApiResult;
import com.rehab.app.vo.RehabPartDictVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共只读接口：/public/**（匿名可访问）。
 * 后续在此扩展：当前有效公告、动作演示预签名资源等。
 */
@Tag(name = "公共接口")
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final RehabPartDictService rehabPartDictService;

    @Operation(summary = "查询启用中的康复部位字典")
    @GetMapping("/rehab-parts")
    public ApiResult<List<RehabPartDictVO>> enabledRehabParts() {
        return ApiResult.ok(rehabPartDictService.listEnabled());
    }
}
