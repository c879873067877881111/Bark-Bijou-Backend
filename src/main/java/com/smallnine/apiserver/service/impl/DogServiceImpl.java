package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.DogDao;
import com.smallnine.apiserver.entity.Dog;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.DogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DogServiceImpl implements DogService {

    private final DogDao dogDao;

    @Override
    public List<Dog> getMyDogs(Long memberId) {
        return dogDao.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public Dog addDog(Dog dog) {
        dogDao.insert(dog);
        return dog;
    }

    @Override
    @Transactional
    public Dog updateDog(Long id, Dog dog, Long memberId) {
        Dog existing = dogDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DOG_NOT_FOUND));
        if (!existing.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        dog.setId(id);
        dog.setMemberId(memberId);
        dogDao.update(dog);
        return dogDao.findById(id).orElse(dog);
    }

    @Override
    @Transactional
    public void deleteDog(Long id, Long memberId) {
        Dog existing = dogDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DOG_NOT_FOUND));
        if (!existing.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        dogDao.deleteById(id);
    }
}
