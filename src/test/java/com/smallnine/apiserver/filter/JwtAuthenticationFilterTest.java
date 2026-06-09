package com.smallnine.apiserver.filter;

import com.smallnine.apiserver.logging.AuditLogger;
import com.smallnine.apiserver.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * filter 拿掉 isAccessToken 守衛後的契約。
 * refresh token 已是 UUID（非 JWT），那道 type 檢查是 dead defense；
 * 本測試鎖住「合法 token 仍正常認證、驗證失敗不認證、無 Bearer 不認證」。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    @Test
    void validToken_authenticates() throws Exception {
        UserDetails ud = new User("alice", "pw", List.of());
        when(jwtUtil.extractUsername("tok")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(ud);
        when(jwtUtil.validateToken("tok", ud)).thenReturn(true);

        MockHttpServletRequest req = bearer("tok");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void invalidToken_doesNotAuthenticate() throws Exception {
        UserDetails ud = new User("alice", "pw", List.of());
        when(jwtUtil.extractUsername("tok")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(ud);
        when(jwtUtil.validateToken("tok", ud)).thenReturn(false);

        MockHttpServletRequest req = bearer("tok");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void noBearerHeader_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void malformedToken_doesNotAuthenticate_andAudits() throws Exception {
        // extractUsername 丟例外 → username 留 null → 不查 user、不認證，但有稽核
        when(jwtUtil.extractUsername("bad")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        MockHttpServletRequest req = bearer("bad");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
        verify(auditLogger).logAccessDenied(any(), any());
        verify(chain).doFilter(any(), any());
    }
}