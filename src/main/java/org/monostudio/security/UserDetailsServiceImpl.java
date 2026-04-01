package org.monostudio.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.monostudio.config.SecurityProperties;
import org.monostudio.jpa.entities.Permission;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.UserRolePermissionsRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.security.services.UserPermissionsService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service required by the DaoAuthenticationProvider bean.
 */
@Service
public class UserDetailsServiceImpl
    implements UserDetailsService {
    private final UsersRepository usersRepository;
    private final UserRolePermissionsRepository rolePermissionsRepository;
    private final UserPermissionsService userPermissionsService;
    private final SecurityProperties securityProperties;

    @Autowired
    public UserDetailsServiceImpl(
        UsersRepository usersRepository,
        UserRolePermissionsRepository rolePermissionsRepository,
        UserPermissionsService userPermissionsService,
        SecurityProperties securityProperties
    ) {
        this.usersRepository = usersRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
        this.userPermissionsService = userPermissionsService;
        this.securityProperties = securityProperties;
    }

    @Override
    public UserDetailsPojo loadUserByUsername(String username) throws UsernameNotFoundException {
        if (securityProperties.isGuestUserEnabled() &&
            username.equals(securityProperties.getGuestUserName())) {
            return this.loadGuestUserDetails();
        }
        Optional<User> foundUser = usersRepository.findByNameWithRole(username);
        if (foundUser.isPresent()) {
            User user = foundUser.get();
            return this.loadRegularUserDetails(user);
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    private UserDetailsPojo loadRegularUserDetails(User user) {
        Set<Permission> permissions = userPermissionsService.loadPermissionsForUser(user);
        List<SimpleGrantedAuthority> authorities = permissions.stream()
            .map(source -> new SimpleGrantedAuthority(source.getCode()))
            .collect(Collectors.toList());
        return UserDetailsPojo.builder()
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .enabled(true)
            .authorities(authorities)
            .username(user.getName())
            .password(user.getPassword())
            .build();
    }

    private UserDetailsPojo loadGuestUserDetails() {
        long guestUserRoleId = securityProperties.getGuestUserRoleId();
        List<SimpleGrantedAuthority> authorities = rolePermissionsRepository.deepFindPermissionsByUserRoleId(guestUserRoleId).stream()
            .map(rolePermission -> new SimpleGrantedAuthority(rolePermission.getPermission().getCode()))
            .collect(Collectors.toList());
        return UserDetailsPojo.builder()
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .enabled(true)
            .authorities(authorities)
            .username(securityProperties.getGuestUserName())
            .password(securityProperties.getGuestUserName())
            .build();
    }
}
