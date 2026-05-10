## 對應 TODO
`api-server/REVIEW_TODO.md` #<C1|H2|M5...>
<!-- 不要用 GitHub `Closes #` 語法,TODO 編號不是 issue 號;merge 後手動回 REVIEW_TODO 打勾 -->


## 為什麼改
<!-- 一兩句講根本原因，不是 what -->

## 怎麼改
- 具體做法 1
- 具體做法 2

## 影響面
- API 行為變化：（有/無，列出來）
- DB schema 變動：（有/無，附 migration 檔名）
- 前端需要同步調整：（有/無，列出端點）

## 測試
- [ ] 新增/更新單元測試覆蓋此 case
- [ ] 整合測試通過 (`./mvnw test`)
- [ ] 手動驗證：列出實際打的 endpoint 與預期/實際結果

## 自審清單（push 前先過一遍）
- [ ] 改動跟 PR 描述一致，沒夾帶不相關的 reformat / rename
- [ ] 沒新增 `${}` 字串拼 SQL（MyBatis 用 `#{}`）
- [ ] log 沒印密碼、完整 token、信用卡等敏感資料
- [ ] 新寫入 endpoint 都檢查過 ownership 或加 `@PreAuthorize`
- [ ] DB schema 變動走 `db/migrations/Vxx__*.sql`，**不是**改 `db/init.sql`
- [ ] 審計日誌欄位符合 `LOGGING_STANDARDS.md`（`action=` / `result=`）

## 風險與 Rollback
- 風險：
- Rollback：revert PR + （是否需 DB migration 反向腳本）
