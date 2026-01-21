package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.CartItemRequest;
import com.smallnine.apiserver.entity.CartItem;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.impl.CartServiceImpl;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "購物車管理", description = "購物車相關 API - 添加、移除、更新購物車項目")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartServiceImpl cartService;

    @Operation(summary = "獲取購物車", description = "獲取當前用戶的購物車所有項目")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取購物車"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItem>>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<CartItem> cartItems = cartService.getCartItems(user.getId());
        return ResponseEntity.ok(ApiResponse.success(cartItems));
    }

    @Operation(summary = "添加商品到購物車", description = "將指定商品添加到購物車")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "商品添加成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數無效或庫存不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItem>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        CartItem cartItem = cartService.addToCart(user.getId(), request.getProductId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("商品添加到購物車成功", cartItem));
    }

    @Operation(summary = "更新購物車項目數量", description = "更新購物車中指定商品的數量")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "數量更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "數量無效或庫存不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "購物車項目不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartItem>> updateCartItemQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        CartItem cartItem = cartService.updateCartItemQuantity(user.getId(), cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("購物車項目數量更新成功", cartItem));
    }

    @Operation(summary = "從購物車移除項目", description = "從購物車中移除指定的商品項目")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "項目移除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "購物車項目不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        cartService.removeFromCart(user.getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("項目已從購物車移除"));
    }

    @Operation(summary = "清空購物車", description = "清空當前用戶的購物車所有項目")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "購物車清空成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        cartService.clearCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success("購物車已清空"));
    }

    @Operation(summary = "計算購物車總金額", description = "計算當前用戶購物車的總金額")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "計算成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getCartTotal(@AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        BigDecimal total = cartService.calculateCartTotal(user.getId());
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @Operation(summary = "驗證購物車庫存", description = "驗證購物車中所有商品的庫存是否充足")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "驗證完成"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @GetMapping("/validate-stock")
    public ResponseEntity<ApiResponse<Boolean>> validateCartStock(@AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        boolean stockValid = cartService.validateCartStock(user.getId());
        return ResponseEntity.ok(ApiResponse.success(stockValid));
    }

    @Operation(summary = "獲取購物車項目數量", description = "獲取當前用戶購物車中的項目總數")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCartItemCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        long count = cartService.countCartItems(user.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

}
