package com.smallnine.apiserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.CartItemRequest;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Product;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ECommerceFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testCompleteECommerceFlow() throws Exception {
        String userToken = createUserAndGetToken();
        Long productId = getExistingProductId();

        addProductToCart(userToken, productId);
        verifyCart(userToken);
        createOrder(userToken);
    }

    private String createUserAndGetToken() {
        String suffix = String.valueOf(System.nanoTime() % 100000);
        User user = new User();
        user.setUsername("ecom_test_" + suffix);
        user.setEmail("ecom_" + suffix + "@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRealname("E-Commerce Test User");
        user.setRole(User.Role.USER);
        user.setEmailValidated(true);
        userDao.insert(user);
        return jwtUtil.generateAccessToken(user.getUsername());
    }

    private Long getExistingProductId() {
        Product product = productDao.findAll(0, 1).stream()
                .filter(Product::getIsActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active product in DB for integration test"));
        return product.getId();
    }

    private void addProductToCart(String token, Long productId) throws Exception {
        CartItemRequest cartItem = new CartItemRequest();
        cartItem.setProductId(productId);
        cartItem.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItem))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private void verifyCart(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(root.path("data").isArray());
        assertFalse(root.path("data").isEmpty());
    }

    private void createOrder(String token) throws Exception {
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setShippingAddress("測試地址");

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(root.path("data").path("id").asLong());
        assertTrue(root.path("data").path("id").asLong() > 0);
    }
}
