package com.xprod.spring.xprod.domain;

import java.util.Collection;
import java.util.stream.Collectors;
import static java.util.Arrays.stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails{ // add methode unumplement
	
	private User user;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() { 
		
		return stream(this.user.getAuthorities()).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		
		return this.user.getPassword();
	}

	@Override
	public String getUsername() {
		
		return this.user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		
		return true;  //changer en true
	}

	@Override
	public boolean isAccountNonLocked() {
		
		return this.user.isNotLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		
		return true;
	}

	@Override
	public boolean isEnabled() {
		
		return this.user.isActive();
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public UserPrincipal(User user) {
		super();
		this.user = user;
	}

	public UserPrincipal() {
		super();
	}
	
	

}
