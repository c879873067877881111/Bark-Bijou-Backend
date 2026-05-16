package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.entity.Dog;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.DogService;
import com.smallnine.apiserver.service.FileStorageService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/member/dogs")
@RequiredArgsConstructor
@Tag(name = "會員寵物", description = "會員寵物管理 API")
public class MemberDogController {

    private final DogService dogService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "取得我的寵物列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Dog>>> getMyDogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<Dog> dogs = dogService.getMyDogs(user.getId());
        return ResponseEntity.ok(ApiResponse.success(dogs));
    }

    @Operation(summary = "新增寵物 (FormData)")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Dog>> addDog(
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

        if (dog_images != null && !dog_images.isEmpty()) {
            MultipartFile first = null;
            for (MultipartFile f : dog_images) {
                if (!f.isEmpty()) {
                    first = f;
                    break;
                }
            }
            if (first != null) {
                String imageUrl = fileStorageService.store(first, "dogs");
                dog.setImageUrl(imageUrl);
            }
        }

        Dog created = dogService.addDog(dog);
        return ResponseEntity.ok(ApiResponse.success("新增狗狗成功", created));
    }

    @Operation(summary = "刪除寵物")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        dogService.deleteDog(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("刪除成功"));
    }
}
