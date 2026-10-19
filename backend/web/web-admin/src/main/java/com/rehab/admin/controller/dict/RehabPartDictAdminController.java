package com.rehab.admin.controller.dict;

import com.rehab.app.service.RehabPartDictService;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.RehabPartDictDTO;
import com.rehab.app.vo.RehabPartDictVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 康复部位字典：/admin/dictionaries/rehab-parts（仅 ADMIN）。
 * 作为后台标准 CRUD 范式：分页查询 + 新增/编辑/删除 + 操作日志。
 */
@Tag(name = "管理端-字典管理")
@RestController
@RequestMapping("/admin/dictionaries/rehab-parts")
@RequiredArgsConstructor
public class RehabPartDictAdminController {

    private final RehabPartDictService rehabPartDictService;

    @Operation(summary = "分页查询康复部位字典")
    @GetMapping
    public ApiResult<PageResult<RehabPartDictVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(rehabPartDictService.pageDict(page, size, groupCode, keyword));
    }

    @Operation(summary = "新增康复部位字典项")
    @OperationLog(module = "DICT", action = "CREATE", targetType = "REHAB_PART_DICT", recordParam = true)
    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody RehabPartDictDTO dto) {
        rehabPartDictService.create(dto);
        return ApiResult.ok();
    }

    @Operation(summary = "修改康复部位字典项（编码不可变）")
    @OperationLog(module = "DICT", action = "UPDATE", targetType = "REHAB_PART_DICT", recordParam = true)
    @PutMapping("/{code}")
    public ApiResult<Void> update(@PathVariable String code,
                                  @Valid @RequestBody RehabPartDictDTO dto) {
        rehabPartDictService.update(code, dto);
        return ApiResult.ok();
    }

    @Operation(summary = "删除康复部位字典项")
    @OperationLog(module = "DICT", action = "DELETE", targetType = "REHAB_PART_DICT")
    @DeleteMapping("/{code}")
    public ApiResult<Void> remove(@PathVariable String code) {
        rehabPartDictService.removeDict(code);
        return ApiResult.ok();
    }
}
