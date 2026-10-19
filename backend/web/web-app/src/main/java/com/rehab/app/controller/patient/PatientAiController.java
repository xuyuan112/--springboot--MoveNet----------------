package com.rehab.app.controller.patient;

import com.rehab.app.service.AiQaService;
import com.rehab.app.vo.AiQaVO;
import com.rehab.common.result.ApiResult;
import com.rehab.model.dto.AiQaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者端 AI 知识问答接口（P5-B4）：/patient/ai/qa。
 *
 * <p>不持久化会话历史；响应强制携带免责声明；仅回答一般性知识；
 * 单用户每日限流（429）；AI 依赖异常 502/504。患者无治疗师绑定也可使用。</p>
 */
@Tag(name = "患者端 - AI 知识问答")
@RestController
@RequestMapping("/patient/ai")
@RequiredArgsConstructor
public class PatientAiController {

    private final AiQaService aiQaService;

    @Operation(summary = "AI 知识问答（不持久化历史，含免责声明）")
    @PostMapping("/qa")
    public ApiResult<AiQaVO> ask(@Valid @RequestBody AiQaRequest request) {
        return ApiResult.ok(aiQaService.ask(request.getQuestion()));
    }
}
