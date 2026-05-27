package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.query.LikeListQuery;
import com.esunbank.financialpreference.business.service.LikeListService;
import com.esunbank.financialpreference.presentation.dto.ApiResponse;
import com.esunbank.financialpreference.presentation.dto.request.CreateLikeRequest;
import com.esunbank.financialpreference.presentation.dto.request.UpdateLikeRequest;
import com.esunbank.financialpreference.presentation.dto.response.LikeItemResponse;
import com.esunbank.financialpreference.presentation.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.math.BigDecimal;
import java.security.Principal;

/**
 * 喜好清單 CRUD。所有端點均需 JWT；userId 一律從 JWT principal 取得，
 * 使用者只能 CRUD 自己的資料（ownership 由 SP 與 Service 雙重保障）。
 */
@RestController
@RequestMapping("/api/v1/likes")
@Validated
@Tag(name = "Likes", description = "金融商品喜好清單 CRUD（需登入）")
@SecurityRequirement(name = "bearerAuth")
public class LikeListController {

    private final LikeListService service;

    public LikeListController(LikeListService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "新增喜好商品", description = "userId 由 JWT 注入；同步建立 PRODUCT 與 LIKE_LIST")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "建立成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "輸入驗證 / ACCOUNT 不一致"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    })
    public ResponseEntity<ApiResponse<Long>> create(Principal principal,
                                                    @Valid @RequestBody CreateLikeRequest request) {
        long sn = service.create(request.toCommand(principal.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sn));
    }

    @GetMapping
    @Operation(summary = "查詢喜好清單",
            description = "回傳當前登入者的喜好商品，支援過濾 / 排序 / 分頁。預設第 1 頁、每頁 10 筆、依 SN DESC")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功（空清單亦為成功）"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    })
    public ApiResponse<PagedResponse<LikeItemResponse>> list(
            Principal principal,
            @Parameter(description = "產品名稱（模糊比對，LIKE %xxx%）") @RequestParam(required = false) String productName,
            @Parameter(description = "扣款帳號（完全比對）")             @RequestParam(required = false) String account,
            @Parameter(description = "預計扣款下限")                      @RequestParam(required = false) BigDecimal amountMin,
            @Parameter(description = "預計扣款上限")                      @RequestParam(required = false) BigDecimal amountMax,
            @Parameter(description = "費率下限（例 0.01 = 1%）")          @RequestParam(required = false) BigDecimal feeRateMin,
            @Parameter(description = "費率上限")                          @RequestParam(required = false) BigDecimal feeRateMax,
            @Parameter(description = "排序欄位（sn|productName|price|feeRate|purchaseQuantity|totalFee|totalAmount）")
                                                                          @RequestParam(required = false) String sortBy,
            @Parameter(description = "排序方向（asc|desc）")               @RequestParam(required = false) String sortDir,
            @Parameter(description = "頁碼（1 起算）")                     @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數（上限 200）")               @RequestParam(defaultValue = "10") int pageSize) {
        LikeListQuery query = new LikeListQuery(
                productName, account, amountMin, amountMax, feeRateMin, feeRateMax,
                sortBy, sortDir, page, pageSize);
        return ApiResponse.success(
                PagedResponse.from(service.listByUserId(principal.getName(), query), LikeItemResponse::from));
    }

    @PutMapping("/{sn}")
    @Operation(summary = "更新喜好商品", description = "只能更新自己的喜好；SN 屬於他人 → 403")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "輸入驗證 / ACCOUNT 不一致"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非該紀錄擁有者"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "喜好紀錄不存在")
    })
    public ApiResponse<Void> update(
            Principal principal,
            @Parameter(description = "喜好紀錄流水序號", required = true, example = "1")
            @PathVariable @Positive long sn,
            @Valid @RequestBody UpdateLikeRequest request) {
        service.update(request.toCommand(sn, principal.getName()));
        return ApiResponse.success();
    }

    @DeleteMapping("/{sn}")
    @Operation(summary = "刪除喜好商品", description = "只能刪除自己的喜好；SN 屬於他人 → 403")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刪除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非該紀錄擁有者"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "喜好紀錄不存在")
    })
    public ApiResponse<Void> delete(
            Principal principal,
            @Parameter(description = "喜好紀錄流水序號", required = true, example = "1")
            @PathVariable @Positive long sn) {
        service.delete(sn, principal.getName());
        return ApiResponse.success();
    }
}
