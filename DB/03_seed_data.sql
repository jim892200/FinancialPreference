/* =============================================================================
   FinancialPreference - 03_seed_data.sql
   範例資料：2 名 USER、3 個 PRODUCT、3 筆 LIKE_LIST
   ============================================================================= */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- 清空舊資料（依 FK 反向順序）
-- 注意：不重設 IDENTITY，以避免首次部署時 RESEED 0 造成下一個值為 0
DELETE FROM dbo.LIKE_LIST;
DELETE FROM dbo.PRODUCT;
DELETE FROM dbo.[USER];
GO

-- -----------------------------------------------------------------------------
-- USER
-- -----------------------------------------------------------------------------
INSERT INTO dbo.[USER] (USER_ID, USER_NAME, EMAIL, ACCOUNT) VALUES
    (N'A1236456789', N'王o明', N'test@email.com',    N'1111999666'),
    (N'B9876543210', N'陳o華', N'chen@example.com',  N'2222888777');
GO

-- -----------------------------------------------------------------------------
-- PRODUCT + LIKE_LIST
--   PRODUCT.NO 由 IDENTITY 自動產生（首次為 1,2,3；重跑後為更大值）
--   用 SCOPE_IDENTITY() 捕捉實際產生的 NO，避免硬編碼造成 FK 違反
--
--   公式：
--     TOTAL_FEE    = PRICE * FEE_RATE * QTY
--     TOTAL_AMOUNT = PRICE * QTY + TOTAL_FEE
-- -----------------------------------------------------------------------------
DECLARE @P1 BIGINT, @P2 BIGINT, @P3 BIGINT;

INSERT INTO dbo.PRODUCT (PRODUCT_NAME, PRICE, FEE_RATE)
VALUES (N'玉山美元定存', 1000.00, 0.0100);
SET @P1 = SCOPE_IDENTITY();

INSERT INTO dbo.PRODUCT (PRODUCT_NAME, PRICE, FEE_RATE)
VALUES (N'玉山日圓基金', 500.00, 0.0150);
SET @P2 = SCOPE_IDENTITY();

INSERT INTO dbo.PRODUCT (PRODUCT_NAME, PRICE, FEE_RATE)
VALUES (N'玉山台幣高息債券基金', 2000.00, 0.0080);
SET @P3 = SCOPE_IDENTITY();

INSERT INTO dbo.LIKE_LIST
    (USER_ID,        PRODUCT_NO, PURCHASE_QUANTITY, ACCOUNT,       TOTAL_FEE, TOTAL_AMOUNT)
VALUES
    (N'A1236456789', @P1,        5,                 N'1111999666',     50.00,     5050.00),
    (N'A1236456789', @P2,        10,                N'1111999666',     75.00,     5075.00),
    (N'B9876543210', @P3,        3,                 N'2222888777',     48.00,     6048.00);
GO

-- 驗證
SELECT 'USER'      AS TBL, COUNT(*) AS CNT FROM dbo.[USER]
UNION ALL SELECT 'PRODUCT',   COUNT(*) FROM dbo.PRODUCT
UNION ALL SELECT 'LIKE_LIST', COUNT(*) FROM dbo.LIKE_LIST;
GO
