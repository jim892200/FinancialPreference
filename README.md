# 金融商品喜好紀錄系統 (Financial Preference)

> 玉山銀行後端工程師實作題 — Java + Spring Boot + Vue.js 三層式架構實作

提供使用者管理金融商品喜好清單的 Web 應用，支援新增、查詢、修改、刪除四項基本功能；資料庫存取全程透過 Stored Procedure，並針對 SQL Injection 與 XSS 進行防護。

---

## 目錄

- [環境需求](#環境需求)
- [安裝與執行](#安裝與執行)
- [功能需求](#功能需求)
- [系統架構](#系統架構)
- [技術棧](#技術棧)
- [專案結構](#專案結構)
- [資料庫設計](#資料庫設計)
- [API 規格](#api-規格)
- [資安防護](#資安防護)
- [測試](#測試)

---

## 環境需求

| 項目 | 版本 |
|------|------|
| JDK | 17 以上 |
| Maven | 3.9 以上 |
| Node.js | 18 以上 |
| SQL Server | 2019 以上（或 SQL Server Express / Developer Edition） |

---

## 安裝與執行

### 1. 資料庫初始化

於 SQL Server 中建立資料庫，並依序執行下列腳本：

```powershell
sqlcmd -S localhost -d FinancialPreference -i DB/01_schema.sql
sqlcmd -S localhost -d FinancialPreference -i DB/02_stored_procedures.sql
sqlcmd -S localhost -d FinancialPreference -i DB/03_seed_data.sql
```

### 2. 啟動後端

調整 `backend/src/main/resources/application.yml` 中的資料庫連線資訊，然後執行：

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

後端服務預設於 `http://localhost:8080` 提供 API。

### 3. 啟動前端

```powershell
cd frontend
npm install
npm run dev
```

前端開發模式預設於 `http://localhost:5173` 提供。

### 4. 正式打包

```powershell
# 後端：產出可執行 jar
cd backend && mvn clean package

# 前端：產出靜態檔
cd frontend && npm run build
```

---

## 功能需求

| # | 功能 | 說明 |
|---|------|------|
| 1 | 新增喜好金融商品 | 紀錄產品名稱、產品價格、手續費率、扣款帳號、購買數量 |
| 2 | 查詢喜好金融商品清單 | 顯示產品名稱清單、預計扣款總金額、總手續費用、扣款帳號、使用者聯絡電子信箱 |
| 3 | 刪除喜好金融商品 | 刪除指定的喜好紀錄 |
| 4 | 更改喜好金融商品 | 更新產品名稱、產品價格、手續費率、扣款帳號、購買數量 |

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
| 業務層 (Business) | `business/` | Service、Domain Model、交易管理、費用計算 |
| 資料層 (Data) | `data/` | Repository、Stored Procedure 呼叫、RowMapper |
| 共用層 (Common) | `common/` | 設定、共用工具、資安過濾器、例外定義 |

---

## 技術棧

| 類別 | 技術 |
|------|------|
| 前端 | Vue.js 3 + Vite + Axios |
| 後端框架 | Spring Boot 3.x (Java 17) |
| 建置工具 | Maven |
| 資料庫 | Microsoft SQL Server |
| 資料庫存取 | Stored Procedure (透過 `SimpleJdbcCall` / `JdbcTemplate`) |
| API 風格 | RESTful (JSON) |
| 驗證 | Jakarta Bean Validation |
| API 文件 | springdoc-openapi (Swagger UI) |
| 測試 | JUnit 5 + Mockito + Spring Boot Test |

---

## 專案結構

```
FinancialPreference/
├── README.md
├── .gitignore
│
├── backend/                              # Spring Boot 應用
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/esunbank/financialpreference/
│       │   │   ├── presentation/         # 展示層
│       │   │   │   ├── controller/
│       │   │   │   ├── dto/
│       │   │   │   └── advice/
│       │   │   ├── business/             # 業務層
│       │   │   │   ├── service/
│       │   │   │   ├── domain/
│       │   │   │   └── calculator/
│       │   │   ├── data/                 # 資料層
│       │   │   │   ├── repository/
│       │   │   │   └── mapper/
│       │   │   └── common/               # 共用層
│       │   │       ├── config/
│       │   │       ├── security/
│       │   │       ├── exception/
│       │   │       └── util/
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/...
│
├── frontend/                             # Vue.js 應用
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/
│       ├── views/
│       ├── components/
│       ├── router/
│       └── stores/
│
└── DB/                                   # 資料庫腳本
    ├── 01_schema.sql                     # DDL
    ├── 02_stored_procedures.sql          # Stored Procedures
    ├── 03_seed_data.sql                  # DML 範例資料
    └── README.md                         # 執行順序說明
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

範例資料：
```json
{
  "UserID": "A1236456789",
  "UserName": "王o明",
  "Email": "test@email.com",
  "Account": "1111999666"
}
```

#### `PRODUCT` 產品資料表
| 欄位 | 型別 | 說明 |
|------|------|------|
| `NO` | BIGINT PK (IDENTITY) | 產品流水號 |
| `PRODUCT_NAME` | NVARCHAR(100) | 產品名稱 |
| `PRICE` | DECIMAL(18,2) | 產品價格 |
| `FEE_RATE` | DECIMAL(5,4) | 手續費率（例：`0.1` = 10%、`0.01` = 1%） |

#### `LIKE_LIST` 喜好清單資料表
| 欄位 | 型別 | 說明 |
|------|------|------|
| `SN` | BIGINT PK (IDENTITY) | 流水序號 |
| `USER_ID` | VARCHAR(20) FK → `USER.USER_ID` | 使用者外鍵 |
| `PRODUCT_NO` | BIGINT FK → `PRODUCT.NO` | 產品外鍵 |
| `PURCHASE_QUANTITY` | INT | 購買數量 |
| `ACCOUNT` | VARCHAR(20) | 扣款帳號（下單當下的快照） |
| `TOTAL_FEE` | DECIMAL(18,2) | 總手續費 (TWD) |
| `TOTAL_AMOUNT` | DECIMAL(18,2) | 預計扣款總金額 |

### 計算公式

| 欄位 | 公式 |
|------|------|
| `TOTAL_AMOUNT` | `PRICE × PURCHASE_QUANTITY × (1 + FEE_RATE)` |
| `TOTAL_FEE` | `PRICE × PURCHASE_QUANTITY × FEE_RATE` |

> 金額一律以 `DECIMAL(18,2)` 儲存，Java 端使用 `BigDecimal` 處理，避免浮點誤差。

### Stored Procedure 一覽

| SP 名稱 | 用途 |
|---------|------|
| `SP_LIKE_INSERT` | 新增喜好（同步 INSERT PRODUCT + LIKE_LIST） |
| `SP_LIKE_QUERY_BY_USER` | 依 UserID 查詢清單（三表 JOIN） |
| `SP_LIKE_UPDATE` | 更新喜好商品與數量，重算金額 |
| `SP_LIKE_DELETE` | 刪除喜好（同步 DELETE PRODUCT + LIKE_LIST） |

涉及多表異動的 SP 內部使用 `BEGIN TRAN / COMMIT / ROLLBACK`，Java Service 層再以 `@Transactional` 包覆，雙重保障資料一致性。

---

## API 規格

Base path: `/api/v1`

### 1. 新增喜好商品
```
POST /api/v1/likes
Content-Type: application/json

{
  "userId": "A1236456789",
  "productName": "玉山美元定存",
  "price": 1000.00,
  "feeRate": 0.01,
  "account": "1111999666",
  "purchaseQuantity": 5
}
```

### 2. 查詢喜好清單
```
GET /api/v1/likes?userId=A1236456789
```

回應：
```json
{
  "code": "0000",
  "message": "success",
  "data": {
    "userName": "王o明",
    "email": "test@email.com",
    "account": "1111999666",
    "totalAmount": 5050.00,
    "totalFee": 50.00,
    "items": [
      {
        "sn": 1,
        "productName": "玉山美元定存",
        "price": 1000.00,
        "feeRate": 0.01,
        "purchaseQuantity": 5
      }
    ]
  }
}
```

### 3. 更新喜好商品
```
PUT /api/v1/likes/{sn}
```

### 4. 刪除喜好商品
```
DELETE /api/v1/likes/{sn}
```

完整 API 文件啟動後可於 `http://localhost:8080/swagger-ui.html` 檢視。

---

## 資安防護

| 威脅 | 對策 |
|------|------|
| SQL Injection | 全面採用 Stored Procedure + 具名參數綁定（`SimpleJdbcCall`），禁用任何字串拼接 SQL |
| XSS | 後端 `XssRequestFilter` 攔截 Request，對輸入字串使用 OWASP Java HTML Sanitizer 清洗；前端 Vue 預設 escape，禁用 `v-html` |
| 過量／格式異常輸入 | Jakarta Bean Validation：`@NotBlank`、`@Size`、`@DecimalMin`、`@Pattern` |
| 錯誤訊息洩漏 | `GlobalExceptionHandler` 統一包裝，僅回傳定義過的錯誤碼與訊息 |

---

## 測試

```powershell
# 後端單元測試與整合測試
cd backend
mvn test
```

測試涵蓋：
- 業務層：費用計算 (`FeeCalculator`)、Service 流程
- 資料層：Stored Procedure 呼叫
- 展示層：Controller HTTP 流程、輸入驗證

---

## 版本控制

本專案採用 Git 進行版本控制，依「資料庫 → 後端共用層 → 後端各層 → 前端 → 文件」順序循序提交，每個 commit 皆可獨立編譯。
