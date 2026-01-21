# JWT Refresh Token 前端整合指南

## 概述

**前端改動**：實現 Access Token + Refresh Token 機制，前端只需要調整 API 調用邏輯。

## 後端配置

### Token 配置
```properties
# Access Token (15分鐘)
jwt.access-token.expiration=900000

# Refresh Token (7天)  
jwt.refresh-token.expiration=604800000
```

### 新增 API 端點
- `POST /api/auth/refresh` - 刷新 Access Token
- `POST /api/auth/logout` - 撤銷 Refresh Token

## 前端改動概覽

### 1. 存儲機制調整
```javascript
// 之前：只存儲一個 token
localStorage.setItem('token', response.data.token);

// 現在：存儲兩個 token
localStorage.setItem('accessToken', response.data.accessToken);
localStorage.setItem('refreshToken', response.data.refreshToken);
```

### 2. 登入回應格式變更
```javascript
// 新的登入回應格式
{
  "code": 200,
  "message": "登入成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",  // 15分鐘有效
    "refreshToken": "abc123-def456-ghi789...",   // 7天有效
    "type": "Bearer",
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "Test User",
    "gender": "male",
    "expiresAt": "2025-11-26T16:03:36.726911"
  },
  "success": true
}
```

## 前端實現建議

### 1. Auth Hook 升級 (`use-auth.js`)

