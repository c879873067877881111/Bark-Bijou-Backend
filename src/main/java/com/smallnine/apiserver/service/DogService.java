package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Dog;

import java.util.List;

public interface DogService {

    List<Dog> getMyDogs(Long memberId);

    Dog addDog(Dog dog);

    Dog updateDog(Long id, Dog dog, Long memberId);

    void deleteDog(Long id, Long memberId);
}
