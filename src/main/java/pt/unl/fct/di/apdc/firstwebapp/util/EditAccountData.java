package pt.unl.fct.di.apdc.firstwebapp.util;

public class EditAccountData {

    private String[] domains = {"com", "org", "net", "int", "edu", "gov", "mil"};
    private String[] publicOrPrivate = {"público", "privado"};
    private String[] roles = {"ADMIN", "BACKOFFICE", "ENDUSER", "PARTNER"};
    private String[] statuses = {"ATIVADA", "SUSPENSA", "DESATIVADA"};

    // mandatory
    public String targetUsername;
    public String phone;
    public String name;
    public String privacy;

    // optional
    public String CC;
    public String ROLE;
    public String NIF;
    public String enterprise;
    public String function;
    public String address;
    public String enterpriseNIF;
    public String accountStatus;

    public EditAccountData() {}

    public EditAccountData(String targetUsername,
                           String phone,
                           String name,
                           String privacy,
                           String CC,
                           String ROLE,
                           String NIF,
                           String enterprise,
                           String function,
                           String address,
                           String enterpriseNIF,
                           String accountStatus) {
        this.targetUsername = targetUsername;
        this.phone = phone;
        this.name = name;
        this.privacy = privacy;

        this.CC = CC;
        this.ROLE = ROLE;
        this.NIF = NIF;
        this.enterprise = enterprise;
        this.function = function;
        this.address = address;
        this.enterpriseNIF = enterpriseNIF;
        this.accountStatus = accountStatus;
    }

    public boolean validEdit(){
        boolean privacyConfirmation = privacy==null || privacy.equals("público") || privacy.equals("privado");
        boolean roleConfirmation = ROLE==null || ROLE.equals("ADMIN") || ROLE.equals("BACKOFFICE") || ROLE.equals("ENDUSER") || ROLE.equals("PARTNER");
        boolean statusConfirmation = accountStatus==null || accountStatus.equals("ATIVADA") || accountStatus.equals("SUSPENSA") || accountStatus.equals("DESATIVADA");
        return privacyConfirmation && roleConfirmation && statusConfirmation;
    }
}
