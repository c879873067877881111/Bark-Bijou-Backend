# ELK 結構化日誌

## 核心原則
1. **格式統一**:所有日誌使用 `key=value` 結構
2. **必要標籤**:每條日誌必須包含 `action` 和 `result`
3. **量化數據**:記錄可聚合的數值(時間、計數、大小)
4. **上下文資訊**:善用 MDC 提供的追蹤信息

## 字段定義

### 必要字段
- `action=` 操作類型(login, register, logout, refresh_token, etc.)
- `result=` 操作結果(attempt, success, failed)

### 識別字段
- `username=` 用戶名
- `user_id=` 用戶ID
- `email=` 郵箱
- `user=` 用戶名或郵箱(登入時使用)

### 失敗原因
- `reason=` 失敗原因(user_not_found, invalid_password, username_exists, etc.)

### 量化指標
- `duration_ms=` 執行時間(毫秒)
- `token_expires=` Token 到期時間
- `count=` 計數

### 安全性字段
- `client_ip=` 客戶端IP(通過 MDC 獲取)
- `trace_id=` 請求追蹤ID(通過 MDC 獲取)

## 範例格式

### 用戶註冊
```java
// 開始嘗試
log.info("action=register username={} email={} result=attempt", username, email);

// 失敗案例
log.warn("action=register username={} result=failed reason=username_exists", username);

// 成功案例
log.info("action=register username={} user_id={} result=success", username, userId);
```

### 用戶登入
```java
// 開始嘗試
log.info("action=login user={} result=attempt", usernameOrEmail);

// 各種失敗情況
log.warn("action=login user={} result=failed reason=user_not_found", usernameOrEmail);
log.warn("action=login username={} user_id={} result=failed reason=account_disabled", username, userId);
log.warn("action=login username={} user_id={} result=failed reason=invalid_password", username, userId);

// 成功案例
log.info("action=login username={} user_id={} result=success token_expires={}", username, userId, expiresAt);
```

### 業務操作
```java
// 訂單創建
log.info("action=create_order user_id={} order_id={} amount={} result=success", userId, orderId, amount);

// 支付處理
log.info("action=payment user_id={} order_id={} amount={} duration_ms={} result=success", 
         userId, orderId, amount, duration);
```

## ELK 查詢範例

### Elasticsearch 查詢
```json
{
  "query": {
    "bool": {
      "must": [
        {"term": {"action": "login"}},
        {"term": {"result": "failed"}}
      ]
    }
  },
  "aggs": {
    "failure_reasons": {
      "terms": {"field": "reason"}
    }
  }
}
```

### Kibana 儀表板指標
- 登入成功率:`action:login AND result:success` / `action:login AND result:attempt`
- 註冊失敗原因分布:`action:register AND result:failed` by `reason`
- 異常用戶:同一 IP 在短時間內的失敗嘗試次數

## 注意事項
1. **敏感資訊**:絕對不記錄密碼、Token 內容等敏感信息
2. **性能考量**:避免在高頻操作中記錄過多詳細信息
3. **術語統一**:團隊內統一 `action` 和 `reason` 的命名規範
