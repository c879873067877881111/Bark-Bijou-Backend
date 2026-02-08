package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Recipient;

import java.util.List;

public interface RecipientService {

    List<Recipient> getByMemberId(Long memberId);

    Recipient add(Recipient recipient);

    Recipient update(Long id, Recipient recipient, Long memberId);

    void delete(Long id, Long memberId);
}
