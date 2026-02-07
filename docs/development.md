# 開發指南

## 環境設置

### 開發環境要求
- Java 21+
- Maven 3.6+
- Docker & Docker Compose
- IDE: IntelliJ IDEA 或 Visual Studio Code

### 本地開發設置

1. **Clone 專案**
```bash
git clone <repository-url>
cd api-server
```

2. **啟動基礎服務**
```bash
docker-compose up -d postgres redis
```

3. **配置 IDE**
- 匯入 Maven 專案
- 設置 JDK 21
- 安裝必要插件:Spring Boot, MyBatis

4. **運行應用**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 專案結構

```
src/main/java/com/smallnine/apiserver/
├── controller/          # REST API 控制器
│   ├── AuthController.java
│   ├── ProductController.java
│   └── ...
├── service/            # 業務邏輯服務
│   ├── impl/
│   │   ├── AuthServiceImpl.java
│   │   └── ProductServiceImpl.java
│   └── ...
├── dao/               # 數據訪問層 (MyBatis Mapper)
│   ├── UserDao.java
│   ├── ProductDao.java
│   └── ...
├── entity/            # 實體模型
│   ├── User.java
│   ├── Product.java
│   └── ...
├── dto/               # 數據傳輸對象
│   ├── request/
│   ├── response/
│   └── ...
├── config/            # 配置類
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   └── ...
├── filter/            # 過濾器
│   └── JwtAuthenticationFilter.java
├── interceptor/       # 攔截器
│   └── LoggingInterceptor.java
├── utils/             # 工具類
│   ├── JwtUtil.java
│   ├── LogUtil.java
│   └── ...
└── exception/         # 自定義異常
    ├── BusinessException.java
    └── ...

src/main/resources/
├── mapper/             # MyBatis XML 映射文件
│   ├── UserMapper.xml
│   ├── ProductMapper.xml
│   └── ...
├── application.properties
├── application-dev.properties
├── application-prod.properties
└── logback-spring.xml

db/                     # 資料庫 Schema（單一來源）
└── schema.sql          # 完整建表腳本（Docker init 用）
```

## 開發規範

### 代碼風格

1. **命名規範**
- 類名:PascalCase (例:UserService)
- 方法名:camelCase (例:getUserById)
- 常量:SNAKE_CASE (例:MAX_RETRY_COUNT)
- 包名:小寫 (例:com.smallnine.apiserver)

2. **註解使用**
```java
// 控制器
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    
    // 服務注入
    @Autowired
    private UserService userService;
    
    // API 端點
    @GetMapping("/{id}")
    @Operation(summary = "獲取用戶詳情")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        // 實現
    }
}

// 服務類
@Service
@Transactional
public class UserServiceImpl implements UserService {
    // 實現
}

// 實體類
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
}
```

### API 設計規範

1. **RESTful URL 設計**
```http
GET    /api/users              # 用戶列表
GET    /api/users/{id}         # 用戶詳情
POST   /api/users              # 創建用戶
PUT    /api/users/{id}         # 更新用戶
DELETE /api/users/{id}         # 刪除用戶
PATCH  /api/users/{id}/status  # 部分更新
```

2. **統一響應格式**
```java
// 成功響應
{
  "status": "success",
  "data": { ... },
  "message": null,
  "timestamp": "2023-12-01T10:00:00"
}

// 錯誤響應
{
  "status": "error",
  "data": null,
  "message": "錯誤描述",
  "code": "ERROR_CODE",
  "timestamp": "2023-12-01T10:00:00"
}
```

3. **分頁響應格式**
```java
{
  "status": "success",
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

### 異常處理

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getMessage(), e.getCode()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message, "VALIDATION_ERROR"));
    }
}
```

### 日誌規範

參考 `LOGGING_STANDARDS.md` 使用結構化日誌:

```java
@Component
public class UserService {
    
    public User createUser(UserCreateDTO dto) {
        LogUtil.info("用戶創建開始")
            .field("action", "create_user")
            .field("username", dto.getUsername())
            .field("email", dto.getEmail())
            .log();
            
        try {
            User user = userDao.create(dto);
            
            LogUtil.info("用戶創建成功")
                .field("action", "create_user")
                .field("userId", user.getId())
                .field("username", user.getUsername())
                .log();
                
            return user;
        } catch (Exception e) {
            LogUtil.error("用戶創建失敗")
                .field("action", "create_user")
                .field("username", dto.getUsername())
                .field("error", e.getMessage())
                .log();
            throw e;
        }
    }
}
```

## 資料庫開發

### MyBatis 配置