```javascript
import { useState, useEffect, useCallback } from 'react';

const API_BASE = 'http://localhost:8080';

export const useAuth = () => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // 獲取 tokens
  const getAccessToken = () => localStorage.getItem('accessToken');
  const getRefreshToken = () => localStorage.getItem('refreshToken');
  
  // 存儲 tokens
  const setTokens = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  };
  
  // 清除 tokens
  const clearTokens = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  };

  // 自動刷新 token
  const refreshAccessToken = useCallback(async () => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token');
    }

    const response = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    const data = await response.json();
    
    if (!data.success) {
      throw new Error(data.message);
    }

    setTokens(data.data.accessToken, data.data.refreshToken);
    return data.data.accessToken;
  }, []);

  // 登入
  const login = async (credentials) => {
    const response = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials)
    });

    const data = await response.json();
    
    if (data.success) {
      setTokens(data.data.accessToken, data.data.refreshToken);
      setUser(data.data);
    }
    
    return data;
  };

  // 登出
  const logout = async () => {
    const refreshToken = getRefreshToken();
    
    if (refreshToken) {
      try {
        await fetch(`${API_BASE}/api/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });
      } catch (error) {
        console.warn('Logout request failed:', error);
      }
    }
    
    clearTokens();
    setUser(null);
  };

  // 註冊
  const register = async (userData) => {
    const response = await fetch(`${API_BASE}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userData)
    });

    return await response.json();
  };

  return {
    user,
    isLoading,
    login,
    logout,
    register,
    getAccessToken,
    refreshAccessToken,
    clearTokens
  };
};
```

### 2. API 攔截器實現

```javascript
// api.js - API 請求處理
import { useAuth } from './use-auth';

class ApiClient {
  constructor() {
    this.baseURL = 'http://localhost:8080';
    this.refreshPromise = null;
  }

  async request(url, options = {}) {
    const { getAccessToken, refreshAccessToken, clearTokens } = useAuth();
    
    // 第一次請求
    let accessToken = getAccessToken();
    
    const makeRequest = async (token) => {
      const config = {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
          ...options.headers,
        },
      };

      return fetch(`${this.baseURL}${url}`, config);
    };

    let response = await makeRequest(accessToken);

    // 如果 Access Token 過期 (401)，自動刷新
    if (response.status === 401 && accessToken) {
      try {
        // 避免多個請求同時刷新 token
        if (!this.refreshPromise) {
          this.refreshPromise = refreshAccessToken();
        }
        
        const newAccessToken = await this.refreshPromise;
        this.refreshPromise = null;
        
        // 使用新 token 重新請求
        response = await makeRequest(newAccessToken);
        
      } catch (refreshError) {
        // Refresh Token 也過期了，清除所有 token 新增路徑到登入頁
        clearTokens();
        window.location.href = '/login';
        throw refreshError;
      }
    }

    return response;
  }

  // GET 請求
  async get(url, options = {}) {
    return this.request(url, { method: 'GET', ...options });
  }

  // POST 請求
  async post(url, data, options = {}) {
    return this.request(url, {
      method: 'POST',
      body: JSON.stringify(data),
      ...options,
    });
  }

  // PUT 請求
  async put(url, data, options = {}) {
    return this.request(url, {
      method: 'PUT',
      body: JSON.stringify(data),
      ...options,
    });
  }

  // DELETE 請求
  async delete(url, options = {}) {
    return this.request(url, { method: 'DELETE', ...options });
  }
}

export const apiClient = new ApiClient();
```

### 3. 其他 Hook 的微調

```javascript
// use-products.js
import { apiClient } from './api';

export const useProducts = () => {
  const fetchProducts = async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const response = await apiClient.get(`/api/products?${query}`);
    return response.json();
  };

  // 其他方法保持不變，只是使用 apiClient 而不是直接 fetch
  return { fetchProducts };
};

// use-cart.js  
import { apiClient } from './api';

export const useCart = () => {
  const addToCart = async (item) => {
    const response = await apiClient.post('/api/cart/items', item);
    return response.json();
  };

  const getCartItems = async () => {
    const response = await apiClient.get('/api/cart');
    return response.json();
  };

  // 其他購物車方法...
  return { addToCart, getCartItems };
};
```

## 遷移步驟

### 第1步：更新登入邏輯
```javascript
// 舊代碼
const handleLogin = async (credentials) => {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials)
  });
  const data = await response.json();
  localStorage.setItem('token', data.data.token); // 舊格式
};

// 新代碼
const handleLogin = async (credentials) => {
  const data = await login(credentials);
  if (data.success) {
    // ✅ 自動存儲在 useAuth hook 中
    router.push('/dashboard');
  }
};
```

### 第2步：更新 API 調用
```javascript
// 舊代碼 - 手動管理 token
const fetchUserData = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('/api/auth/me', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
};

// 新代碼 - 自動管理 token
const fetchUserData = async () => {
  const response = await apiClient.get('/api/auth/me');
  // 自動處理 token 刷新
};
```

### 第3步：更新登出邏輯
```javascript
// 舊代碼
const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};

// 新代碼
const handleLogout = async () => {
  await logout(); // 會自動撤銷 refresh token
  router.push('/login');
};
```

## 性能優化

### 1. Token 過期檢查
```javascript
// 可選：在 useAuth 中添加 token 過期檢查
const isTokenExpired = (token) => {
  if (!token) return true;
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
};
```

### 2. 預防性刷新
```javascript
// 可選：在 token 過期前 1 分鐘自動刷新
const scheduleTokenRefresh = (token) => {
  const payload = JSON.parse(atob(token.split('.')[1]));
  const expiryTime = payload.exp * 1000;
  const refreshTime = expiryTime - 60000; // 提前1分鐘
  
  setTimeout(() => {
    refreshAccessToken().catch(console.error);
  }, refreshTime - Date.now());
};
```

## 安全考量

### 1. Token 存儲
- **Access Token**: 存儲在 `localStorage` 或 memory（更安全）
- **Refresh Token**: 存儲在 `localStorage` 或 `httpOnly cookie`（推薦）

### 2. 自動清理
```javascript
// 監聽頁面關閉，清理過期 token
window.addEventListener('beforeunload', () => {
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    // 可選：通知服務器撤銷 token
    navigator.sendBeacon('/api/auth/logout', 
      JSON.stringify({ refreshToken }));
  }
});
```

## 測試方案

### 1. 手動測試步驟
1. 登入後獲得雙 token
2. 等待 Access Token 過期（15分鐘）
3. 發起任何需要認證的請求
4. 驗證自動刷新機制是否正常

### 2. 模擬過期測試
```javascript
// 在開發環境中，可以修改配置文件縮短過期時間進行測試
jwt.access-token.expiration=10000  // 10秒，僅用於測試
```

## 總結

### 前端改動清單：
**最小改動**：
1. 修改登入後的 token 存儲邏輯
2. 創建 API 客戶端（自動刷新）
3. 更新登出邏輯
4. 調整其他 hooks 使用新的 API 客戶端

**無需改動**：
- 組件結構
- 路由配置  
- 狀態管理架構
- UI/UX 邏輯

### 優勢：
- **更安全**：短期 Access Token 降低風險
- **用戶友好**：長期 Refresh Token 減少重複登入
- **自動化**：透明的 token 刷新機制
- **可控制**：可隨時撤銷 Refresh Token
