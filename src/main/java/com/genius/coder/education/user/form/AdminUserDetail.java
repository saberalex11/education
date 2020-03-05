package com.genius.coder.education.user.form;

import com.genius.coder.education.user.enums.StatusEnum;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * @author GaoWeicai.(lili14520 @ gmail.com)
 * @date 2020/3/5
 */
@NoArgsConstructor
public class AdminUserDetail implements UserDetails, Serializable {

    private AdminUserForm user;

    private String password;

    public AdminUserDetail(AdminUserForm user) {
        Assert.notNull(user, "用户信息不能为空");
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password == null ? user.getPassword() : password;
    }

    @Override
    public String getUsername() {
        return user.getPhoneNum();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == StatusEnum.ENABLE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == StatusEnum.ENABLE;
    }

    public AdminUserForm getUser() {
        return user;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}