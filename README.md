# Bark Bijou 電商 API 服務

Spring Boot 3.5.7 + PostgreSQL + Redis 的電商 API 網站

## 技術棧
- **後端**: Spring Boot 3.5.7 (Java 21) + MyBatis
- **資料庫**: PostgreSQL 15 + Redis 7  
- **認證**: Spring Security + JWT
- **文件檔案**: OpenAPI 3 + Swagger UI

## 啟動

### 環境要求
- Java 21+
- Docker & Docker Compose  
- Maven 3.6+

### 啟動步驟
```bash
# 1. Clone 專案
git clone https://github.com/c879873067877881111/Bark-Bijou-backend.git
cd api-server

# 2. 啟動資料庫服務
docker-compose up -d

# 3. 啟動應用
./mvnw spring-boot:run
```

### 網址
- **API 服務**: http://localhost:8080/api
- **API 文檔**: http://localhost:8080/swagger-ui.html  
- **健康檢查**: http://localhost:8080/actuator/health

## 核心功能
- 用戶註冊/登入 (JWT Token 認證)
- 商品管理 (分類、品牌、庫存)
- 購物車管理
- 訂單處理
- RESTful API 設計

## 文檔
- [API 文件檔案](docs/api.md) - 詳細的 API 說明
- [部署指南](docs/deployment.md) - Docker/雲端部署配置  
- [開發指南](docs/development.md) - 本地開發和程式碼規範

## 相關文檔
- [JWT Token 機制](REFRESH_TOKEN.md)
- [日誌規範](LOGGING_STANDARDS.md)
