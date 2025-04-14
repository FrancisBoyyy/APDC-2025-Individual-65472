package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2;
	
	public String username;
	public String role;
	public String tokenID;
	public long VALID_FROM;
	public long VALID_TO;

	public AuthToken() {}
	
	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.VALID_FROM = System.currentTimeMillis();
		this.VALID_TO = VALID_FROM + EXPIRATION_TIME;
	}
}