```java
// Mapper 接口
@Mapper
public interface UserDao {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Insert("INSERT INTO users (username, email, password) VALUES (#{username}, #{email}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    // 複雜查詢使用 XML
    List<User> findByConditions(UserSearchDTO conditions);
}
```

```xml
<!-- UserMapper.xml -->
<mapper namespace="com.smallnine.apiserver.dao.UserDao">
    
    <select id="findByConditions" resultType="User">
        SELECT * FROM users
        <where>
            <if test="username != null">
                AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="email != null">
                AND email = #{email}
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
        ORDER BY created_at DESC
        <if test="limit != null and offset != null">
            LIMIT #{limit} OFFSET #{offset}
        </if>
    </select>
    
</mapper>
```

### 事務管理

```java
@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    
    // 只讀事務
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderDao.findById(id);
    }
    
    // 寫入事務
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        // 1. 檢查庫存
        // 2. 創建訂單
        // 3. 扣減庫存
        // 4. 清空購物車
    }
}
```

## 安全開發

### JWT 實作

```java
@Component
public class JwtUtil {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration}")
    private int jwtExpiration;
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("created", new Date());
        return createToken(claims, userDetails.getUsername());
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

### 輸入驗證

```java
public class UserCreateDTO {
    
    @NotBlank(message = "用戶名不能為空")
    @Size(min = 3, max = 50, message = "用戶名長度必須在3-50字符之間")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用戶名只能包含字母、數字和下劃線")
    private String username;
    
    @NotBlank(message = "郵箱不能為空")
    @Email(message = "郵箱格式不正確")
    private String email;
    
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, message = "密碼長度不能少於8位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "密碼必須包含大小寫字母和數字")
    private String password;
}
```

## 測試開發

### 單元測試

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserDao userDao;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        
        when(userDao.findByUsername("testuser")).thenReturn(null);
        when(userDao.save(any(User.class))).thenReturn(savedUser);
        
        // When
        User result = userService.createUser(dto);
        
        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userDao).save(any(User.class));
    }
}
```

### 整合測試

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        UserCreateDTO request = new UserCreateDTO();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123");
        
        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/users", 
            request, 
            ApiResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo("success");
    }
}
```

### API 測試

```bash
# 運行所有測試
./mvnw test

# 運行特定測試類
./mvnw test -Dtest=UserServiceTest

# 運行整合測試
./mvnw test -Dtest=ECommerceIntegrationTest

# 生成測試報告
./mvnw surefire-report:report
```

## 性能優化

### 資料庫優化

1. **查詢優化**
```sql
-- 添加索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_products_category_brand ON products(category_id, brand_id);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
```

2. **分頁查詢**
```java
@GetMapping
public ApiResponse<Page<ProductResponse>> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String search) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<Product> products = productService.findProducts(search, pageable);
    return ApiResponse.success(products.map(this::toProductResponse));
}
```

### 緩存策略

```java
@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        return productDao.findById(id);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productDao.update(product);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() {
        // 清除所有產品緩存
    }
}
```

## 調試技巧

### 本地調試

1. **設置斷點**
- 在 IDE 中設置斷點
- 使用條件斷點:`user.getId() == 123L`

2. **日誌調試**
```properties
# application-dev.properties
logging.level.com.smallnine.apiserver=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.mybatis=DEBUG
```

3. **SQL 調試**
```properties
# 顯示 SQL 語句
mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

# 或使用 logback
logging.level.com.smallnine.apiserver.dao=DEBUG
```

### 遠程調試

```bash
# 啟動應用時添加 JVM 參數
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar

# Docker 容器調試
docker run -p 8080:8080 -p 5005:5005 \
  -e JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  bark-bijou-api
```

## Git 工作流程

### 分支策略

```bash
# 功能開發
git checkout -b feature/user-management
git commit -m "feat: 添加用戶管理功能"
git push origin feature/user-management

# Bug 修復
git checkout -b hotfix/fix-login-issue
git commit -m "fix: 修復登入問題"
git push origin hotfix/fix-login-issue

# 發布準備
git checkout -b release/v1.2.0
git commit -m "chore: 準備 v1.2.0 發布"
git push origin release/v1.2.0
```

### 提交信息規範

```
type(scope): subject

body

footer
```

類型:
- `feat`: 新功能
- `fix`: Bug 修復
- `docs`: 文檔更新
- `style`: 代碼格式化
- `refactor`: 重構
- `test`: 測試相關
- `chore`: 建構過程或輔助工具變動

範例:
```
feat(auth): 添加 JWT refresh token 機制

- 實作 refresh token 自動刷新
- 添加 token 過期處理
- 更新前端整合範例

Closes #123
```