/* =============================================================================
   FinancialPreference - 02_stored_procedures.sql
   Stored Procedure 定義（5 支）

   - SP_LIKE_INSERT          新增喜好（INSERT PRODUCT + INSERT LIKE_LIST）
   - SP_LIKE_QUERY_BY_USER   查詢某 USER 的喜好清單（三表 JOIN）
   - SP_LIKE_UPDATE          更新產品 + 數量 + 帳號（重算金額）— 帶 ownership 檢查
   - SP_LIKE_DELETE          刪除喜好（DELETE LIKE_LIST + DELETE PRODUCT）— 帶 ownership 檢查
   - SP_USER_LOGIN_LOOKUP    依 USER_ID 撈出登入所需資訊

   規範：
     1. 全部具名參數，杜絕 SQL Injection
     2. 跨表異動均在 BEGIN TRY / BEGIN TRAN / COMMIT / ROLLBACK / THROW 包覆
     3. 金額欄位一律 DECIMAL
     4. INSERT/UPDATE 一律重算 TOTAL_FEE、TOTAL_AMOUNT

   錯誤碼：
     50001 USER_NOT_FOUND
     50002 INVALID_QUANTITY
     50003 LIKE_NOT_FOUND
     50004 FORBIDDEN          — 該 SN 不屬於請求者
     50005 ACCOUNT_MISMATCH   — @ACCOUNT 與 USER.ACCOUNT 不一致
   ============================================================================= */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO


-- =============================================================================
-- SP_LIKE_INSERT
--   INSERT PRODUCT → 取 SCOPE_IDENTITY() → INSERT LIKE_LIST
--   先驗證 @ACCOUNT 與 USER.ACCOUNT 一致，避免使用者把扣款帳號改成別人的
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

        DECLARE @USER_ACCOUNT VARCHAR(20);

        SELECT @USER_ACCOUNT = ACCOUNT
        FROM   dbo.[USER]
        WHERE  USER_ID = @USER_ID;

        IF @USER_ACCOUNT IS NULL
            THROW 50001, 'USER_NOT_FOUND', 1;

        IF @PURCHASE_QUANTITY <= 0
            THROW 50002, 'INVALID_QUANTITY', 1;

        IF @ACCOUNT <> @USER_ACCOUNT
            THROW 50005, 'ACCOUNT_MISMATCH', 1;

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
--   所有 filter 參數皆為 nullable：NULL 代表「不套用此條件」
--     @PRODUCT_NAME    LIKE '%xxx%' 模糊比對（NULL → 不過濾）
--     @ACCOUNT         完全比對
--     @AMOUNT_MIN/MAX  TOTAL_AMOUNT 區間（含端點）
--     @FEE_RATE_MIN/MAX FEE_RATE 區間（含端點）
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_QUERY_BY_USER', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_QUERY_BY_USER;
GO

