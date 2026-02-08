package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.DogDao;
import com.smallnine.apiserver.dao.SitterBookingDao;
import com.smallnine.apiserver.dao.SitterDao;
import com.smallnine.apiserver.dto.SitterBookingRequest;
import com.smallnine.apiserver.entity.Dog;
import com.smallnine.apiserver.entity.SitterBooking;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.SitterBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SitterBookingServiceImpl implements SitterBookingService {

    private final SitterBookingDao sitterBookingDao;
    private final SitterDao sitterDao;
    private final DogDao dogDao;

    @Override
    @Transactional
    public SitterBooking createBooking(Long sitterId, SitterBookingRequest request, Long memberId) {
        sitterDao.findById(sitterId)
                .orElseThrow(() -> new BusinessException(ResponseCode.SITTER_NOT_FOUND));

        Dog dog = dogDao.findById(request.getPetId())
                .orElseThrow(() -> new BusinessException(ResponseCode.DOG_NOT_FOUND));
        if (!dog.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }

        LocalDateTime startTime = LocalDate.parse(request.getStartDate()).atTime(LocalTime.MIN);
        LocalDateTime endTime = LocalDate.parse(request.getEndDate()).atTime(LocalTime.MAX);

        SitterBooking booking = new SitterBooking();
        booking.setMemberId(memberId);
        booking.setSitterId(sitterId);
        booking.setPetId(request.getPetId());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        sitterBookingDao.insert(booking);
        return booking;
    }
}
