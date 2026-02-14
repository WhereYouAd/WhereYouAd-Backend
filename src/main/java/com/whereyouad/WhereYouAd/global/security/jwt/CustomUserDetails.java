package com.whereyouad.WhereYouAd.global.security.jwt;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Provider provider;

    // 일단 User 엔티티에 권한 구분(ADMIN / USER) 가 없기도 하고,
    // 팀장, 멤버등의 역할 구분은 2차 MVP 에서 진행한다.
    // 따라서 일단은 편의를 위해 User 를 모두 사용자(ROLE_USER) 권한을 갖도록 진행
    // 개발자용 페이지를 따로 만든다면 해당 메서드 및 User 엔티티 변경 필요
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return List.of();
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠김 여부 (true: 안 잠김)
    @Override
    public boolean isAccountNonLocked() {
        return true; // 나중에 로그인 실패 5회 시 잠금 로직 등이 필요하면 user.isLocked() 등으로 교체
    }

    // 비밀번호 만료 여부 (true: 만료 안 됨)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    // 편의 메서드: 컨트롤러에서 @AuthenticationPrincipal로 ID만 바로 꺼내 쓸 때 유용
    public Long getUserId() {
        return user.getId();
    }
}
