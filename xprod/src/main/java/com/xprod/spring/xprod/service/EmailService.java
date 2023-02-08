package com.xprod.spring.xprod.service;



import static com.xprod.spring.xprod.constant.EmailConstant.CC_EMAIL;
import static com.xprod.spring.xprod.constant.EmailConstant.DEFAULT_PORT;
import static com.xprod.spring.xprod.constant.EmailConstant.EMAIL_SUBJECT;
import static com.xprod.spring.xprod.constant.EmailConstant.FROM_EMAIL;
import static com.xprod.spring.xprod.constant.EmailConstant.GMAIL_SMTP_SERVER;
import static com.xprod.spring.xprod.constant.EmailConstant.SMTP_AUTH;
import static com.xprod.spring.xprod.constant.EmailConstant.SMTP_HOST;
import static com.xprod.spring.xprod.constant.EmailConstant.SMTP_PORT;
import static com.xprod.spring.xprod.constant.EmailConstant.SMTP_STARTTLS_ENABLE;
import static com.xprod.spring.xprod.constant.EmailConstant.*;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

import com.sun.mail.smtp.SMTPTransport;


@Service
public class EmailService {
	
	private Message createEmail(String firstname, String password, String email)
 throws MessagingException {
		Message message = new MimeMessage(getEmailSession());
		message.setFrom(new InternetAddress(FROM_EMAIL));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email,false));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(CC_EMAIL,false));
		message.setSubject(EMAIL_SUBJECT);
		message.setText("Welcome "+firstname+ ", \n\n Your new account password is : "+password);
		message.setSentDate(new Date());
		message.saveChanges();
		return message;
		
	}
	private Session getEmailSession() {
		Properties properties = System.getProperties();
		properties.put(SMTP_HOST, GMAIL_SMTP_SERVER);
		properties.put(SMTP_AUTH, true);
		properties.put(SMTP_PORT, DEFAULT_PORT);
		properties.put(SMTP_STARTTLS_ENABLE, true);
		properties.put(SMTP_STARTTLS_REQUIRED, true);
	
		return Session.getInstance(properties,null);
		
	}
	public void sendNewPasswordEmail(String firstname, String password, String email)	throws MessagingException{
		
		Message message = createEmail (firstname,password,email);
		SMTPTransport smtpTransport =(SMTPTransport) getEmailSession().getTransport(SIMPLE_MAIL_TRANSFERT_PROTOCOLE);
		smtpTransport.connect(GMAIL_SMTP_SERVER,USERNAME,PASSWORD);
		smtpTransport.sendMessage(message, message.getAllRecipients());
		smtpTransport.close();
	}
	
	
}
