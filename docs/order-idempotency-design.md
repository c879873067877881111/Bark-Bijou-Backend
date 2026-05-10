# 下單冪等性 + 交易設計

## 問題背景

使用者在下單頁面連點兩次、或網路不穩重送請求，會建出兩張一樣的訂單。需要做**冪等性（idempotency）**控制：**相同意圖的重複請求只能產生一次實際寫入**。

傳統解法：資料庫 unique constraint。缺點是會先進到 INSERT 才發現衝突，已經吃了一輪 IO + 交易回滾成本。

本專案採用：**Redis SETNX 佔位 + Spring `@Transactional` + self-proxy** 三層協作。

---

## 整體架構

```
Controller (POST /api/orders)
    │  Idempotency-Key header (UUID，選用)
    ▼
OrderServiceImpl.createOrderFromCart(memberId, request, idempotencyKey)
    │  無 @Transactional，負責冪等協調
    │
    ├─ 1. SETNX "order:idempotency:{memberId}:{key}" = "PENDING" (24h TTL)
    ├─ 2. 佔位失敗 → 讀回已存在的 orderId 或拋 CONFLICT
    ├─ 3. 佔位成功 → 透過 self-proxy 呼叫下一層
    │
    ▼
OrderServiceImpl.createOrderFromCart(memberId, request)
    │  @Transactional 真正開交易
    │
    └─ doCreateOrderFromCart(memberId, request)  // private，7 步驟都在這條交易裡
        1. 取購物車商品
        2. 驗證庫存（防超賣）
        3. 計算總金額
        4. 原子 SQL 扣庫存（UPDATE stock WHERE stock >= ?）
        5. INSERT 訂單
        6. 批次 INSERT 訂單明細
        7. DELETE 清空購物車
        → 任一步拋例外，整條交易 rollback
```

**回到外層後**，`redisTemplate.set(redisKey, orderId, 24h)` 把 Redis 的 PENDING 覆寫成真實 orderId。

---

## 兩個關鍵設計決策

### 1. 為什麼用 self-proxy（`ObjectProvider<OrderService>`）？

外層冪等協調 + 內層交易，是兩件事。如果放在同一個方法、同一層 `@Transactional`：

- Redis 操作被包進 DB 交易切面，但 Redis 本來就不是 DB 交易的一部分 → 誤導
- 失敗處理路徑混亂：commit 失敗時很難區分「Redis 要留 PENDING」還是「Redis 要清掉」

拆成兩層後，**同一個 bean 呼叫自己的另一個方法會觸發 self-invocation**——`this.xxx()` 繞過 Spring 代理，被呼叫方法上的 `@Transactional` 不會生效。

解法：注入自己這個 bean 的代理參考：

```java
private final ObjectProvider<OrderService> selfProvider;  // 延遲解析避免循環依賴

// 呼叫時透過代理，@Transactional 切面才會啟動
Order order = selfProvider.getObject().createOrderFromCart(memberId, request);
```

**為什麼用 `ObjectProvider` 而不是 `@Lazy @Autowired` 欄位注入？**
- `@RequiredArgsConstructor` 產生的建構子能直接收 `ObjectProvider<T>`
- 保持 constructor injection，final 欄位，不可變
- 避免欄位注入（欄位注入無法 `final`、難測試）

### 2. 為什麼 orderId 寫 Redis 放在 commit 之後？

**錯誤設計**：Redis 寫入發生在 commit 前（或包在 `@Transactional` 內）。
- commit 失敗 → DB rollback，但 Redis 已經寫了 orderId
- 下次重送同 key → 讀到「殭屍 orderId」，查 DB 拿不到 → 使用者收到 404

**正確設計**：先 commit 再寫 Redis。
- self-proxy 呼叫的兩參數方法 return 時，`@Transactional` 切面已經完成 commit
- 回到外層才執行 `redisTemplate.set(..., orderId)`
- commit 失敗則拋例外被外層 catch 抓到，清掉 PENDING 佔位

保證不變式：**Redis 只會有「沒資料」或「真實 orderId」兩種狀態，絕不出現指向不存在訂單的殭屍 ID**。

---

## 異常處理矩陣

| 失敗點 | Redis 狀態 | DB 狀態 | 下次重送同 key 的行為 |
|--------|-----------|---------|---------------------|
| **正常流程** | 真實 orderId | 訂單存在 | 讀 Redis → 回傳同一張訂單 |
| **DB 寫入失敗**（業務錯誤） | catch 清掉 key | rollback | SETNX 成功 → 全新建單 |
| **DB commit 失敗** | catch 清掉 key | rollback | SETNX 成功 → 全新建單 |
| **commit 成功但寫 Redis 前 process 掛掉** | PENDING（未覆寫） | 訂單已存在 | 讀 Redis 拿到 PENDING → 拋 `CONFLICT`，叫前端至訂單列表查詢 |
| **同一 idempotencyKey 並發重送（建單中）** | PENDING | 建立中 | SETNX 失敗 → 拋 `CONFLICT` |

---

## 可改進點（已知限制）

1. **PENDING 卡 24 小時**：目前 PENDING 跟成功 orderId 共用同一個 TTL。實務上應該把 PENDING 設短 TTL（例如 30 秒），只用來防連點；建立成功後才設 24 小時。
2. **Redis 全掛的降級**：目前 SETNX 失敗會拋例外。更健壯的做法是用 DB 的 `idempotency_key` unique constraint 兜底，Redis 當快取層。
3. **分散式場景**：如果後端多實例同時拿到同一個 key 的請求，SETNX 是原子操作，仍然只會有一個實例佔位成功，設計是安全的。
