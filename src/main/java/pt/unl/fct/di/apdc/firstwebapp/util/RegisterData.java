package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

	private String[] domains = {"com", "org", "net", "int", "edu", "gov", "mil"};
	private String[] publicOrPrivate = {"público", "privado"};
	private String[] roles = {"ADMIN", "BACKOFFICE", "ENDUSER", "PARTNER"};
	private String[] statuses = {"ATIVADA", "SUSPENSA", "DESATIVADA"};

	// mandatory
	public String username;
	public String phone;
	public String password;
	public String confirmation;
	public String email;
	public String name;
	public String privacy;

	// optional
	public String CC;
	public String ROLE = "ENDUSER";
	public String NIF;
	public String enterprise;
	public String function;
	public String address;
	public String enterpriseNIF;
	public String accountStatus = "DESATIVADA";

	public RegisterData() {}
	
	public RegisterData(String username, String phone, String password, String confirmation, String email, String name, String privacy, String CC, String NIF, String enterprise, String function, String address, String enterpriseNIF) {
		this.username = username;
		this.phone = phone;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.privacy = privacy;

		this.CC = CC;
		this.NIF = NIF;
		this.enterprise = enterprise;
		this.function = function;
		this.address = address;
		this.enterpriseNIF = enterpriseNIF;
	}
	
	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}
	
	public boolean validRegistration() {

		boolean emailConfirmation =
				nonEmptyOrBlankField(email) &&
				email.contains("@");
		boolean domainConfirmation = false;
		for(String r : domains)
			domainConfirmation = domainConfirmation || email.endsWith(r);
		emailConfirmation = emailConfirmation && domainConfirmation;

		boolean privacyConfirmation = nonEmptyOrBlankField(privacy) && (privacy.equals("público")||privacy.equals("privado"));
		 	
		return nonEmptyOrBlankField(username) &&
				nonEmptyOrBlankField(phone) &&
			   nonEmptyOrBlankField(password) &&
			   nonEmptyOrBlankField(name) &&
			   emailConfirmation &&
			   password.equals(confirmation) &&
				privacyConfirmation;
	}
}
