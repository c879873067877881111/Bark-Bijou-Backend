# api-server 安全與架構審查 TODO

> 來源：2026-05-10 senior-backend code review。每條 TODO 對應一個 PR、一條 feature branch。
> 本文件是「進度追蹤」，修完打勾、merge 後刪除該行（保留 git history 即可）。

---

## 🔁 標準處理流程（每條 TODO 都走這套）

```
   ┌─ 1. 選一條 TODO（從上往下，優先級高的先做）
   │
   ├─ 2. 從 main 開 feature branch
   │      git checkout main && git pull --ff-only
   │      git checkout -b <type>/<scope>-<short-desc>
   │      例：fix/auth-c1-skip-email-validation
   │           refactor/jwt-m3-remove-dead-refresh-path
   │
   ├─ 3. 寫測試（先紅再綠）
   │      ./mvnw test -Dtest=<TargetTest>          # 確認失敗
   │      實作修正
   │      ./mvnw test                              # 全部綠
   │
   ├─ 4. 本地驗證
   │      ./mvnw test
   │      docker-compose up -d postgres redis
   │      ./mvnw spring-boot:run                   # 手動 smoke 對應 endpoint
   │
   ├─ 5. Commit（Conventional Commits）
   │      <type>(<scope>): <subject>
   │      type: fix | feat | refactor | chore | test | docs | perf
   │      scope: auth | order | cart | jwt | logging | db | ...
   │      例：fix(auth): enforce emailValidated check in login (#C1)
   │           refactor(jwt): drop unused refresh-as-jwt code path (#M3)
   │
   ├─ 6. Push & 開 PR
   │      git push -u origin HEAD
   │      gh pr create --title "<subject>" --body-file <PR_TEMPLATE>
   │      → PR title 帶 TODO 編號，body 用下方範本
   │
   ├─ 7. Code Review（依規模選一）
   │      自審（小改）：對著本檔末尾的「自審清單」逐項檢查
   │      AI 輔助：在 PR commit 上跑 /review 或 /security-review
   │      重大安全修正：跑 /ultrareview <PR#>（Critical 等級必跑）
   │
   ├─ 8. 處理 review 意見 → 同 branch 再推 → 重新 review
   │
   ├─ 9. Merge 到 main（squash，commit message 對齊 PR title）
   │      gh pr merge --squash --delete-branch
   │
   └─10. 在本檔把對應 TODO 打勾並寫 PR 連結；下一輪部署時跟著上線
```

### Branch 命名

| 用途 | 前綴 | 範例 |
|------|------|------|
| 修 bug / 漏洞 | `fix/` | `fix/order-c3-cancel-race` |
| 新功能 | `feat/` | `feat/auth-rate-limit-bucket4j` |
| 重構/清理 | `refactor/` | `refactor/jwt-m3-dead-code` |
| 純文件 | `docs/` | `docs/review-todo` |
| 效能 | `perf/` | `perf/cart-batch-query` |

每條 branch 對應**一個** TODO 編號（`#C1`、`#H2`...），PR 也是。**不要把多個無關修正塞同一個 PR**，review 跟 revert 都會痛。

### PR 範本（建到 `api-server/.github/pull_request_template.md`）

```markdown
## 對應 TODO
`api-server/REVIEW_TODO.md` #<C1|H2|M5...>
<!-- 不要用 GitHub `Closes #` 語法,TODO 編號不是 issue 號 -->

## 為什麼改
（一兩句講根本原因，不是 what）

## 怎麼改
- 具體做法 1
- 具體做法 2

## 影響面
- API 行為變化：（有/無，列出來）
- DB schema 變動：（有/無，附 migration）
- 前端需要同步調整：（有/無，列出端點）

## 測試
- [ ] 新增單元測試覆蓋此 case
- [ ] 整合測試通過 (`./mvnw test`)
- [ ] 手動驗證：列出實際打的 endpoint 與預期/實際結果

## 風險與 rollback
- 風險：
- Rollback：revert PR + （是否需 DB migration 反向）
```

### 自審清單（push 前先走一遍）

- [ ] 改動跟 PR 描述一致，沒夾帶不相關的 reformat / rename
- [ ] 沒新增 `${}` 字串拼 SQL（MyBatis 用 `#{}`）
- [ ] 沒在 log 裡印密碼、token、完整 email、信用卡等敏感資料
- [ ] 新 `@PostMapping`/`@PutMapping`/`@DeleteMapping` 都檢查過 ownership 或加 `@PreAuthorize`
- [ ] 新 endpoint 加進 `SecurityConfig` 白名單時，明確列出 HTTP method（不要全 path 開放）
- [ ] DB schema 變動有對應的 `db/migrations/Vxx__*.sql`，**不是**改 `init.sql`（init 只在空 volume 時跑）
- [ ] 更新 `LOGGING_STANDARDS.md` 規範的 `action=...` 欄位

