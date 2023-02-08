package com.xprod.spring.xprod.constant.filter.listener;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.event.*;
import com.xprod.spring.xprod.service.LoginAttemptService;

@Component
public class AuthenticationFailureListener {
	

	private LoginAttemptService loginAttemptService;
@Autowired
	public AuthenticationFailureListener(LoginAttemptService loginAttemptService) {
		super();
		this.loginAttemptService = loginAttemptService;
	}
@EventListener
public void onAuthenticationFaillure(AuthenticationFailureBadCredentialsEvent event) throws ExecutionException {
	Object principal = event.getAuthentication().getPrincipal();
	if (principal instanceof String) {
		
		String username= (String)event.getAuthentication().getPrincipal();
		loginAttemptService.addUserToLoginAttemptCache(username);
	}
}
}

