# DB — 資料庫腳本執行說明

本資料夾包含 FinancialPreference 系統的完整資料庫定義，分為三個檔案，**請依編號依序執行**。

| 編號 | 檔案 | 內容 | 重複執行 |
|------|------|------|----------|
| 01 | `01_schema.sql` | DDL：建立 `USER` / `PRODUCT` / `LIKE_LIST` 三張表、FK、Check Constraint、索引 | 安全（會先 DROP） |
| 02 | `02_stored_procedures.sql` | 4 支 Stored Procedure：`SP_LIKE_INSERT` / `SP_LIKE_QUERY_BY_USER` / `SP_LIKE_UPDATE` / `SP_LIKE_DELETE` | 安全（會先 DROP） |
| 03 | `03_seed_data.sql` | 範例資料：2 名 USER、3 個 PRODUCT、3 筆 LIKE_LIST | 安全（會先 TRUNCATE） |

---

## 1. 建立資料庫

先建立空的資料庫（**`master` 上下文**執行一次）：

```sql
IF DB_ID(N'FinancialPreference') IS NULL
    CREATE DATABASE FinancialPreference;
GO
```

---

## 2. 執行三支腳本

### 透過 `sqlcmd`（推薦，可從專案根目錄執行）

```powershell
sqlcmd -S localhost -U sa -P "$env:MSSQL_PASSWORD" -d FinancialPreference -i DB/01_schema.sql
sqlcmd -S localhost -U sa -P "$env:MSSQL_PASSWORD" -d FinancialPreference -i DB/02_stored_procedures.sql
sqlcmd -S localhost -U sa -P "$env:MSSQL_PASSWORD" -d FinancialPreference -i DB/03_seed_data.sql
```

### 透過 Docker exec（適用 Docker 部署）

```powershell
docker cp DB sql2022:/tmp/DB
docker exec -i sql2022 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$env:MSSQL_PASSWORD" -C -d FinancialPreference -i /tmp/DB/01_schema.sql
docker exec -i sql2022 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$env:MSSQL_PASSWORD" -C -d FinancialPreference -i /tmp/DB/02_stored_procedures.sql
docker exec -i sql2022 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$env:MSSQL_PASSWORD" -C -d FinancialPreference -i /tmp/DB/03_seed_data.sql
```

> `-C` 表示信任 self-signed 憑證；SQL Server 2022 Docker image 預設啟用 TLS。

### 透過 SSMS / Azure Data Studio

1. 連線到 `localhost,1433`（帳號 `sa` / 密碼 = `$env:MSSQL_PASSWORD`）
2. 切換至 `FinancialPreference` 資料庫
3. 依序開啟並執行 `01_schema.sql` → `02_stored_procedures.sql` → `03_seed_data.sql`

---

## 3. 驗證

執行完 `03_seed_data.sql` 後尾端會自動 SELECT 各表筆數，預期輸出：

| TBL | CNT |
|-----|-----|
| USER | 2 |
| PRODUCT | 3 |
| LIKE_LIST | 3 |

也可手動測試 SP：

```sql
DECLARE @NEW_SN BIGINT;
EXEC dbo.SP_LIKE_INSERT
    @USER_ID           = N'A1236456789',
    @PRODUCT_NAME      = N'玉山歐元定存',
    @PRICE             = 800.00,
    @FEE_RATE          = 0.0120,
    @PURCHASE_QUANTITY = 4,
    @ACCOUNT           = N'1111999666',
    @NEW_SN            = @NEW_SN OUTPUT;
SELECT @NEW_SN AS NEW_SN;

EXEC dbo.SP_LIKE_QUERY_BY_USER @USER_ID = N'A1236456789';
```

---

## 4. 規範重點

- **Stored Procedure only**：Java 端禁止 inline SQL；所有資料異動透過 SP 完成。
- **具名參數**：所有 SP 參數都以 `@PARAM_NAME` 命名，杜絕 SQL Injection。
- **交易控制**：跨表異動的 SP（INSERT / UPDATE / DELETE）皆以 `BEGIN TRY / BEGIN TRAN / COMMIT / ROLLBACK / THROW` 包覆，Java 層另以 `@Transactional` 雙重保障。
- **金額型別**：`PRICE` / `FEE_RATE` / `TOTAL_FEE` / `TOTAL_AMOUNT` 皆為 `DECIMAL`，禁用 `FLOAT` / `REAL`。
- **DELETE 順序**：先 `LIKE_LIST` 再 `PRODUCT`（FK 子先父後）。
- **INSERT 順序**：先 `PRODUCT` 取 `SCOPE_IDENTITY()` 再 `LIKE_LIST`。
