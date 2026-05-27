/* =============================================================================
   FinancialPreference - 01_schema.sql
   建立資料表：USER / PRODUCT / LIKE_LIST
   執行前提：資料庫 FinancialPreference 已存在；以該資料庫上下文執行此腳本。
   ============================================================================= */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- -----------------------------------------------------------------------------
-- 依 FK 反向順序卸除舊表（重跑安全）
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.LIKE_LIST', N'U') IS NOT NULL DROP TABLE dbo.LIKE_LIST;
IF OBJECT_ID(N'dbo.PRODUCT',   N'U') IS NOT NULL DROP TABLE dbo.PRODUCT;
IF OBJECT_ID(N'dbo.[USER]',    N'U') IS NOT NULL DROP TABLE dbo.[USER];
GO

-- -----------------------------------------------------------------------------
-- USER：使用者
--   PASSWORD_HASH 為 BCrypt（含 $2a$10$ 前綴），由後端啟動時 Seeder 填入。
-- -----------------------------------------------------------------------------
CREATE TABLE dbo.[USER] (
    USER_ID         VARCHAR(20)   NOT NULL,
    USER_NAME       NVARCHAR(50)  NOT NULL,
    EMAIL           VARCHAR(100)  NOT NULL,
    ACCOUNT         VARCHAR(20)   NOT NULL,
    PASSWORD_HASH   VARCHAR(100)  NOT NULL CONSTRAINT DF_USER_PWD DEFAULT '',
    CONSTRAINT PK_USER PRIMARY KEY CLUSTERED (USER_ID)
);
GO

-- -----------------------------------------------------------------------------
-- PRODUCT：金融商品
--   NO 為 IDENTITY，由系統產生
-- -----------------------------------------------------------------------------
CREATE TABLE dbo.PRODUCT (
    NO              BIGINT          IDENTITY(1,1) NOT NULL,
    PRODUCT_NAME    NVARCHAR(100)   NOT NULL,
    PRICE           DECIMAL(18, 2)  NOT NULL,
    FEE_RATE        DECIMAL(5, 4)   NOT NULL,
    CONSTRAINT PK_PRODUCT PRIMARY KEY CLUSTERED (NO),
    CONSTRAINT CK_PRODUCT_PRICE     CHECK (PRICE    >= 0),
    CONSTRAINT CK_PRODUCT_FEE_RATE  CHECK (FEE_RATE >= 0 AND FEE_RATE <= 1)
);
GO

-- -----------------------------------------------------------------------------
-- LIKE_LIST：使用者喜好清單
--   SN 為 IDENTITY
--   USER_ID、PRODUCT_NO 為外鍵；ACCOUNT 為下單當下的扣款帳號快照
-- -----------------------------------------------------------------------------
CREATE TABLE dbo.LIKE_LIST (
    SN                  BIGINT          IDENTITY(1,1) NOT NULL,
    USER_ID             VARCHAR(20)     NOT NULL,
    PRODUCT_NO          BIGINT          NOT NULL,
    PURCHASE_QUANTITY   INT             NOT NULL,
    ACCOUNT             VARCHAR(20)     NOT NULL,
    TOTAL_FEE           DECIMAL(18, 2)  NOT NULL,
    TOTAL_AMOUNT        DECIMAL(18, 2)  NOT NULL,
    CONSTRAINT PK_LIKE_LIST PRIMARY KEY CLUSTERED (SN),
    CONSTRAINT FK_LIKE_LIST_USER
        FOREIGN KEY (USER_ID)    REFERENCES dbo.[USER](USER_ID),
    CONSTRAINT FK_LIKE_LIST_PRODUCT
        FOREIGN KEY (PRODUCT_NO) REFERENCES dbo.PRODUCT(NO),
    CONSTRAINT CK_LIKE_LIST_QTY          CHECK (PURCHASE_QUANTITY > 0),
    CONSTRAINT CK_LIKE_LIST_TOTAL_FEE    CHECK (TOTAL_FEE     >= 0),
    CONSTRAINT CK_LIKE_LIST_TOTAL_AMOUNT CHECK (TOTAL_AMOUNT  >= 0)
);
GO

-- -----------------------------------------------------------------------------
-- 索引（依 SP_LIKE_QUERY_BY_USER 的查詢 pattern）
-- -----------------------------------------------------------------------------
CREATE NONCLUSTERED INDEX IX_LIKE_LIST_USER_ID    ON dbo.LIKE_LIST(USER_ID);
CREATE NONCLUSTERED INDEX IX_LIKE_LIST_PRODUCT_NO ON dbo.LIKE_LIST(PRODUCT_NO);
GO
