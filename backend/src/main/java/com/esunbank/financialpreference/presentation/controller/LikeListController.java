package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.service.LikeListService;
import com.esunbank.financialpreference.presentation.dto.ApiResponse;
import com.esunbank.financialpreference.presentation.dto.request.CreateLikeRequest;
import com.esunbank.financialpreference.presentation.dto.request.UpdateLikeRequest;
import com.esunbank.financialpreference.presentation.dto.response.LikeItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/likes")
@Validated
@Tag(name = "Likes", description = "金融商品喜好清單 CRUD")
public class LikeListController {

    private final LikeListService service;

    public LikeListController(LikeListService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "新增喜好商品", description = "同步建立 PRODUCT 與 LIKE_LIST，回傳新增的 SN")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "建立成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "輸入驗證失敗"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "使用者不存在")
    })
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody CreateLikeRequest request) {
        long sn = service.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sn));
    }

    @GetMapping
    @Operation(summary = "查詢喜好清單", description = "依 userId 查詢該使用者的所有喜好商品（三表 JOIN）")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功（空陣列亦為成功）"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "userId 缺失或為空")
    })
    public ApiResponse<List<LikeItemResponse>> list(
            @Parameter(description = "使用者 ID", required = true, example = "A1236456789")
            @RequestParam @NotBlank String userId) {
        List<LikeItemResponse> items = service.listByUserId(userId).stream()
                .map(LikeItemResponse::from)
                .toList();
        return ApiResponse.success(items);
    }

    @PutMapping("/{sn}")
    @Operation(summary = "更新喜好商品", description = "更新指定 SN 的喜好商品與數量，並重算 TotalFee / TotalAmount")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "輸入驗證失敗"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "喜好紀錄不存在")
    })
    public ApiResponse<Void> update(
            @Parameter(description = "喜好紀錄流水序號", required = true, example = "1")
            @PathVariable @Positive long sn,
            @Valid @RequestBody UpdateLikeRequest request) {
        service.update(request.toCommand(sn));
        return ApiResponse.success();
    }

    @DeleteMapping("/{sn}")
    @Operation(summary = "刪除喜好商品", description = "刪除指定 SN，並同步刪除對應 PRODUCT")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刪除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "SN 驗證失敗"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "喜好紀錄不存在")
    })
    public ApiResponse<Void> delete(
            @Parameter(description = "喜好紀錄流水序號", required = true, example = "1")
            @PathVariable @Positive long sn) {
        service.delete(sn);
        return ApiResponse.success();
    }
}
