/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Mail;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.Globals;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.DVD.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@out(name = "rs", type = Boolean.class, desc = "")
	public static Boolean fnSendEmail(String to, String from, Boolean authenticate, String username, String password, String host, Integer port, Boolean ssl, Integer ssl_port, String subject, String email_body, String attachment) throws Exception {
		// The code below is not part of fabric product and is delivered specifically 
		//for the project and for the customer's use
		/*
		if(to == null || to.trim().equals("")){
			throw new RuntimeException("fnSendEmail - To is mandatory field!, please check!");
		}
		
		if(from == null || from.trim().equals("")){
			throw new RuntimeException("fnSendEmail - From is mandatory field!, please check!");
		}
		
		if(authenticate && (username == null || username.trim().equals(""))){
			throw new RuntimeException("fnSendEmail - User name is mandatory field!, please check!");
		}
		
		if(authenticate && (password == null || password.trim().equals(""))){
			throw new RuntimeException("fnSendEmail - Password is mandatory field!, please check!");
		}
		
		if(host == null || host.trim().equals("")){
			throw new RuntimeException("fnSendEmail - Host is mandatory field!, please check!");
		}
		
		if(port == null || port == 0){
			throw new RuntimeException("fnSendEmail - Port is mandatory field!, please check!");
		}
		
		if(ssl && (ssl_port == null || ssl_port == 0)){
			throw new RuntimeException("fnSendEmail - Port is mandatory field!, please check!");
		}
		
		Properties props = System.getProperties();
		
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		if(!ssl){
		    props.put("mail.smtp.starttls.enable", "true");
		}else{
		    props.put("mail.smtp.socketFactory.port", ssl_port);
		    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		}
		
		Session session = null;
		if(authenticate){
			session = Session.getInstance(props, new Authenticator() {
			    @Override
			    protected PasswordAuthentication getPasswordAuthentication() {
			        return new PasswordAuthentication(username, password);
			    }
			});
		}else{
			session = Session.getInstance(props, null);
		}
		
		try {
		    Message message = new MimeMessage(session);
		    message.setFrom(new InternetAddress(from));
			String[] toArr = to.split(",");
		    InternetAddress[] sendTo = new InternetAddress[toArr.length];
		    for (int i = 0; i <toArr.length; i++) {
		        try {
		            sendTo[i] = new InternetAddress(toArr[i]);
		        } catch (AddressException e) {
		            log.error("fnSendEmail - Failed to parse Email address:" + toArr[i] + ", Ignoring address!", e);
		        }
		    }
		    message.setRecipients(Message.RecipientType.TO, sendTo);
			message.setSubject(subject);
		    BodyPart messageBodyPart = new MimeBodyPart();
		    messageBodyPart.setText(email_body + "\n");
		
		    Multipart multipart = new MimeMultipart();
		    multipart.addBodyPart(messageBodyPart);
		
			if(attachment != null){
			    messageBodyPart = new MimeBodyPart();
			    String filename = attachment;
			    DataSource source = new FileDataSource(filename);
			    messageBodyPart.setDataHandler(new DataHandler(source));
			    messageBodyPart.setFileName(filename);
			    multipart.addBodyPart(messageBodyPart);
			}
		    message.setContent(multipart);
		
		    Transport.send(message);
		} catch (MessagingException e) {
			log.error("fnSendEmail - Failed to send Email!");
		    throw new RuntimeException(e);
		}
		*/
		return true;
	}

	
	
	
	
}
