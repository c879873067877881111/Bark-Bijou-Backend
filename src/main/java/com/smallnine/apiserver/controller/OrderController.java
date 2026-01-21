package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.entity.OrderItem;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.impl.OrderServiceImpl;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "訂單管理", description = "訂單相關 API - 創建、查詢、更新、取消訂單")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderServiceImpl orderService;

    @Operation(summary = "獲取用戶訂單列表", description = "分頁獲取當前用戶的訂單列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取訂單列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<Order> orders = orderService.findUserOrders(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @Operation(summary = "根據ID獲取訂單", description = "根據訂單ID獲取訂單詳情")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取訂單"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限訪問"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Order order = orderService.findById(id);
        verifyOrderOwnership(order, user.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @Operation(summary = "根據訂單號獲取訂單", description = "根據訂單號獲取訂單詳情")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取訂單"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限訪問"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNumber(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderNumber) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Order order = orderService.findByOrderNumber(orderNumber);
        verifyOrderOwnership(order, user.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @Operation(summary = "獲取訂單項目", description = "獲取指定訂單的所有訂單項目")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取訂單項目"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限訪問"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    @GetMapping("/{orderId}/items")
    public ResponseEntity<ApiResponse<List<OrderItem>>> getOrderItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Order order = orderService.findById(orderId);
        verifyOrderOwnership(order, user.getId());
        List<OrderItem> orderItems = orderService.findOrderItems(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderItems));
    }

    @Operation(summary = "從購物車創建訂單", description = "從當前用戶的購物車創建新訂單")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "訂單創建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "購物車為空或庫存不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Order order = orderService.createOrderFromCart(
                user.getId(),
                request.getShippingAddress(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("訂單創建成功", order));
    }

    @Operation(summary = "更新訂單狀態", description = "更新訂單狀態（管理員功能）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "訂單狀態更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "狀態無效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam Long statusId) {
        orderService.updateOrderStatus(orderId, statusId);
        return ResponseEntity.ok(ApiResponse.success("訂單狀態更新成功"));
    }

    @Operation(summary = "取消訂單", description = "取消指定的訂單")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "訂單取消成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單狀態不允許取消"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作此訂單"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("訂單取消成功"));
    }

    @Operation(summary = "刪除訂單", description = "刪除指定的訂單（管理員功能）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "訂單刪除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("訂單刪除成功"));
    }

    @Operation(summary = "統計用戶訂單數量", description = "獲取當前用戶的訂單總數")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權")
    })
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserOrderCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        long count = orderService.countUserOrders(user.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 驗證訂單所有權
     */
    private void verifyOrderOwnership(Order order, Long userId) {
        if (!order.getMemberId().equals(userId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "無權限訪問此訂單");
        }
    }
}