---

## 🔴 Critical — 阻擋上線

- [ ] **#C1** `AuthServiceImpl.login()` 旁路 `isEnabled` 檢查，未驗證 email 也能拿 token
  - 位置：`src/main/java/com/smallnine/apiserver/service/impl/AuthServiceImpl.java:85-116`
  - 改法：login 走 `AuthenticationManager.authenticate(...)`，或顯式檢查 `user.getEmailValidated()` 與 filter 行為對齊
  - 測試：`shouldRejectLoginWhenEmailNotValidated()`
  - PR：

- [ ] **#C2** 每次登入撤銷該 user **所有**裝置 refresh token，多裝置互踢
  - 位置：`src/main/java/com/smallnine/apiserver/service/impl/RefreshTokenServiceImpl.java:30`
  - 改法：`refresh_token` 表加 `device_id`/`client_id` 欄位（migration），只 revoke 同裝置；或改為到期 + 主動 logout 才 revoke
  - DB migration：`db/migrations/V2__refresh_token_device_id.sql`
  - PR：

- [ ] **#C3** `cancelOrder` 並發時可雙重恢復庫存
  - 位置：`OrderServiceImpl.java:264-287`、`ProductMapper.xml:177` (`increaseStock`)
  - 改法：先用 CAS 改狀態 `UPDATE orders SET status_id=CANCELLED WHERE id=? AND status_id IN (...)`，更新行數=0 即 throw；確認唯一一次後才 increaseStock
  - 測試：兩 thread 同時 cancel 同一 orderId，斷言 stock 只 +N 一次
  - PR：

- [ ] **#C4** Idempotency Redis 寫入無失敗保護，DB commit 後 Redis 失敗會卡 24h
  - 位置：`OrderServiceImpl.java:145`
  - 改法：包 try/catch，失敗時 `redisTemplate.delete(redisKey)` 並 metrics 記一筆；考慮改用 Lua script 原子完成「PENDING → orderId」轉換
  - PR：

---

## 🟠 High — 安全性 / 帳號枚舉

- [ ] **#H1** `/api/auth/resend-verification` 是 email 炸彈 + 帳號枚舉武器
  - 位置：`AuthController.java:124`、`AuthServiceImpl.java:191-207`
  - 改法：(a) bucket4j rate limit，每 email 每 5 分鐘 1 次、每 IP 每分鐘 N 次；(b) email 不存在或已驗證一律回 200「若該信箱存在且未驗證，已寄送驗證信」
  - PR：

- [ ] **#H2** 登入端點全無 rate limit / 失敗計數
  - 位置：`SecurityConfig.java`、`AuthController.java`
  - 改法：引入 bucket4j + Redis；`/login` 失敗 N 次 → 該 IP 限流 / 該 username 鎖 15 分鐘；`/register`、`/oauth/exchange` 也加 IP 級 throttle
  - PR：

- [ ] **#H3** `JWT_SECRET` 沒 startup 長度斷言 + `getBytes()` 用預設 charset
  - 位置：`JwtUtil.java:29-31`
  - 改法：`secret.getBytes(StandardCharsets.UTF_8)`；加 `@PostConstruct` 斷言 `secret.getBytes(UTF_8).length >= 32`，否則 fail-fast
  - PR：

- [ ] **#H4** JWT 缺 `iss`/`aud`/`jti`，access token 偷了無法廢
  - 位置：`JwtUtil.java:101-109`
  - 改法：加 `iss=api-server`、`aud=bark-bijou-web`、`jti=UUID`；新增 jti 黑名單表 + Redis cache，logout 時把當前 access token 的 jti 寫黑名單到 exp
  - 註：低優先，access token 才 15 分；先做 #C1/C2 再回頭看
  - PR：

- [ ] **#H5** `SqlSecurityUtil.sanitizeInput()` 是錯誤的安全模型
  - 位置：`utils/SqlSecurityUtil.java:31-43`
  - 改法：刪除 `sanitizeInput`；保留 `escapeLikePattern`（LIKE 確實需要）；全 repo `grep -rn sanitizeInput` 找呼叫端，改為輸出層 escape 或單純驗證長度
  - PR：

---