CREATE PROCEDURE dbo.SP_LIKE_QUERY_BY_USER
    @USER_ID        VARCHAR(20),
    @PRODUCT_NAME   NVARCHAR(100)  = NULL,
    @ACCOUNT        VARCHAR(20)    = NULL,
    @AMOUNT_MIN     DECIMAL(18, 2) = NULL,
    @AMOUNT_MAX     DECIMAL(18, 2) = NULL,
    @FEE_RATE_MIN   DECIMAL(5, 4)  = NULL,
    @FEE_RATE_MAX   DECIMAL(5, 4)  = NULL,
    @SORT_BY        VARCHAR(20)    = 'sn',     -- sn|productName|price|feeRate|purchaseQuantity|totalFee|totalAmount
    @SORT_DIR       VARCHAR(4)     = 'desc',   -- asc | desc
    @PAGE           INT            = 1,
    @PAGE_SIZE      INT            = 10,
    @TOTAL          BIGINT         OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- 防呆與上限：避免 client 帶非法值
    IF @PAGE      IS NULL OR @PAGE      < 1   SET @PAGE      = 1;
    IF @PAGE_SIZE IS NULL OR @PAGE_SIZE < 1   SET @PAGE_SIZE = 10;
    IF @PAGE_SIZE > 200                       SET @PAGE_SIZE = 200;
    IF @SORT_DIR  IS NULL OR @SORT_DIR NOT IN ('asc', 'desc') SET @SORT_DIR = 'desc';

    -- 先把過濾後完整 row 物化到 temp，避免重算 WHERE 兩次
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
    INTO    #filtered
    FROM    dbo.LIKE_LIST  L
    JOIN    dbo.[USER]     U ON U.USER_ID = L.USER_ID
    JOIN    dbo.PRODUCT    P ON P.NO      = L.PRODUCT_NO
    WHERE   L.USER_ID = @USER_ID
      AND   (@PRODUCT_NAME IS NULL OR P.PRODUCT_NAME LIKE N'%' + @PRODUCT_NAME + N'%')
      AND   (@ACCOUNT      IS NULL OR L.ACCOUNT      = @ACCOUNT)
      AND   (@AMOUNT_MIN   IS NULL OR L.TOTAL_AMOUNT >= @AMOUNT_MIN)
      AND   (@AMOUNT_MAX   IS NULL OR L.TOTAL_AMOUNT <= @AMOUNT_MAX)
      AND   (@FEE_RATE_MIN IS NULL OR P.FEE_RATE     >= @FEE_RATE_MIN)
      AND   (@FEE_RATE_MAX IS NULL OR P.FEE_RATE     <= @FEE_RATE_MAX);

    SELECT @TOTAL = COUNT(*) FROM #filtered;

    -- 動態排序以 CASE 白名單避開字串拼接（安全）
    -- 每個欄位以 ASC / DESC 各一個 CASE 表示；無命中時為 NULL → 不影響排序
    SELECT *
    FROM   #filtered
    ORDER BY
        CASE WHEN @SORT_BY = 'sn'               AND @SORT_DIR = 'asc'  THEN SN END ASC,
        CASE WHEN @SORT_BY = 'sn'               AND @SORT_DIR = 'desc' THEN SN END DESC,
        CASE WHEN @SORT_BY = 'productName'      AND @SORT_DIR = 'asc'  THEN PRODUCT_NAME END ASC,
        CASE WHEN @SORT_BY = 'productName'      AND @SORT_DIR = 'desc' THEN PRODUCT_NAME END DESC,
        CASE WHEN @SORT_BY = 'price'            AND @SORT_DIR = 'asc'  THEN PRICE END ASC,
        CASE WHEN @SORT_BY = 'price'            AND @SORT_DIR = 'desc' THEN PRICE END DESC,
        CASE WHEN @SORT_BY = 'feeRate'          AND @SORT_DIR = 'asc'  THEN FEE_RATE END ASC,
        CASE WHEN @SORT_BY = 'feeRate'          AND @SORT_DIR = 'desc' THEN FEE_RATE END DESC,
        CASE WHEN @SORT_BY = 'purchaseQuantity' AND @SORT_DIR = 'asc'  THEN PURCHASE_QUANTITY END ASC,
        CASE WHEN @SORT_BY = 'purchaseQuantity' AND @SORT_DIR = 'desc' THEN PURCHASE_QUANTITY END DESC,
        CASE WHEN @SORT_BY = 'totalFee'         AND @SORT_DIR = 'asc'  THEN TOTAL_FEE END ASC,
        CASE WHEN @SORT_BY = 'totalFee'         AND @SORT_DIR = 'desc' THEN TOTAL_FEE END DESC,
        CASE WHEN @SORT_BY = 'totalAmount'      AND @SORT_DIR = 'asc'  THEN TOTAL_AMOUNT END ASC,
        CASE WHEN @SORT_BY = 'totalAmount'      AND @SORT_DIR = 'desc' THEN TOTAL_AMOUNT END DESC,
        SN DESC   -- 最後的 tiebreaker，保證頁與頁之間順序穩定
    OFFSET (@PAGE - 1) * @PAGE_SIZE ROWS
    FETCH NEXT @PAGE_SIZE ROWS ONLY;
END
GO


-- =============================================================================
-- SP_LIKE_UPDATE
--   依 SN 更新 PRODUCT（名稱/價格/費率）與 LIKE_LIST（數量/帳號），並重算金額
--   @USER_ID 用於 ownership 檢查，避免越權修改他人喜好（IDOR）
--   @ACCOUNT 必須與 USER.ACCOUNT 一致
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_UPDATE', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_UPDATE;
GO

