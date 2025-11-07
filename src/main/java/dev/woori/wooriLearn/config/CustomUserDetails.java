package dev.woori.wooriLearn.config;

import dev.woori.wooriLearn.domain.user.entity.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Users user;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Users user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public Long getId() { return user.getId(); }

    @Override public String getUsername() { return user.getUserId(); }
    @Override public String getPassword() { return user.getPassword(); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override public boolean isAccountNonExpired() { return user.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked() { return user.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired() { return user.isCredentialsNonExpired(); }
    @Override public boolean isEnabled() { return user.isEnabled(); }
}
