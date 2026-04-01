package org.monostudio.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Data
@Builder
public class UserDetailsPojo
    implements UserDetails {
    private static final long serialVersionUID = 1L;
    private final List<? extends GrantedAuthority> authorities;
    private final String username;
    private final String password;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
}
