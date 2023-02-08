package com.xprod.spring.xprod.service.impl;


import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.xprod.spring.xprod.domain.User;
import com.xprod.spring.xprod.domain.UserPrincipal;
import com.xprod.spring.xprod.exception.domain.EmailExistException;
import com.xprod.spring.xprod.exception.domain.UserNotFoundException;
import com.xprod.spring.xprod.exception.domain.UsernameExistException;
import com.xprod.spring.xprod.repository.IUserRepository;
import com.xprod.spring.xprod.service.EmailService;
import com.xprod.spring.xprod.service.IUserService;
import com.xprod.spring.xprod.service.LoginAttemptService;

import jakarta.transaction.Transactional;

import static com.xprod.spring.xprod.enumeration.Role.*;


@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements IUserService, UserDetailsService{
	

	private static final String DEFAULT_USER_IMAGE_PATH = "/user/image/profile/temp";
	private static final String UN_UTILISATEUR_AVEC_CETTE_ADRESSE_MAIL_EXIST_DÉJÀ = "Un utilisateur avec cette adresse mail exist déjà";
	private static final String L_USERNAME_EXISTE_DÉJÀ = "L'username existe déjà";
	private static final String PAS_D_UTILISATEUR_TROUVÉ_AVEC_CET_USERNAME = "Pas d'utilisateur trouvé avec cet username";
	private static final String RETURNING_TROUVER_L_UTILISATEUR_PAR_L_USERNAME = "Returning : trouver l'utilisateur par l'username : ";
	private static final String NEW_USER_PASSWORD = "New user password : ";
	private static final String USER_NOT_FOUND_BY_USERNAME = "User not found by username";
	private Logger LOGGER = LoggerFactory.getLogger(getClass());
	private IUserRepository iUserRepository;
	private BCryptPasswordEncoder passwordEncoder;
	private LoginAttemptService loginAttemptService;
	private EmailService emailService;

	@Autowired
	public UserServiceImpl(IUserRepository iUserRepository, BCryptPasswordEncoder passwordEncoder,LoginAttemptService loginAttemptService,EmailService emailService) {
		super();
		this.iUserRepository = iUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginAttemptService=loginAttemptService;
		this.emailService= emailService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user  = iUserRepository.findUserByUsername(username); 
		if (user == null) {
			LOGGER.error(USER_NOT_FOUND_BY_USERNAME + username);
			throw new UsernameNotFoundException(USER_NOT_FOUND_BY_USERNAME + username);
			
		}else {
			try {
				validateLoginAttempt(user);
			} catch (ExecutionException e) {
				
				e.printStackTrace();
			}
			user.setLastLoginDateDisplay(user.getLastLoginDate());
			user.setLastLoginDate(new Date());
			iUserRepository.save(user);
			UserPrincipal userPrincipal = new UserPrincipal(user);
			LOGGER.info(RETURNING_TROUVER_L_UTILISATEUR_PAR_L_USERNAME + username);
			
			return userPrincipal;
	
		}
		
	}
	private void validateLoginAttempt(User user) throws ExecutionException {
		if (user.isNotLocked() ) {
			if (loginAttemptService.hasExceededMaxAttempts(user.getUsername()))  {
				user.setNotLocked(false);
			} else {
				user.setNotLocked(true);
			}
		}else {
			loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
		}
		
	}
	// Ajoute également un objet utilisateur dans la base de données, réserver au front office elle est destinée 
	// à l'ajout d'un utilisateur lorsqu'un utilisateur créé un compte dans l'application
	@Override
	public User register(String firstName, String lastName, String username, String email) {
		try {
			validatedNewUsernameAndEmail(StringUtils.EMPTY, username,email);
			
			User user = new User();
			
			user.setIdUser(generateUserId());
			String password = generatePassword();
			String encodedPassword = encodePassword(password);
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setUsername(username);
			user.setEmail(email);
			user.setJoinDate(new Date());
			user.setPassword(encodedPassword);
			user.setActive(true);
			user.setNotLocked(true);
			user.setRole(ROLE_USER.name());
			user.setAuthorities(ROLE_USER.getAuthorities());
			user.setProfileImageURL(getTemporaryProfilImageUrl());
			
			iUserRepository.save(user);
			LOGGER.info(NEW_USER_PASSWORD+password);
			emailService.sendNewPasswordEmail(firstName, password, email);
		} catch (UserNotFoundException | UsernameExistException |EmailExistException |MessagingException e) {
			
			e.printStackTrace();
		
		}
		return null;
	}

	private String getTemporaryProfilImageUrl() {
		
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH).toString();
	}

	private String encodePassword(String password) {
		
		return passwordEncoder.encode(password);
	}

	private String generatePassword() {
		
		return RandomStringUtils.randomAlphanumeric(10);
	}

	private String generateUserId() {
		
		return RandomStringUtils.randomNumeric(10);
	}

	// validateNewUsernameAndEmail() est appelé par validateNewUsernameAndEmail() et register()
	// elle vérifie si les valeurs Username et Email appartiennent déjà à un utlisateur 
	private User validatedNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException  {
		
		User userNewByUsername = findUserByUsername(newUsername);
		User userNewByEmail = findUserByEmail(newEmail);
		
		
		if(StringUtils.isNotBlank(currentUsername)) {
			
			User currentUser = findUserByUsername(currentUsername);
			if (currentUser == null) {
				throw new UserNotFoundException(PAS_D_UTILISATEUR_TROUVÉ_AVEC_CET_USERNAME + currentUsername);
				
			}
			
			
			if (userNewByUsername != null && currentUser.getId().equals(userNewByUsername.getId())) {
				throw new UsernameExistException(L_USERNAME_EXISTE_DÉJÀ);
				
			}
			
			
			if (userNewByEmail != null && currentUser.getId().equals(userNewByEmail.getId())) {
				throw new EmailExistException(UN_UTILISATEUR_AVEC_CETTE_ADRESSE_MAIL_EXIST_DÉJÀ);
				
			}
			
			return currentUser; 
		
		}else {
			
			if (userNewByUsername != null ) {
				throw new UsernameExistException(L_USERNAME_EXISTE_DÉJÀ + userNewByUsername );
				
			}
			
			
			if (userNewByEmail != null ) {
				throw new EmailExistException(UN_UTILISATEUR_AVEC_CETTE_ADRESSE_MAIL_EXIST_DÉJÀ + userNewByEmail);
				
			}
			return null;
			
		}
		
		
	}



	@Override
	public List<User> getUsers() {
		return  iUserRepository.findAll();
	}



	@Override
	public User findUserByUsername(String username) {
		return iUserRepository.findUserByUsername(username);
	}



	@Override
	public User findUserByEmail(String email) {
		return iUserRepository.findUserByEmail(email);
	}
	
	

}
