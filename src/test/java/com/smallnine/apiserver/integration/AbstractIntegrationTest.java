package com.smallnine.apiserver.integration;

/**
 * 整合測試基類
 * 使用 Docker 容器中的 PostgreSQL 數據庫（透過 .env 配置）
 */
public abstract class AbstractIntegrationTest {
    // 數據庫配置由 spring-dotenv 從 .env 文件讀取
    // 確保 docker-compose up 已啟動 PostgreSQL 容器
}
