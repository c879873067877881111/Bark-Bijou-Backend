# Bark Bijou API 開發指南

## 快速開始

### 環境要求
- **Java 21+** (推薦使用 OpenJDK 21)
- **Docker & Docker Compose** (用於數據庫服務)
- **Maven 3.6+** (項目構建工具)

### 本地開發環境設置

#### 1. 啟動基礎服務
```bash
# 啟動 PostgreSQL + Redis
docker-compose up -d

# 確認服務狀態
docker-compose ps
```

#### 2. 啟動應用
```bash
# 開發模式 (自動重載)
./mvnw spring-boot:run

# 指定開發配置文件
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 後台啟動
nohup ./mvnw spring-boot:run &
```

#### 3. 驗證部署
- **API Base URL**: http://localhost:8080/api
- **API 文檔**: http://localhost:8080/swagger-ui.html
- **健康檢查**: http://localhost:8080/actuator/health

### 數據庫初始化

#### PostgreSQL 設置
```bash
# 連接數據庫
docker exec -it postgres psql -U user -d bark_bijou

# 查看表結構
\d+ member
\d+ product
\d+ orders
```

#### Redis 設置
```bash
# 連接 Redis
docker exec -it redis redis-cli

# 查看 Session 數據
keys "spring:session:*"
```

## 開發工作流

### 代碼結構說明
```
src/main/java/com/smallnine/apiserver/
├── controller/     # REST API 控制器
│   ├── auth/       # 認證相關 API
│   ├── product/    # 商品管理 API  
│   ├── order/      # 訂單管理 API
│   └── cart/       # 購物車 API
├── service/        # 業務邏輯層
│   ├── impl/       # 服務實現
│   └── intf/       # 服務接口 (待完善)
├── dao/            # 數據訪問層
├── entity/         # 實體模型
├── dto/            # 數據傳輸對象
├── config/         # 配置類
├── filter/         # 過濾器 (JWT 等)
├── interceptor/    # 攔截器 (日誌等)
├── utils/          # 工具類
└── exception/      # 自定義異常
```

### 開發規範

#### API 設計規範
```java
// 控制器示例
@RestController
@RequestMapping("/api/products")
@Api(tags = "商品管理")
public class ProductController {
    
    @PostMapping
    @ApiOperation("創建商品")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        // 業務邏輯
        return ApiResponse.success(productService.createProduct(request));
    }
}
```

#### 日誌規範
```java
// ELK 友好的日誌格式
log.info("action=create_product user_id={} product_id={} result=success", 
         userId, productId);

log.warn("action=create_product user_id={} result=failed reason=invalid_category", 
         userId);
```

#### 異常處理
```java
// 使用統一異常處理
@Component
public class ProductServiceImpl {
    
    public ProductResponse createProduct(CreateProductRequest request) {
        if (!categoryDao.existsById(request.getCategoryId())) {
            throw new BusinessException("分類不存在");
        }
        // 業務邏輯
    }
}
```

### 測試指南

#### 運行測試
```bash
# 所有測試
./mvnw test

# 特定測試類
./mvnw test -Dtest=AuthServiceTest

# 集成測試
./mvnw test -Dtest=ECommerceIntegrationTest

# 測試覆蓋率報告
./mvnw jacoco:report
# 報告位置: target/site/jacoco/index.html
```

#### 測試數據庫
```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.org.springframework.web=DEBUG
```

### API 測試

#### 使用 curl 測試
```bash
# 用戶註冊
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 用戶登入  
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser","password":"password123"}'

# 獲取商品列表
curl http://localhost:8080/api/products?page=0&size=10
```

#### 使用 Postman
1. 導入 OpenAPI 規範: http://localhost:8080/v3/api-docs
2. 設置環境變量: `baseUrl=http://localhost:8080/api`
3. 配置 JWT Token 自動刷新

## 調試指南

### 日誌查看
```bash
# 實時查看應用日誌
tail -f logs/api-server.log

# 過濾特定用戶操作
grep "user_id=123" logs/api-server.log

# 查看錯誤日誌
grep "ERROR\|result=failed" logs/api-server.log
```

