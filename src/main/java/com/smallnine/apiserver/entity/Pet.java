package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pet {
    
    private Long id;
    
    private Long memberId;
    
    private String name;
    
    private PetType type;
    
    private String breed;
    
    private Gender gender;
    
    private LocalDate birthday;
    
    private String photo;
    
    private String notes;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PetType {
        DOG, CAT, BIRD, FISH, RABBIT, OTHER
    }
    
    public enum Gender {
        MALE, FEMALE
    }
}