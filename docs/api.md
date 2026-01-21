# API 文檔

## 認證管理 `/api/auth`

### 用戶註冊
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "string",
  "email": "string", 
  "password": "string"
}
```

### 用戶登入
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "user",
      "email": "user@example.com"
    }
  }
}
```

### 獲取當前用戶
```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

### 刷新 Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "string"
}
```

### 用戶登出
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

## 商品管理 `/api/products`

### 商品列表
```http
GET /api/products?page=0&size=20&search=keyword&categoryId=1&brandId=1
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "商品名稱",
        "description": "商品描述",
        "price": 100.00,
        "stockQuantity": 50,
        "category": {
          "id": 1,
          "name": "分類名稱"
        },
        "brand": {
          "id": 1,
          "name": "品牌名稱"
        },
        "images": [
          {
            "id": 1,
            "imageUrl": "http://example.com/image.jpg",
            "isPrimary": true
          }
        ]
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20
  }
}
```

### 商品詳情
```http
GET /api/products/{id}
```

### 創建商品
```http
POST /api/products
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "string",
  "description": "string",
  "price": 0.00,
  "stockQuantity": 0,
  "categoryId": 1,
  "brandId": 1,
  "images": [
    {
      "imageUrl": "string",
      "isPrimary": true
    }
  ]
}
```

### 更新商品
```http
PUT /api/products/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "string",
  "description": "string", 
  "price": 0.00,
  "stockQuantity": 0,
  "categoryId": 1,
  "brandId": 1
}
```

### 更新庫存
```http
PATCH /api/products/{id}/stock
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "stockQuantity": 100
}
```

## 購物車管理 `/api/cart` [需認證]

### 獲取購物車
```http
GET /api/cart
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "id": 1,
      "productId": 1,
      "productName": "商品名稱",
      "price": 100.00,
      "quantity": 2,
      "subtotal": 200.00,
      "product": {
        "id": 1,
        "name": "商品名稱",
        "price": 100.00,
        "stockQuantity": 50
      }
    }
  ]
}
```

### 添加商品到購物車
```http
POST /api/cart/items
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

### 更新購物車商品數量
```http
PUT /api/cart/items/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "quantity": 3
}
```

### 移除購物車商品
```http
DELETE /api/cart/items/{id}
Authorization: Bearer {accessToken}
```

## 訂單管理 `/api/orders` [需認證]

### 訂單列表
```http
GET /api/orders?page=0&size=20
Authorization: Bearer {accessToken}
```

### 創建訂單
```http
POST /api/orders
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "shippingAddress": {
    "recipientName": "收件人",
    "phone": "0900000000",
    "address": "台北市信義區信義路五段7號",
    "postalCode": "110"
  }
}
```

### 訂單詳情
```http
GET /api/orders/{id}
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "orderNumber": "ORD20231201001",
    "status": "PENDING",
    "totalAmount": 200.00,
    "createdAt": "2023-12-01T10:00:00",
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "商品名稱",
        "price": 100.00,
        "quantity": 2,
        "subtotal": 200.00
      }
    ],
    "shippingAddress": {
      "recipientName": "收件人",
      "phone": "0900000000",
      "address": "台北市信義區信義路五段7號",
      "postalCode": "110"
    }
  }
}
```

### 取消訂單
```http
PUT /api/orders/{id}/cancel
Authorization: Bearer {accessToken}
```

## 分類管理 `/api/categories`

### 分類列表
```http
GET /api/categories
```

### 創建分類
```http
POST /api/categories
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "string",
  "description": "string",
  "parentId": 1
}
```

## 品牌管理 `/api/brands`

### 品牌列表
```http
GET /api/brands
```

### 創建品牌
```http
POST /api/brands
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "string",
  "description": "string",
  "logoUrl": "string"
}
```

## 錯誤響應格式

所有錯誤響應都遵循以下格式:

```json
{
  "status": "error",
  "message": "錯誤描述",
  "code": "ERROR_CODE",
  "timestamp": "2023-12-01T10:00:00"
}
```

### 常見錯誤碼

- `UNAUTHORIZED` (401) - 未認證或 Token 無效
- `FORBIDDEN` (403) - 權限不足
- `NOT_FOUND` (404) - 資源不存在
- `VALIDATION_ERROR` (400) - 參數驗證失敗
- `BUSINESS_ERROR` (400) - 業務邏輯錯誤

## JWT Token 機制

### Token 類型
- **Access Token**: 有效期 15 分鐘,用於 API 認證
- **Refresh Token**: 有效期 7 天,用於刷新 Access Token