package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.SitterBookingDao;
import com.smallnine.apiserver.dao.SitterDao;
import com.smallnine.apiserver.dao.SitterGalleryDao;
import com.smallnine.apiserver.dao.SitterReviewDao;
import com.smallnine.apiserver.dto.SitterListResponse;
import com.smallnine.apiserver.dto.SitterRequest;
import com.smallnine.apiserver.dto.SitterResponse;
import com.smallnine.apiserver.dto.SitterReviewResponse;
import com.smallnine.apiserver.entity.Sitter;
import com.smallnine.apiserver.entity.SitterGallery;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.SitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SitterServiceImpl implements SitterService {

    private final SitterDao sitterDao;
    private final SitterGalleryDao sitterGalleryDao;
    private final SitterReviewDao sitterReviewDao;
    private final SitterBookingDao sitterBookingDao;

    @Override
    public List<SitterReviewResponse> getRecentReviews(int limit) {
        return sitterReviewDao.findRecentWithMember(limit);
    }

    @Override
    public SitterListResponse searchSitters(String search, String area, String sort, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Sitter> sitters = sitterDao.searchSitters(search, area, sort, offset, pageSize);
        int total = sitterDao.countSitters(search, area);

        List<SitterResponse> data = sitters.stream().map(s -> {
            SitterResponse resp = SitterResponse.from(s);
            Double avg = sitterReviewDao.getAverageRating(s.getId());
            resp.setRating(avg != null ? avg : 0.0);
            return resp;
        }).toList();

        return new SitterListResponse(total, page, pageSize, data);
    }

    @Override
    public SitterResponse getSitterDetail(Long id, Long currentMemberId) {
        Sitter sitter = sitterDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.SITTER_NOT_FOUND));

        SitterResponse resp = SitterResponse.from(sitter);

        Double avg = sitterReviewDao.getAverageRating(id);
        resp.setRating(avg != null ? avg : 0.0);
        resp.setReviewCount(sitterReviewDao.countBySitterId(id));

        List<SitterGallery> galleryEntities = sitterGalleryDao.findBySitterId(id);
        resp.setGallery(galleryEntities.stream().map(SitterGallery::getImageUrl).toList());

        resp.setReviews(sitterReviewDao.findBySitterIdWithMember(id));

        if (currentMemberId != null) {
            boolean hasBooked = sitterBookingDao.existsByMemberAndSitter(currentMemberId, id);
            boolean hasReviewed = sitterReviewDao.existsByMemberAndSitter(currentMemberId, id);
            if (hasReviewed) {
                resp.setReviewStatus("already");
            } else if (hasBooked) {
                resp.setReviewStatus("ok");
            } else {
                resp.setReviewStatus("no_booking");
            }
        } else {
            resp.setReviewStatus("unauthorized");
        }

        return resp;
    }

    @Override
    public SitterResponse getMySitter(Long memberId) {
        return sitterDao.findByMemberId(memberId)
                .map(sitter -> {
                    SitterResponse resp = SitterResponse.from(sitter);
                    Double avg = sitterReviewDao.getAverageRating(sitter.getId());
                    resp.setRating(avg != null ? avg : 0.0);
                    return resp;
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public Sitter createSitter(SitterRequest request, Long memberId) {
        if (sitterDao.findByMemberId(memberId).isPresent()) {
            throw new BusinessException(ResponseCode.SITTER_ALREADY_EXISTS);
        }

        Sitter sitter = new Sitter();
        sitter.setMemberId(memberId);
        sitter.setName(request.getName());
        sitter.setArea(request.getArea());
        sitter.setServiceTime(request.getServiceTime());
        sitter.setExperience(request.getExperience());
        sitter.setIntroduction(request.getIntroduction());
        sitter.setPrice(request.getPrice());
        sitter.setAvatarUrl(request.getAvatarUrl());
        sitterDao.insert(sitter);
        return sitter;
    }

    @Override
    @Transactional
    public Sitter updateSitter(Long id, SitterRequest request, Long memberId) {
        Sitter sitter = sitterDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.SITTER_NOT_FOUND));

        if (!sitter.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.SITTER_FORBIDDEN);
        }

        sitter.setName(request.getName());
        sitter.setArea(request.getArea());
        sitter.setServiceTime(request.getServiceTime());
        sitter.setExperience(request.getExperience());
        sitter.setIntroduction(request.getIntroduction());
        sitter.setPrice(request.getPrice());
        sitter.setAvatarUrl(request.getAvatarUrl());
        sitterDao.update(sitter);
        return sitter;
    }

    @Override
    @Transactional
    public void deleteSitter(Long id, Long memberId) {
        Sitter sitter = sitterDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.SITTER_NOT_FOUND));

        if (!sitter.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.SITTER_FORBIDDEN);
        }

        sitterDao.deleteById(id);
    }

    @Override
    @Transactional
    public void addReview(Long sitterId, Integer rating, String comment, Long memberId) {
        sitterDao.findById(sitterId)
                .orElseThrow(() -> new BusinessException(ResponseCode.SITTER_NOT_FOUND));

        if (!sitterBookingDao.existsByMemberAndSitter(memberId, sitterId)) {
            throw new BusinessException(ResponseCode.REVIEW_NO_BOOKING);
        }

        if (sitterReviewDao.existsByMemberAndSitter(memberId, sitterId)) {
            throw new BusinessException(ResponseCode.REVIEW_ALREADY_EXISTS);
        }

        sitterReviewDao.insert(memberId, sitterId, rating, comment);
    }
}
