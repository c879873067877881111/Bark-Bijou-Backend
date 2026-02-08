package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.SitterBookingRequest;
import com.smallnine.apiserver.entity.Dog;
import com.smallnine.apiserver.entity.SitterBooking;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.dao.SitterDao;
import com.smallnine.apiserver.dao.DogDao;
import com.smallnine.apiserver.service.DogService;
import com.smallnine.apiserver.service.SitterBookingService;
import com.smallnine.apiserver.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sitter-booking")
@RequiredArgsConstructor
public class SitterBookingController {

    private final SitterBookingService sitterBookingService;
    private final DogService dogService;
    private final SitterDao sitterDao;
    private final DogDao dogDao;

    @GetMapping("/dogs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyDogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<Dog> dogs = dogService.getMyDogs(user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        if (dogs.isEmpty()) {
            result.put("status", "empty");
            result.put("dogs", List.of());
        } else {
            result.put("status", "success");
            result.put("dogs", dogs);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/dogs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addDog(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        String name = body.get("name");
        Dog dog = new Dog();
        dog.setMemberId(user.getId());
        dog.setName(name);
        dog = dogService.addDog(dog);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("dog", dog);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("新增狗狗成功", result));
    }

    @PostMapping("/{sitterId}/bookings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(
            @PathVariable Long sitterId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SitterBookingRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        SitterBooking booking = sitterBookingService.createBooking(sitterId, request, user.getId());

        var sitter = sitterDao.findById(sitterId).orElse(null);
        var dog = dogDao.findById(request.getPetId()).orElse(null);

        Map<String, Object> bookingData = new LinkedHashMap<>();
        bookingData.put("booking_id", booking.getId());
        bookingData.put("username", user.getUsername());
        bookingData.put("email", user.getEmail());
        bookingData.put("dog_name", dog != null ? dog.getName() : "");
        bookingData.put("sitter_name", sitter != null ? sitter.getName() : "");
        bookingData.put("start_date", request.getStartDate());
        bookingData.put("end_date", request.getEndDate());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("booking", bookingData);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("預約成功", result));
    }
}
