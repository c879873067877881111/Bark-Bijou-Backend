package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.EcpayCreateRequest;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.EcpayService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ecpay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "金流", description = "ECPay 金流整合 API")
public class PaymentController {

    private final EcpayService ecpayService;

    @Operation(summary = "建立 ECPay 付款訂單")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, String>>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EcpayCreateRequest request) {
        AuthUtils.getAuthenticatedUser(userDetails);
        String formHtml = ecpayService.createPaymentForm(
                request.getOrderId(),
                request.getTotalAmount().intValue(),
                request.getItemName()
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of("htmlForm", formHtml)));
    }

    @Operation(summary = "ECPay 回呼")
    @PostMapping("/callback")
    public String ecpayCallback(@RequestParam Map<String, String> params) {
        log.info("ECPay callback received: {}", params);
        boolean success = ecpayService.handleCallback(params);
        // ECPay expects "1|OK" response
        return success ? "1|OK" : "0|FAIL";
    }
}
