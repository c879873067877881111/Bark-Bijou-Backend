package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.RecipientDao;
import com.smallnine.apiserver.entity.Recipient;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.RecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipientServiceImpl implements RecipientService {

    private final RecipientDao recipientDao;

    @Override
    public List<Recipient> getByMemberId(Long memberId) {
        return recipientDao.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public Recipient add(Recipient recipient) {
        if (Boolean.TRUE.equals(recipient.getIsDefault())) {
            recipientDao.clearDefault(recipient.getMemberId());
        }
        recipientDao.insert(recipient);
        return recipient;
    }

    @Override
    @Transactional
    public Recipient update(Long id, Recipient recipient, Long memberId) {
        Recipient existing = recipientDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RECIPIENT_NOT_FOUND));
        if (!existing.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(recipient.getIsDefault())) {
            recipientDao.clearDefault(memberId);
        }
        recipient.setId(id);
        recipient.setMemberId(memberId);
        recipientDao.update(recipient);
        return recipientDao.findById(id).orElse(recipient);
    }

    @Override
    @Transactional
    public void delete(Long id, Long memberId) {
        Recipient existing = recipientDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RECIPIENT_NOT_FOUND));
        if (!existing.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        recipientDao.deleteById(id);
    }
}