CREATE PROCEDURE dbo.SP_LIKE_UPDATE
    @SN                 BIGINT,
    @USER_ID            VARCHAR(20),
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
        DECLARE @OWNER_ID   VARCHAR(20);

        SELECT @PRODUCT_NO = PRODUCT_NO,
               @OWNER_ID   = USER_ID
        FROM   dbo.LIKE_LIST
        WHERE  SN = @SN;

        IF @PRODUCT_NO IS NULL
            THROW 50003, 'LIKE_NOT_FOUND', 1;

        IF @OWNER_ID <> @USER_ID
            THROW 50004, 'FORBIDDEN', 1;

        IF @PURCHASE_QUANTITY <= 0
            THROW 50002, 'INVALID_QUANTITY', 1;

        DECLARE @USER_ACCOUNT VARCHAR(20);
        SELECT @USER_ACCOUNT = ACCOUNT FROM dbo.[USER] WHERE USER_ID = @USER_ID;

        IF @ACCOUNT <> @USER_ACCOUNT
            THROW 50005, 'ACCOUNT_MISMATCH', 1;

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
--   @USER_ID 用於 ownership 檢查（IDOR 防護）
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_LIKE_DELETE', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_LIKE_DELETE;
GO

CREATE PROCEDURE dbo.SP_LIKE_DELETE
    @SN         BIGINT,
    @USER_ID    VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRAN;

        DECLARE @PRODUCT_NO BIGINT;
        DECLARE @OWNER_ID   VARCHAR(20);

        SELECT @PRODUCT_NO = PRODUCT_NO,
               @OWNER_ID   = USER_ID
        FROM   dbo.LIKE_LIST
        WHERE  SN = @SN;

        IF @PRODUCT_NO IS NULL
            THROW 50003, 'LIKE_NOT_FOUND', 1;

        IF @OWNER_ID <> @USER_ID
            THROW 50004, 'FORBIDDEN', 1;

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


-- =============================================================================
-- SP_USER_LOGIN_LOOKUP
--   依 USER_ID 撈出登入驗證所需資料（PASSWORD_HASH + 基本資訊）
--   BCrypt 驗證由 Java 層完成
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_USER_LOGIN_LOOKUP', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_USER_LOGIN_LOOKUP;
GO

CREATE PROCEDURE dbo.SP_USER_LOGIN_LOOKUP
    @USER_ID VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT  USER_ID,
            USER_NAME,
            EMAIL,
            ACCOUNT,
            PASSWORD_HASH
    FROM    dbo.[USER]
    WHERE   USER_ID = @USER_ID;
END
GO


-- =============================================================================
-- SP_USER_LIST_PENDING_PASSWORDS
--   列出所有 PASSWORD_HASH 為空的 USER_ID（供首次啟動 seed BCrypt hash）
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_USER_LIST_PENDING_PASSWORDS', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_USER_LIST_PENDING_PASSWORDS;
GO

CREATE PROCEDURE dbo.SP_USER_LIST_PENDING_PASSWORDS
AS
BEGIN
    SET NOCOUNT ON;

    SELECT  USER_ID
    FROM    dbo.[USER]
    WHERE   PASSWORD_HASH IS NULL
       OR   PASSWORD_HASH = '';
END
GO


-- =============================================================================
-- SP_USER_SET_PASSWORD_HASH
--   為指定 USER 寫入 BCrypt hash（僅在原本為空時更新，避免覆寫已設定的密碼）
-- =============================================================================
IF OBJECT_ID(N'dbo.SP_USER_SET_PASSWORD_HASH', N'P') IS NOT NULL
    DROP PROCEDURE dbo.SP_USER_SET_PASSWORD_HASH;
GO

CREATE PROCEDURE dbo.SP_USER_SET_PASSWORD_HASH
    @USER_ID        VARCHAR(20),
    @PASSWORD_HASH  VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.[USER]
    SET    PASSWORD_HASH = @PASSWORD_HASH
    WHERE  USER_ID = @USER_ID
      AND  (PASSWORD_HASH IS NULL OR PASSWORD_HASH = '');
END
GO
