package com.esunbank.financialpreference.presentation.dto.request;

import com.esunbank.financialpreference.business.command.CreateLikeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "新增喜好商品請求")
public record CreateLikeRequest(

        @Schema(description = "使用者 ID", example = "A1236456789", maxLength = 20)
        @NotBlank @Size(max = 20)
        String userId,

        @Schema(description = "金融商品名稱", example = "玉山美元定存", maxLength = 100)
        @NotBlank @Size(max = 100)
        String productName,

        @Schema(description = "產品價格（TWD）", example = "1000.00")
        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 16, fraction = 2)
        BigDecimal price,

        @Schema(description = "手續費率（0.0000–1.0000）", example = "0.0100")
        @NotNull
        @DecimalMin(value = "0.0000", inclusive = true)
        @DecimalMax(value = "1.0000", inclusive = true)
        @Digits(integer = 1, fraction = 4)
        BigDecimal feeRate,

        @Schema(description = "購買數量", example = "5", minimum = "1")
        @Min(1)
        int purchaseQuantity,

        @Schema(description = "扣款帳號", example = "1111999666", maxLength = 20)
        @NotBlank @Size(max = 20)
        String account
) {
    public CreateLikeCommand toCommand() {
        return new CreateLikeCommand(userId, productName, price, feeRate, purchaseQuantity, account);
    }
}
