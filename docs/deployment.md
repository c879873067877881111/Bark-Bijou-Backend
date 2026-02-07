# 部署指南

## Docker 部署

### 快速部署

1. **啟動基礎服務**
```bash
docker-compose up -d
```

2. **構建應用鏡像**
```bash
docker build -t bark-bijou-api .
```

3. **運行應用容器**
```bash
docker run -d \
  -p 8080:8080 \
  --link postgres:postgres \
  --link redis:redis \
  -e SPRING_PROFILES_ACTIVE=prod \
  bark-bijou-api
```

### Docker Compose 完整部署

創建 `docker-compose.prod.yml`:

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bark_bijou
      SPRING_DATA_REDIS_HOST: redis
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: bark_bijou
      POSTGRES_USER: user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./db/schema.sql:/docker-entrypoint-initdb.d/init.sql
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    restart: unless-stopped

volumes:
  postgres_data:
```

## 生產環境配置

### 環境變數配置

```bash
# 資料庫配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bark_bijou
DB_USERNAME=user
DB_PASSWORD=secure_password

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# JWT 配置
JWT_SECRET=your-super-secure-jwt-secret-key-here
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000

# 日誌配置
LOG_LEVEL=INFO
LOG_FILE_PATH=/var/log/bark-bijou/api-server.log
```

### application-prod.properties

```properties
# 基本配置
spring.profiles.active=prod
server.port=8080

# 資料庫連接
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:bark_bijou}
spring.datasource.username=${DB_USERNAME:user}
spring.datasource.password=${DB_PASSWORD:password}

# 連接池配置
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=60000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=900000

# Redis 配置
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0

# JWT 配置
app.jwt.secret=${JWT_SECRET:default-secret}
app.jwt.expiration=${JWT_EXPIRATION:900000}
app.jwt.refresh-expiration=${REFRESH_TOKEN_EXPIRATION:604800000}

# 日誌配置
logging.level.root=WARN
logging.level.com.smallnine.apiserver=INFO
logging.file.name=${LOG_FILE_PATH:/var/log/bark-bijou/api-server.log}

# 安全配置
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never

# Actuator 配置
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

## 雲端部署

### AWS ECS 部署

1. **構建並推送鏡像到 ECR**
```bash
# 登入 ECR
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-west-2.amazonaws.com

# 構建鏡像
docker build -t bark-bijou-api .

# 標記鏡像
docker tag bark-bijou-api:latest 123456789012.dkr.ecr.us-west-2.amazonaws.com/bark-bijou-api:latest

# 推送鏡像
docker push 123456789012.dkr.ecr.us-west-2.amazonaws.com/bark-bijou-api:latest
```

2. **ECS Task Definition**
```json
{
  "family": "bark-bijou-api",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "bark-bijou-api",
      "image": "123456789012.dkr.ecr.us-west-2.amazonaws.com/bark-bijou-api:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-west-2:123456789012:secret:prod/bark-bijou/db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/bark-bijou-api",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Google Cloud Run 部署

1. **構建並推送鏡像**
```bash
# 配置 Docker 認證
gcloud auth configure-docker

# 構建鏡像
docker build -t gcr.io/PROJECT_ID/bark-bijou-api .

# 推送鏡像
docker push gcr.io/PROJECT_ID/bark-bijou-api
```

2. **部署到 Cloud Run**
```bash
gcloud run deploy bark-bijou-api \
  --image=gcr.io/PROJECT_ID/bark-bijou-api \
  --platform=managed \
  --region=asia-east1 \
  --allow-unauthenticated \
  --memory=1Gi \
  --cpu=1 \
  --port=8080 \
  --set-env-vars=SPRING_PROFILES_ACTIVE=prod \
  --set-secrets=DB_PASSWORD=projects/PROJECT_ID/secrets/db-password:latest
```

## 負載均衡和反向代理

### Nginx 配置

```nginx
upstream bark_bijou_backend {
    server app1:8080;
    server app2:8080;
    server app3:8080;
}

server {
    listen 80;
    server_name api.bark-bijou.com;
    
    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.bark-bijou.com;
    
    ssl_certificate /etc/nginx/certs/bark-bijou.com.crt;
    ssl_certificate_key /etc/nginx/certs/bark-bijou.com.key;
    
    # 安全標頭
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # API 代理
    location /api/ {
        proxy_pass http://bark_bijou_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超時配置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
    
    # 靜態資源
    location /swagger-ui/ {
        proxy_pass http://bark_bijou_backend;
        proxy_set_header Host $host;
    }
    
    # 健康檢查
    location /actuator/health {
        proxy_pass http://bark_bijou_backend;
        access_log off;
    }
}
```

## 監控和日誌

### Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'bark-bijou-api'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana 儀表板配置

重要指標監控:
- HTTP 請求量和響應時間
- JVM 記憶體使用量
- 資料庫連接池狀態
- Redis 連接狀態
- 錯誤率和異常計數

### ELK Stack 日誌收集

```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/bark-bijou/*.log
  fields:
    service: bark-bijou-api
  json.keys_under_root: true

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "bark-bijou-api-%{+yyyy.MM.dd}"

setup.template.name: "bark-bijou-api"
setup.template.pattern: "bark-bijou-api-*"
```

## 安全配置

### SSL/TLS 設定

```bash
# 生成自簽證書 (僅開發環境)
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

# Let's Encrypt 證書 (生產環境)
certbot certonly --webroot -w /var/www/html -d api.bark-bijou.com
```

### 防火牆規則

```bash
# UFW 配置
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

## 備份和恢復

### 資料庫備份

```bash
# 每日備份腳本
#!/bin/bash
BACKUP_DIR="/backup/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="bark_bijou"

# 創建備份
pg_dump -h localhost -U user -d $DB_NAME > $BACKUP_DIR/backup_$DATE.sql

# 保留最近 7 天的備份
find $BACKUP_DIR -name "backup_*.sql" -mtime +7 -delete
```

### 應用數據備份

```bash
# 備份上傳的文件
rsync -av /var/bark-bijou/uploads/ /backup/uploads/

# 備份日誌
tar -czf /backup/logs/logs_$(date +%Y%m%d).tar.gz /var/log/bark-bijou/
```

## 故障排除

### 常見問題

1. **應用啟動失敗**
```bash
# 檢查日誌
docker logs bark-bijou-api

# 檢查端口佔用
netstat -tulpn | grep 8080

# 檢查資料庫連接
telnet postgres-host 5432
```

2. **記憶體不足**
```bash
# 調整 JVM 記憶體
-Xms512m -Xmx1024m

# 檢查記憶體使用
docker stats bark-bijou-api
```

3. **資料庫連接池耗盡**
```properties
# 調整連接池大小
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.connection-timeout=20000
```

### 健康檢查

```bash
# 應用健康狀態
curl https://api.bark-bijou.com/actuator/health

# 資料庫連接測試
curl https://api.bark-bijou.com/actuator/health/db

# 記憶體使用情況
curl https://api.bark-bijou.com/actuator/metrics/jvm.memory.used
```