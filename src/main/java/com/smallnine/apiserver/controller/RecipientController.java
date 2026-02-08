package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.RecipientRequest;
import com.smallnine.apiserver.entity.Recipient;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.RecipientService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/me/recipients")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "收件人管理", description = "會員收件人 CRUD API")
public class RecipientController {

    private final RecipientService recipientService;

    @Operation(summary = "取得收件人列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Recipient>>> getRecipients(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(recipientService.getByMemberId(user.getId())));
    }

    @Operation(summary = "新增收件人")
    @PostMapping
    public ResponseEntity<ApiResponse<Recipient>> addRecipient(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RecipientRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Recipient recipient = new Recipient();
        recipient.setMemberId(user.getId());
        recipient.setName(request.getName());
        recipient.setPhone(request.getPhone());
        recipient.setCity(request.getCity());
        recipient.setTown(request.getTown());
        recipient.setAddress(request.getAddress());
        recipient.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        Recipient created = recipientService.add(recipient);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("新增成功", created));
    }

    @Operation(summary = "修改收件人")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipient>> updateRecipient(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody RecipientRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Recipient recipient = new Recipient();
        recipient.setName(request.getName());
        recipient.setPhone(request.getPhone());
        recipient.setCity(request.getCity());
        recipient.setTown(request.getTown());
        recipient.setAddress(request.getAddress());
        recipient.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        Recipient updated = recipientService.update(id, recipient, user.getId());
        return ResponseEntity.ok(ApiResponse.success("修改成功", updated));
    }

    @Operation(summary = "刪除收件人")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRecipient(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        recipientService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("刪除成功"));
    }
}
