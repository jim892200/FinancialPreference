package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.service.LikeListService;
import com.esunbank.financialpreference.presentation.dto.ApiResponse;
import com.esunbank.financialpreference.presentation.dto.request.CreateLikeRequest;
import com.esunbank.financialpreference.presentation.dto.request.UpdateLikeRequest;
import com.esunbank.financialpreference.presentation.dto.response.LikeItemResponse;
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
public class LikeListController {

    private final LikeListService service;

    public LikeListController(LikeListService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody CreateLikeRequest request) {
        long sn = service.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sn));
    }

    @GetMapping
    public ApiResponse<List<LikeItemResponse>> list(@RequestParam @NotBlank String userId) {
        List<LikeItemResponse> items = service.listByUserId(userId).stream()
                .map(LikeItemResponse::from)
                .toList();
        return ApiResponse.success(items);
    }

    @PutMapping("/{sn}")
    public ApiResponse<Void> update(@PathVariable @Positive long sn,
                                    @Valid @RequestBody UpdateLikeRequest request) {
        service.update(request.toCommand(sn));
        return ApiResponse.success();
    }

    @DeleteMapping("/{sn}")
    public ApiResponse<Void> delete(@PathVariable @Positive long sn) {
        service.delete(sn);
        return ApiResponse.success();
    }
}
