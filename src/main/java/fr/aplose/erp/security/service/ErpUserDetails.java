package fr.aplose.erp.security.service;

import fr.aplose.erp.security.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom UserDetails that carries ERP-specific context (tenantId, locale, etc.)
 * so it doesn't need to be reloaded from DB on every request.
 */
@Getter
public class ErpUserDetails implements UserDetails {

    private final Long userId;
    private final String tenantId;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final String locale;
    private final String timezone;
    private final boolean active;
    private final boolean locked;
    private final Collection<GrantedAuthority> authorities;

    public ErpUserDetails(User user) {
        this.userId      = user.getId();
        this.tenantId    = user.getTenantId();
        this.username    = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.locale      = user.getLocale();
        this.timezone    = user.getTimezone();
        this.active      = user.isActive();
        this.locked      = user.isLocked();
        this.authorities = buildAuthorities(user);
    }

    private static Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> {
            // Spring role prefix
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            // Individual permission codes as authorities
            role.getPermissions().forEach(perm ->
                authorities.add(new SimpleGrantedAuthority(perm.getCode()))
            );
        });
        return authorities;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()  { return passwordHash; }
    @Override public String getUsername()  { return username; }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return !locked; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return active; }
}