### 數據庫調試
```sql
-- 查看當前活動會話
SELECT * FROM pg_stat_activity WHERE datname = 'bark_bijou';

-- 查看慢查詢
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;
```

### 性能監控
```bash
# JVM 監控
jconsole localhost:8080

# 堆內存分析
jmap -histo <pid>

# GC 日誌
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-XX:+PrintGC"
```

## 常見問題

### Q: 應用啟動失敗
```
解決方法:
1. 確認 PostgreSQL 和 Redis 服務已啟動
   docker-compose ps
   
2. 檢查端口占用
   lsof -i :8080
   lsof -i :5432
   lsof -i :6379
   
3. 查看詳細錯誤日誌
   ./mvnw spring-boot:run --debug
```

### Q: JWT Token 過期問題
```
解決方法:
1. 檢查系統時間是否正確
2. 使用 Refresh Token 刷新 Access Token
3. 前端實現自動 Token 刷新邏輯 (參考 REFRESH_TOKEN.md)
```

### Q: 數據庫連接失敗
```
解決方法:
1. 檢查 docker-compose.yml 配置
2. 確認數據庫用戶名密碼
3. 查看 PostgreSQL 日誌
   docker logs postgres
```

### Q: CORS 錯誤
```
解決方法:
1. 檢查前端域名是否在允許列表中
2. 確認 WebConfig 中的 CORS 配置
3. 瀏覽器開發者工具查看具體錯誤
```

## 部署指南

### 本地部署
```bash
# 打包應用
./mvnw clean package -DskipTests

# 運行 JAR 包
java -jar target/api-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker 部署
```bash
# 構建鏡像
docker build -t bark-bijou-api .

# 運行容器
docker run -d \
  -p 8080:8080 \
  --link postgres:postgres \
  --link redis:redis \
  -e SPRING_PROFILES_ACTIVE=prod \
  bark-bijou-api
```

## 相關文檔

### 項目文檔
- [**README.md**](./README.md) - 項目概覽和快速開始
- [**CODE_REVIEW_GUIDE.md**](./CODE_REVIEW_GUIDE.md) - 代碼審查標準
- [**LOGGING_STANDARDS.md**](./LOGGING_STANDARDS.md) - ELK 友好日誌規範
- [**REFRESH_TOKEN.md**](./REFRESH_TOKEN.md) - JWT 前端整合指南

### 外部參考
- [Spring Boot 官方文檔](https://docs.spring.io/spring-boot/docs/3.5.7/reference/)
- [MyBatis 官方文檔](https://mybatis.org/mybatis-3/)
- [PostgreSQL 文檔](https://www.postgresql.org/docs/15/)
- [Redis 文檔](https://redis.io/documentation)

### 線上資源
- **API 文檔**: http://localhost:8080/swagger-ui.html
- **健康檢查**: http://localhost:8080/actuator/health
- **應用信息**: http://localhost:8080/actuator/info

## 團隊協作

### Git 工作流
```bash
# 創建功能分支
git checkout -b feature/add-product-search

# 提交代碼
git add .
git commit -m "feat: 添加商品搜索功能

- 實現關鍵字搜索
- 支持價格區間過濾
- 添加相關測試用例"

# 推送並創建 PR
git push origin feature/add-product-search
```

### Code Review 流程
1. **提交前自檢**: 運行測試、格式化代碼
2. **創建 PR**: 填寫完整的描述和測試清單
3. **等待審查**: 至少一位 Senior 開發者 LGTM
4. **修改反饋**: 根據 Review 意見修改代碼
5. **合併主分支**: 確保 CI 檢查通過

### 版本發布
```bash
# 創建發布分支
git checkout -b release/v1.2.0

# 更新版本號
./mvnw versions:set -DnewVersion=1.2.0

# 創建標籤
git tag v1.2.0
git push origin v1.2.0
```

---

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.7/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.7/maven-plugin/build-image.html)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.