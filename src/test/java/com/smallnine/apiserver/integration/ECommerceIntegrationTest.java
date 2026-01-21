package com.smallnine.apiserver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
// import com.smallnine.apiserver.constants.enums.OrderStatus;
import com.smallnine.apiserver.dto.CartItemRequest;
import com.smallnine.apiserver.dto.LoginRequest;
import com.smallnine.apiserver.dto.OrderRequest;
import com.smallnine.apiserver.dto.RegisterRequest;
import com.smallnine.apiserver.entity.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ECommerceIntegrationTest extends AbstractIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @Disabled("TODO: 需要重寫測試，使用真實 JWT token 而非 mock token")
    public void testCompleteECommerceFlow() throws Exception {
        String userToken = registerAndLoginUser();
        Long categoryId = createCategory();
        Long brandId = createBrand();
        Long productId = createProduct(categoryId, brandId);
        addProductToCart(userToken, productId);
        createOrder(userToken);
    }
    
    private String registerAndLoginUser() throws Exception {
        // 註冊用戶（使用唯一用戶名避免衝突）
        String uniqueSuffix = String.valueOf(System.nanoTime() % 100000);
        String username = "user" + uniqueSuffix;
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail("test" + uniqueSuffix + "@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRealname("Test User");
        
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();
        
        System.out.println("Register Response Status: " + registerResult.getResponse().getStatus());
        System.out.println("Register Response Body: " + registerResult.getResponse().getContentAsString());
        
        if (registerResult.getResponse().getStatus() != 201) {
            throw new RuntimeException("Registration failed with status: " + registerResult.getResponse().getStatus() +
                ", body: " + registerResult.getResponse().getContentAsString());
        }

        // 登錄用戶
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword("password123");
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        // 提取JWT token (簡化處理)
        return "mock-jwt-token";
    }
    
    private Long createCategory() throws Exception {
        Category category = new Category();
        category.setName("測試分類");
        category.setDescription("測試分類描述");
        category.setIsActive(true);
        
        MvcResult result = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category))
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isCreated())
                .andReturn();
        
        return 1L; // 簡化返回
    }
    
    private Long createBrand() throws Exception {
        Brand brand = new Brand();
        brand.setName("測試品牌");
        brand.setDescription("測試品牌描述");
        
        mockMvc.perform(post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(brand))
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isCreated());
        
        return 1L; // 簡化返回
    }
    
    private Long createProduct(Long categoryId, Long brandId) throws Exception {
        Product product = new Product();
        product.setName("測試商品");
        product.setDescription("測試商品描述");
        product.setPrice(new BigDecimal("99.99"));
        product.setSku("TEST-001");
        product.setStockQuantity(100);
        product.setCategoryId(categoryId);
        product.setBrandId(brandId);
        product.setIsActive(true);
        
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isCreated());
        
        return 1L; // 簡化返回
    }
    
    private void addProductToCart(String userToken, Long productId) throws Exception {
        CartItemRequest cartItem = new CartItemRequest();
        cartItem.setProductId(productId);
        cartItem.setQuantity(2);
        
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartItem))
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }
    
    private void createOrder(String userToken) throws Exception {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setShippingAddress("測試地址");
        orderRequest.setPaymentMethod("CREDIT_CARD");
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest))
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());
    }
}