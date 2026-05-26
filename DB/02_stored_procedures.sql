/* =============================================================================
   FinancialPreference - 02_stored_procedures.sql
   Stored Procedure 定義（4 支）

   - SP_LIKE_INSERT          新增喜好（INSERT PRODUCT + INSERT LIKE_LIST）
   - SP_LIKE_QUERY_BY_USER   查詢某 USER 的喜好清單（三表 JOIN）
   - SP_LIKE_UPDATE          更新產品 + 數量 + 帳號（重算金額）
   - SP_LIKE_DELETE          刪除喜好（DELETE LIKE_LIST + DELETE PRODUCT）

   規範：
     1. 全部具名參數，杜絕 SQL Injection
     2. 跨表異動均在 BEGIN TRY / BEGIN TRAN / COMMIT / ROLLBACK / THROW 包覆
     3. 金額欄位一律 DECIMAL
     4. INSERT/UPDATE 一律重算 TOTAL_FEE、TOTAL_AMOUNT
   ============================================================================= */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO


-- =============================================================================
-- SP_LIKE_INSERT
--   INSERT PRODUCT → 取 SCOPE_IDENTITY() → INSERT LIKE_LIST
--   OUT 參數 @NEW_SN 回傳新增的 LIKE_LIST.SN
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_INSERT', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_INSERT;
GO

CREATE PROCEDURE dbo.SP_LIKE_INSERT
    @USER_ID            VARCHAR(20),
    @PRODUCT_NAME       NVARCHAR(100),
    @PRICE              DECIMAL(18, 2),
    @FEE_RATE           DECIMAL(5, 4),
    @PURCHASE_QUANTITY  INT,
    @ACCOUNT            VARCHAR(20),
    @NEW_SN             BIGINT          OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRAN;

        IF NOT EXISTS (SELECT 1 FROM dbo.[USER] WHERE USER_ID = @USER_ID)
            THROW 50001, 'USER_NOT_FOUND', 1;

        IF @PURCHASE_QUANTITY <= 0
            THROW 50002, 'INVALID_QUANTITY', 1;

        DECLARE @PRODUCT_NO BIGINT;

        INSERT INTO dbo.PRODUCT (PRODUCT_NAME, PRICE, FEE_RATE)
        VALUES (@PRODUCT_NAME, @PRICE, @FEE_RATE);

        SET @PRODUCT_NO = SCOPE_IDENTITY();

        DECLARE @TOTAL_FEE    DECIMAL(18, 2) = @PRICE * @FEE_RATE * @PURCHASE_QUANTITY;
        DECLARE @TOTAL_AMOUNT DECIMAL(18, 2) = @PRICE * @PURCHASE_QUANTITY + @TOTAL_FEE;

        INSERT INTO dbo.LIKE_LIST
            (USER_ID, PRODUCT_NO, PURCHASE_QUANTITY, ACCOUNT, TOTAL_FEE, TOTAL_AMOUNT)
        VALUES
            (@USER_ID, @PRODUCT_NO, @PURCHASE_QUANTITY, @ACCOUNT, @TOTAL_FEE, @TOTAL_AMOUNT);

        SET @NEW_SN = SCOPE_IDENTITY();

        COMMIT TRAN;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRAN;
        THROW;
    END CATCH
END
GO


-- =============================================================================
-- SP_LIKE_QUERY_BY_USER
--   依 USER_ID 撈出該使用者完整喜好清單（USER + PRODUCT + LIKE_LIST 三表 JOIN）
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_QUERY_BY_USER', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_QUERY_BY_USER;
GO

CREATE PROCEDURE dbo.SP_LIKE_QUERY_BY_USER
    @USER_ID VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT  L.SN,
            U.USER_ID,
            U.USER_NAME,
            U.EMAIL,
            P.NO                 AS PRODUCT_NO,
            P.PRODUCT_NAME,
            P.PRICE,
            P.FEE_RATE,
            L.PURCHASE_QUANTITY,
            L.ACCOUNT,
            L.TOTAL_FEE,
            L.TOTAL_AMOUNT
    FROM    dbo.LIKE_LIST  L
    JOIN    dbo.[USER]     U ON U.USER_ID = L.USER_ID
    JOIN    dbo.PRODUCT    P ON P.NO      = L.PRODUCT_NO
    WHERE   L.USER_ID = @USER_ID
    ORDER BY L.SN DESC;
END
GO


-- =============================================================================
-- SP_LIKE_UPDATE
--   依 SN 更新 PRODUCT（名稱/價格/費率）與 LIKE_LIST（數量/帳號），並重算金額
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_UPDATE', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_UPDATE;
GO

CREATE PROCEDURE dbo.SP_LIKE_UPDATE
    @SN                 BIGINT,
    @PRODUCT_NAME       NVARCHAR(100),
    @PRICE              DECIMAL(18, 2),
    @FEE_RATE           DECIMAL(5, 4),
    @PURCHASE_QUANTITY  INT,
    @ACCOUNT            VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRAN;

        DECLARE @PRODUCT_NO BIGINT;

        SELECT @PRODUCT_NO = PRODUCT_NO
        FROM   dbo.LIKE_LIST
        WHERE  SN = @SN;

        IF @PRODUCT_NO IS NULL
            THROW 50003, 'LIKE_NOT_FOUND', 1;

        IF @PURCHASE_QUANTITY <= 0
            THROW 50002, 'INVALID_QUANTITY', 1;

        UPDATE dbo.PRODUCT
        SET    PRODUCT_NAME = @PRODUCT_NAME,
               PRICE        = @PRICE,
               FEE_RATE     = @FEE_RATE
        WHERE  NO = @PRODUCT_NO;

        DECLARE @TOTAL_FEE    DECIMAL(18, 2) = @PRICE * @FEE_RATE * @PURCHASE_QUANTITY;
        DECLARE @TOTAL_AMOUNT DECIMAL(18, 2) = @PRICE * @PURCHASE_QUANTITY + @TOTAL_FEE;

        UPDATE dbo.LIKE_LIST
        SET    PURCHASE_QUANTITY = @PURCHASE_QUANTITY,
               ACCOUNT           = @ACCOUNT,
               TOTAL_FEE         = @TOTAL_FEE,
               TOTAL_AMOUNT      = @TOTAL_AMOUNT
        WHERE  SN = @SN;

        COMMIT TRAN;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRAN;
        THROW;
    END CATCH
END
GO


-- =============================================================================
-- SP_LIKE_DELETE
--   依 SN 刪除 LIKE_LIST，再刪除對應 PRODUCT（先子後父，避免 FK 違反）
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_DELETE', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_DELETE;
GO

CREATE PROCEDURE dbo.SP_LIKE_DELETE
    @SN BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRAN;

        DECLARE @PRODUCT_NO BIGINT;

        SELECT @PRODUCT_NO = PRODUCT_NO
        FROM   dbo.LIKE_LIST
        WHERE  SN = @SN;

        IF @PRODUCT_NO IS NULL
            THROW 50003, 'LIKE_NOT_FOUND', 1;

        DELETE FROM dbo.LIKE_LIST WHERE SN = @SN;
        DELETE FROM dbo.PRODUCT   WHERE NO = @PRODUCT_NO;

        COMMIT TRAN;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRAN;
        THROW;
    END CATCH
END
GO
