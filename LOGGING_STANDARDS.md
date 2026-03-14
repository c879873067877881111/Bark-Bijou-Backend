# ELK 結構化日誌

## 核心原則
1. **結構化 JSON**：生產環境使用 LogstashEncoder 輸出結構化 JSON，所有欄位為 top-level fields
2. **必要標籤**：每條審計日誌必須包含 `action` 和 `result`
3. **量化數據**：記錄可聚合的數值(時間、計數、大小)
4. **上下文資訊**：MDC 欄位（traceId, userId 等）自動成為 JSON top-level fields

## JSON 輸出格式

LogstashEncoder 輸出範例：
```json
{
  "@timestamp": "2026-03-14T10:30:00.123Z",
  "@version": "1",
  "message": "audit_event=true action=LOGIN result=SUCCESS ...",
  "logger_name": "AUDIT",
  "level": "INFO",
  "service": "api-server",
  "audit_event": true,
  "action": "LOGIN",
  "result": "SUCCESS",
  "user_id": "123",
  "username": "john",
  "client_ip": "192.168.1.1",
  "traceId": "abc-def-123",
  "userId": "123"
}
```

審計欄位透過 `StructuredArguments.entries()` 提升為 top-level fields，可直接在 Elasticsearch 查詢。
MDC 欄位（traceId, userId 等）由 LogstashEncoder 自動包含，不需在 `toJsonMap()` 重複設定。

## 字段定義

### 必要字段
- `action` — 操作類型(LOGIN, REGISTER, LOGOUT, CREATE, UPDATE, DELETE, etc.)
- `result` — 操作結果(SUCCESS, FAILURE, DENIED)

### 識別字段
- `username` — 用戶名
- `user_id` — 用戶ID
- `client_ip` — 客戶端IP

### 資源字段
- `resource` — 操作的資源類型
- `resource_id` — 資源ID
- `desc` — 描述

### 量化指標
- `duration_ms` — 執行時間(毫秒)

### 追蹤字段
- `trace_id` — 請求追蹤ID

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

### 審計事件（自動結構化）
```java
// AuditLogger 使用 StructuredArguments，JSON appender 自動提升欄位
auditLogger.logLoginSuccess(userId, username);
// JSON 輸出：{"action":"LOGIN","result":"SUCCESS","user_id":"123",...}
// 非 JSON 輸出：audit_event=true action=LOGIN result=SUCCESS user_id=123 ...
```

## Elasticsearch 查詢範例

### 查詢登入失敗（欄位為 top-level）
```json
{
  "query": {
    "bool": {
      "must": [
        {"term": {"action": "LOGIN_FAILED"}},
        {"term": {"result": "FAILURE"}}
      ]
    }
  },
  "aggs": {
    "by_username": {
      "terms": {"field": "username.keyword"}
    }
  }
}
```

### 查詢特定用戶的所有操作
```json
{
  "query": {
    "term": {"user_id": "123"}
  },
  "sort": [{"@timestamp": "desc"}]
}
```

### Kibana 儀表板指標
- 登入成功率：`action:LOGIN AND result:SUCCESS` / `action:LOGIN`
- 安全事件：`action:LOGIN_FAILED` by `username.keyword`
- 異常用戶：同一 `client_ip` 在短時間內的失敗嘗試次數
- CRUD 操作統計：`audit_event:true` by `action`

## 注意事項
1. **敏感資訊**：絕對不記錄密碼、Token 內容等敏感信息
2. **性能考量**：避免在高頻操作中記錄過多詳細信息
3. **術語統一**：團隊內統一 `action` 和 `result` 的命名規範
