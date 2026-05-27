package com.esunbank.financialpreference.presentation.dto.response;

import com.esunbank.financialpreference.business.domain.LikeItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "喜好商品紀錄（三表 JOIN 結果）")
public record LikeItemResponse(

        @Schema(description = "流水序號", example = "1")
        long sn,

        @Schema(description = "使用者 ID", example = "A1236456789")
        String userId,

        @Schema(description = "使用者名稱", example = "王o明")
        String userName,

        @Schema(description = "使用者 Email", example = "test@email.com")
        String email,

        @Schema(description = "產品流水號", example = "10")
        long productNo,

        @Schema(description = "產品名稱", example = "玉山美元定存")
        String productName,

        @Schema(description = "產品價格", example = "1000.00")
        BigDecimal price,

        @Schema(description = "手續費率", example = "0.0100")
        BigDecimal feeRate,

        @Schema(description = "購買數量", example = "5")
        int purchaseQuantity,

        @Schema(description = "扣款帳號（下單當下快照）", example = "1111999666")
        String account,

        @Schema(description = "總手續費（TWD）", example = "50.00")
        BigDecimal totalFee,

        @Schema(description = "預計扣款總金額", example = "5050.00")
        BigDecimal totalAmount
) {
    public static LikeItemResponse from(LikeItem item) {
        return new LikeItemResponse(
                item.sn(),
                item.user().userId(),
                item.user().userName(),
                item.user().email(),
                item.product().no(),
                item.product().productName(),
                item.product().price(),
                item.product().feeRate(),
                item.purchaseQuantity(),
                item.account(),
                item.totalFee(),
                item.totalAmount()
        );
    }
}
