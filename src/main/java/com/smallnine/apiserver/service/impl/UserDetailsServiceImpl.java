package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用戶不存在: " + username));
        return new UserPrincipal(user);
    }
}