## 🟡 Medium — 架構 / 正確性

- [ ] **#M1** 授權紀律不一致：4/24 controller 有 `@PreAuthorize`，其餘靠 service 手動檢查
  - 位置：`controller/*.java`、`SecurityConfig.java`
  - 改法：訂出規則「所有寫入 endpoint 必須有 `@PreAuthorize`」，逐 controller 補；ownership 檢查抽到 `@securityService.canEdit*(authentication, #id)` bean，避免 SpEL 內混業務邏輯
  - 拆 PR 建議：依 controller 分批，一次補一個 controller，避免單一 PR 太大
  - PR：

- [ ] **#M2** `@PreAuthorize` SpEL 內含 DB 查詢，重複命中
  - 位置：`ArticleController.java:136, 154`
  - 改法：抽 `SecurityService.canEditArticle(...)`，內部 cache 或合併查詢；若該 controller 後續也要查 article，用 `@PathVariable` + `Article` 解析器避免雙查
  - 隨 #M1 一起做
  - PR：

- [ ] **#M3** Refresh token 是 UUID，但 `JwtUtil` 留著一整套 refresh-as-JWT 的 dead code
  - 位置：`JwtUtil.java:91-121`、`JwtAuthenticationFilter.java:57-62`
  - 改法：刪 `generateRefreshToken`、`isRefreshToken`、`isAccessToken`、`extractTokenType`；filter 拿掉 `if (!isAccessToken(...))`
  - PR：

- [ ] **#M4** `CartServiceImpl.addToCart` 庫存檢查是 UX 軟提示，未寫清楚
  - 位置：`CartServiceImpl.java:56-72`
  - 改法：加一段 javadoc 註明「此處檢查僅為 UX 提示，真實扣庫存以 `OrderService.createOrderFromCart` 為準」
  - 純文件改動，5 分鐘
  - PR：

- [ ] **#M5** `RuntimeException` handler 把 `ex.getMessage()` 直印 ELK
  - 位置：`GlobalExceptionHandler.java:172-183`
  - 改法：對 `DataAccessException` 子類別只記類名 + stack trace，不打 raw message；其他 RuntimeException 維持現狀
  - PR：

- [ ] **#M6** Cache key 容易爆量、`@CacheEvict(allEntries=true)` 太粗
  - 位置：`BrandServiceImpl.java`、`CategoryServiceImpl.java`
  - 改法：只 cache 高頻入口（如 `findById`、`tree`），列表查詢別 cache；evict 改為 by id
  - 影響不大，列為觀察項
  - PR：

- [ ] **#M7** Email 驗證 token 與密碼重置 token 共用 `reset_token` 欄位，會互相覆蓋
  - 位置：`User.java`、`AuthServiceImpl.java:73`、`ForgotPasswordServiceImpl`
  - 改法：拆 `email_verification_token` / `password_reset_token` 兩欄位，或新增 `email_verifications` 表
  - DB migration：`db/migrations/V3__split_token_columns.sql`
  - PR：

---

## 🟢 Low / Nit — 技術債

- [ ] **#N1** `JwtUtil` 三個 `generateToken*` 重載，刪掉沒用的兩個
- [ ] **#N2** `CartServiceImpl.countCartItems` 是 `getCartItemCount` 的別名，挑一個刪
- [ ] **#N3** `JwtUtil.extractAllClaims` 五個 catch 濃縮成 `JwtException | IllegalArgumentException`
- [ ] **#N4** `GlobalExceptionHandler` 中的「數據」「數據庫」改繁體
- [ ] **#N5** 刪掉 `application.properties` 死設定 `spring.jpa.database-platform`
- [ ] **#N6** `RefreshTokenServiceImpl` log 印 token prefix 8 字元，確認與隱私政策一致

> Nit 類可一個 PR 打包（`chore/nit-cleanup-batch-1`），但不要混 fix/feat。

---

## 建議排程

| 週 | 主題 | TODO |
|----|------|------|
| W1 | 認證安全 | #C1 → #C2 → #H1 → #H2 |
| W2 | 訂單一致性 | #C3 → #C4 |
| W3 | 授權紀律 | #M1（拆 4-5 個 PR）→ #M2 |
| W4 | 清理 + 文件 | #M3 → #M5 → #H3 → #H5 → Nit 批次 |
| 後續 | 觀察項 | #H4、#M6、#M7 |

每週開始前更新本檔的 PR 連結欄位；每週結束 retro 一次，把已 merge 的劃掉並同步專案根層文件（如有架構級變化）。
