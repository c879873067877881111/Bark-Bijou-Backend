package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dto.SitterBookingRequest;
import com.smallnine.apiserver.entity.SitterBooking;

public interface SitterBookingService {

    SitterBooking createBooking(Long sitterId, SitterBookingRequest request, Long memberId);
}
