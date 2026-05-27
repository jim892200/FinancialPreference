# 金融商品喜好紀錄系統 (Financial Preference)

提供使用者管理金融商品喜好清單的 Web 應用，支援新增、查詢、修改、刪除四項基本功能；資料庫存取全程透過 Stored Procedure，並針對 SQL Injection 與 XSS 進行防護。

---

## 目錄

- [環境需求](#環境需求)
- [快速開始（Docker）](#快速開始docker)
- [安裝與執行（手動）](#安裝與執行手動)
- [功能需求](#功能需求)
- [系統架構](#系統架構)
- [技術棧](#技術棧)
- [專案結構](#專案結構)
- [資料庫設計](#資料庫設計)
- [API 規格](#api-規格)
- [資安防護](#資安防護)
- [測試與覆蓋率](#測試與覆蓋率)

---

## 環境需求

| 項目 | 版本 | 備註 |
|------|------|------|
| JDK | 17 以上 | 後端編譯 / 執行 |
| Maven | — | **內附 `mvnw` Wrapper，無需另裝** |
| Node.js | 18 以上 | 前端 dev / build |
| MS SQL Server | 2019 以上 | 或用 Docker（推薦） |

---

## 快速開始（Docker）

**一條指令拉起整個系統**（DB + 後端 + 前端）：

```powershell
docker compose up -d --build
```

`docker-compose.yml` 已串好啟動順序：

| Service | 角色 | Port |
|---------|------|------|
| `sql2022` | MS SQL Server 2022 | `1433` |
| `db-init` | 一次性建立 DB / 載入 schema / SP / seed（idempotent，已存在就跳過） | — |
| `backend` | Spring Boot REST API（含 actuator healthcheck） | `8080` |
| `frontend` | Nginx 提供 Vue build，並 reverse-proxy `/api` 至 backend | `5173` (對外) → `80` (內部) |

依賴順序：`sql2022` healthy → `db-init` 完成 → `backend` healthy → `frontend` 啟動。

啟動後可直接使用：

| 服務 | 網址 |
|------|------|
| 前端（SPA） | http://localhost:5173 |
| REST API | http://localhost:8080/api/v1/likes |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Actuator Health | http://localhost:8080/actuator/health |

### 自訂 sa 密碼

預設密碼為 `YourStrong@Passw0rd`。要改：

```powershell
$env:MSSQL_SA_PASSWORD = "<你的密碼>"
docker compose up -d --build
```

或於專案根目錄新增 `.env` 檔：
```
MSSQL_SA_PASSWORD=<你的密碼>
```

### 重新初始化資料

```powershell
docker compose down -v          # -v 一併清掉 mssql-data volume
docker compose up -d --build
```

### 只看後端 log

```powershell
docker compose logs -f backend
```

---

## 安裝與執行（手動）

### 1. 資料庫初始化

於 SQL Server 中建立資料庫，並依序執行下列腳本：

```powershell
sqlcmd -S localhost -U sa -P "<your-password>" -Q "IF DB_ID(N'FinancialPreference') IS NULL CREATE DATABASE FinancialPreference;"

sqlcmd -S localhost -U sa -P "<your-password>" -d FinancialPreference -i DB/01_schema.sql
sqlcmd -S localhost -U sa -P "<your-password>" -d FinancialPreference -i DB/02_stored_procedures.sql
sqlcmd -S localhost -U sa -P "<your-password>" -d FinancialPreference -i DB/03_seed_data.sql
```

詳細執行說明見 [`DB/README.md`](DB/README.md)。

### 2. 啟動後端

後端從 `MSSQL_PASSWORD` 環境變數讀取 sa 密碼（可避免硬編碼）：

```powershell
$env:MSSQL_PASSWORD = "YourStrong@Passw0rd"

cd backend
./mvnw spring-boot:run
```

或先打包再執行 jar：

```powershell
cd backend
./mvnw -DskipTests package
java -jar target/financial-preference-0.0.1-SNAPSHOT.jar
```

後端服務：

| 服務 | 網址 |
|------|------|
| REST API | http://localhost:8080/api/v1/likes |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Actuator Health | http://localhost:8080/actuator/health |

### 3. 啟動前端

```powershell
cd frontend
npm install
npm run dev
```

開啟 http://localhost:5173 即可看到列表頁。Vite dev server 已設定 `/api/**` 反向代理至 `localhost:8080`。

### 4. 正式打包

```powershell
# 後端：產出可執行 jar
cd backend
./mvnw -DskipTests package

# 前端：產出靜態檔（給 Nginx 之類的 Web Server）
cd ../frontend
npm run build
```

> Windows PowerShell 5.1 不支援 `&&` / `||`，故指令分行撰寫；若使用 PowerShell 7+ 或 bash，可改用 `cd backend && ./mvnw -DskipTests package` 串接。

---

## 功能需求

| # | 功能 | 說明 |
|---|------|------|
| 1 | 新增喜好金融商品 | 紀錄產品名稱、產品價格、手續費率、扣款帳號、購買數量 |
| 2 | 查詢喜好金融商品清單 | 顯示產品名稱清單、預計扣款總金額、總手續費用、扣款帳號、使用者聯絡電子信箱 |
| 3 | 刪除喜好金融商品 | 刪除指定的喜好紀錄與對應產品 |
| 4 | 更改喜好金融商品 | 更新產品名稱、產品價格、手續費率、扣款帳號、購買數量，並重算金額 |

---

## 系統架構

採用 **Web Server + Application Server + RDBMS** 三層式架構：

```
┌─────────────────┐      ┌──────────────────────┐      ┌────────────────┐
│  Web Server     │ HTTP │ Application Server   │ JDBC │  RDBMS         │
│  Vue.js (Nginx) │ ───► │ Spring Boot REST API │ ───► │  SQL Server    │
└─────────────────┘      └──────────────────────┘      │  + Stored Proc │
                                                       └────────────────┘
```

後端依需求區分為四層：

| 分層 | 套件 | 職責 |
|------|------|------|
| 展示層 (Presentation) | `presentation/` | Controller、DTO、輸入驗證、全域例外處理 |
| 業務層 (Business) | `business/` | Service、Domain Model、Command、交易管理、費用計算 |
| 資料層 (Data) | `data/` | Repository（SimpleJdbcCall）、RowMapper |
| 共用層 (Common) | `common/` | 設定、共用工具、資安過濾器、例外定義 |

---

## 技術棧

| 類別 | 技術 |
|------|------|
| 前端 | Vue.js 3.5 + Vite 8 + Axios + vue-router 4 |
| 後端框架 | Spring Boot 3.5.0（Java 17） |
| 建置工具 | Maven（內附 mvnw Wrapper） |
| 資料庫 | Microsoft SQL Server 2022 |
| 資料庫存取 | Stored Procedure（透過 `SimpleJdbcCall` 具名參數綁定） |
| API 風格 | RESTful (JSON) |
| 驗證 | Jakarta Bean Validation（Hibernate Validator） |
| 資安 | OWASP Java HTML Sanitizer + Jackson 自訂反序列化器 |
| API 文件 | springdoc-openapi 2.8（OpenAPI 3.1） |
| 測試 | JUnit 5 + Mockito + Spring Boot Test |
| 覆蓋率 | JaCoCo 0.8 |

---

## 專案結構

```
FinancialPreference/
├── README.md
├── .gitignore
├── docker-compose.yml                  # MSSQL 一鍵起
│
├── backend/                            # Spring Boot 應用
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd                 # Maven Wrapper
│   └── src/
│       ├── main/
│       │   ├── java/com/esunbank/financialpreference/
│       │   │   ├── presentation/       # 展示層
│       │   │   │   ├── controller/     #   LikeListController
│       │   │   │   ├── dto/            #   ApiResponse / request / response
│       │   │   │   └── advice/         #   GlobalExceptionHandler
│       │   │   ├── business/           # 業務層
│       │   │   │   ├── service/        #   LikeListService (@Transactional)
│       │   │   │   ├── command/        #   CreateLikeCommand / UpdateLikeCommand
│       │   │   │   ├── domain/         #   User / Product / LikeItem
│       │   │   │   └── calculator/     #   FeeCalculator
│       │   │   ├── data/               # 資料層
│       │   │   │   ├── repository/     #   LikeListRepository (SimpleJdbcCall)
│       │   │   │   └── mapper/         #   LikeItemRowMapper
│       │   │   └── common/             # 共用層
│       │   │       ├── config/         #   WebMvcConfig / OpenApiConfig / JacksonXssConfig
│       │   │       ├── security/       #   HtmlSanitizer / SanitizingRequestWrapper / XssRequestFilter
│       │   │       ├── exception/      #   ErrorCode / BusinessException
│       │   │       └── util/           #   MoneyUtil / Constants
│       │   └── resources/
│       │       ├── application.yml
│       │       └── application-dev.yml
│       └── test/java/...               # 47 個測試
│
├── frontend/                           # Vue.js 應用
│   ├── package.json
│   ├── vite.config.js                  # /api 反向代理至 :8080
│   └── src/
│       ├── api/likeListApi.js          # axios 封裝
│       ├── views/                      # LikeListView / LikeFormView
│       ├── router/index.js
│       ├── App.vue
│       └── main.js
│
└── DB/                                 # 資料庫腳本
    ├── 01_schema.sql                   # DDL（含 FK / CHECK）
    ├── 02_stored_procedures.sql        # 4 支 SP
    ├── 03_seed_data.sql                # DML 範例資料（2 USER / 3 PRODUCT / 3 LIKE）
    └── README.md                       # 執行方式（sqlcmd / docker exec / SSMS）
```

---

## 資料庫設計

### 資料表

#### `USER` 使用者資料表
| 欄位 | 型別 | 說明 |
|------|------|------|
| `USER_ID` | VARCHAR(20) PK | 使用者 ID |
| `USER_NAME` | NVARCHAR(50) | 使用者名稱 |
| `EMAIL` | VARCHAR(100) | 電子郵件 |
| `ACCOUNT` | VARCHAR(20) | 扣款帳號 |

#### `PRODUCT` 產品資料表
| 欄位 | 型別 | 說明 |
|------|------|------|
| `NO` | BIGINT PK (IDENTITY) | 產品流水號 |
| `PRODUCT_NAME` | NVARCHAR(100) | 產品名稱 |
| `PRICE` | DECIMAL(18,2) | 產品價格 |
| `FEE_RATE` | DECIMAL(5,4) | 手續費率（例：`0.0100` = 1%、`0.1000` = 10%） |

#### `LIKE_LIST` 喜好清單資料表
| 欄位 | 型別 | 說明 |
|------|------|------|
| `SN` | BIGINT PK (IDENTITY) | 流水序號 |
| `USER_ID` | VARCHAR(20) FK → `USER.USER_ID` | 使用者外鍵 |
| `PRODUCT_NO` | BIGINT FK → `PRODUCT.NO` | 產品外鍵 |
| `PURCHASE_QUANTITY` | INT | 購買數量（> 0） |
| `ACCOUNT` | VARCHAR(20) | 扣款帳號（下單當下的快照） |
| `TOTAL_FEE` | DECIMAL(18,2) | 總手續費 (TWD) |
| `TOTAL_AMOUNT` | DECIMAL(18,2) | 預計扣款總金額 |

### 計算公式

| 欄位 | 公式 |
|------|------|
| `TOTAL_FEE` | `PRICE × FEE_RATE × PURCHASE_QUANTITY` |
| `TOTAL_AMOUNT` | `PRICE × PURCHASE_QUANTITY + TOTAL_FEE` |

> 金額一律以 `DECIMAL(18,2)` 儲存，Java 端使用 `BigDecimal` 處理，避免浮點誤差。

### Stored Procedure 一覽

| SP 名稱 | 用途 | 涉及資料表 |
|---------|------|----------|
| `SP_LIKE_INSERT` | 新增喜好（INSERT PRODUCT 取 SCOPE_IDENTITY 後 INSERT LIKE_LIST） | PRODUCT、LIKE_LIST |
| `SP_LIKE_QUERY_BY_USER` | 依 UserID 查詢清單（三表 JOIN） | USER、PRODUCT、LIKE_LIST |
| `SP_LIKE_UPDATE` | 更新產品與數量，重算金額 | PRODUCT、LIKE_LIST |
| `SP_LIKE_DELETE` | 刪除喜好（先 LIKE_LIST 再 PRODUCT，FK 順序） | PRODUCT、LIKE_LIST |

所有 SP 均以**具名參數**呼叫；跨表異動的 SP 內部含 `BEGIN TRY / BEGIN TRAN / COMMIT / ROLLBACK / THROW`，Java Service 層再以 `@Transactional(rollbackFor = Exception.class)` 包覆，**雙重保障**資料一致性。

### 預設 Seed 資料

| USER_ID | USER_NAME | EMAIL |
|---------|-----------|-------|
| A1236456789 | 王o明 | test@email.com |
| B9876543210 | 陳o華 | chen@example.com |

A 使用者 2 筆喜好、B 使用者 1 筆。

---

## API 規格

Base path: `/api/v1`

統一回應殼：
```json
{ "code": "0000", "message": "success", "data": ... }
```

### 1. 新增喜好商品 `POST /api/v1/likes`

請求：
```json
{
  "userId": "A1236456789",
  "productName": "美元定存",
  "price": 1000.00,
  "feeRate": 0.0100,
  "purchaseQuantity": 5,
  "account": "1111999666"
}
```

回應（**HTTP 201**，`data` 為新增的 SN）：
```json
{ "code": "0000", "message": "success", "data": 7 }
```

### 2. 查詢喜好清單 `GET /api/v1/likes?userId={id}`

回應（`data` 為扁平陣列）：
```json
{
  "code": "0000",
  "message": "success",
  "data": [
    {
      "sn": 2,
      "userId": "A1236456789",
      "userName": "王o明",
      "email": "test@email.com",
      "productNo": 2,
      "productName": "日圓基金",
      "price": 500.00,
      "feeRate": 0.0150,
      "purchaseQuantity": 10,
      "account": "1111999666",
      "totalFee": 75.00,
      "totalAmount": 5075.00
    }
  ]
}
```

### 3. 更新喜好商品 `PUT /api/v1/likes/{sn}`

請求同 POST，但**不含 `userId`**（不可改）。回應 `data: null`。

### 4. 刪除喜好商品 `DELETE /api/v1/likes/{sn}`

無 body。回應 `data: null`。

### 錯誤碼

| code | HTTP | 觸發情境 |
|------|------|----------|
| `0000` | 200 / 201 | 成功 |
| `4000` | 400 | Bean Validation 失敗（空字串、超長、負數、quantity = 0…） |
| `4001` | 400 | quantity 不合法 |
| `4040` | 404 | USER_ID 不存在 |
| `4041` | 404 | SN 不存在 |
| `5000` | 500 | 未預期錯誤（不洩漏 stack trace） |

完整 API 文件啟動後可於 http://localhost:8080/swagger-ui/index.html 互動式查看。

---

## 資安防護

| 威脅 | 對策 | 實作 |
|------|------|------|
| **SQL Injection** | 全面採用 Stored Procedure + 具名參數綁定，禁用任何字串拼接 SQL | `SimpleJdbcCall` + `MapSqlParameterSource.addValue(name, value, Types.X)` → JDBC PreparedStatement `?` 佔位符 |
| **XSS（JSON body）** | OWASP Java HTML Sanitizer 在反序列化階段清洗 String 欄位 | `JacksonXssConfig` 註冊全域 String `JsonDeserializer` |
| **XSS（query / form）** | HttpServletRequestWrapper 清洗所有參數 | `XssRequestFilter` + `SanitizingRequestWrapper` |
| **過量／格式異常輸入** | Jakarta Bean Validation | `@NotBlank` / `@Size` / `@DecimalMin` / `@DecimalMax` / `@Digits` / `@Min` |
| **錯誤訊息洩漏** | 統一例外處理 + 隱藏 stack trace | `GlobalExceptionHandler` + `server.error.include-stacktrace: never` |
| **DB 密碼硬編碼** | 環境變數注入 | `MSSQL_PASSWORD` env，未提供時連線失敗 |

> XSS 清洗發生在 **Bean Validation 之前**，所以 `productName = "<script>alert(1)</script>"` 會先清洗為空字串，再被 `@NotBlank` 擋下回 400，根本不會進入 Service。

---

## 測試與覆蓋率

```powershell
cd backend
./mvnw test
```

執行結果：

| 測試類 | 個數 | 範圍 |
|--------|------|------|
| `MoneyUtilTest` | 5 | BigDecimal 精度與進位 |
| `HtmlSanitizerTest` | 7 | XSS 清洗策略 |
| `SanitizingRequestWrapperTest` | 5 | Query/Form param 清洗 |
| `FeeCalculatorTest` | 4 | 金額重算公式 |
| `LikeListServiceTest` | 7 | Service + SP 錯誤碼轉譯 |
| `LikeListControllerTest` | 12 | 4 endpoints × happy / 驗證 / 業務錯誤 |
| `XssIntegrationTest` | 3 | End-to-end XSS 攔截 |
| `LikeListRepositoryIntegrationTest` | 3 | 對真 DB round-trip |
| `FinancialPreferenceApplicationTests` | 1 | Spring context load |
| **小計** | **47** | |

> Repository 集成測試以 `@EnabledIfEnvironmentVariable(MSSQL_PASSWORD)` 控管 — 未設定密碼時自動 skip，方便無 DB 環境 build。

### JaCoCo 覆蓋率

| 指標 | 比例 |
|------|------|
| Instruction | 92.1% |
| Line | 92.6% |
| Branch | 70.0% |

報告位置：`backend/target/site/jacoco/index.html`（執行 `./mvnw test` 後產生）。
