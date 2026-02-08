package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.entity.Dog;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.DogService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member/dogs")
@RequiredArgsConstructor
@Tag(name = "會員寵物", description = "會員寵物管理 API")
public class MemberDogController {

    private final DogService dogService;

    @Operation(summary = "取得我的寵物列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyDogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<Dog> dogs = dogService.getMyDogs(user.getId());
        return ResponseEntity.ok(Map.of("data", dogs));
    }

    @Operation(summary = "新增寵物 (FormData)")
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addDog(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String name,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String size_id,
            @RequestParam(required = false) List<MultipartFile> dog_images) {

        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Dog dog = new Dog();
        dog.setMemberId(user.getId());
        dog.setName(name);
        if (breed != null) dog.setBreed(breed);
        if (age != null) {
            try { dog.setAge(Integer.parseInt(age)); } catch (NumberFormatException ignored) {}
        }
        if (description != null) dog.setDescription(description);

        Dog created = dogService.addDog(dog);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", "新增狗狗成功");
        result.put("dog", created);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "刪除寵物")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        dogService.deleteDog(id, user.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", "刪除成功");
        return ResponseEntity.ok(result);
    }
}
