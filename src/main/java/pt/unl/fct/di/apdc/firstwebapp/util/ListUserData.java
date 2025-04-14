package pt.unl.fct.di.apdc.firstwebapp.util;

import com.google.cloud.datastore.Entity;

public class ListUserData {
    public String name;
    public String phone;
    public String email;
    public String username;
    public String privacy;

    public String CC;
    public String ROLE;
    public String NIF;
    public String enterprise;
    public String function;
    public String address;
    public String enterpriseNIF;
    public String accountStatus;

    public ListUserData(String name,
                        String phone,
                        String email,
                        String username,
                        String privacy,
                        String status,
                        String role,
                        String CC,
                        String NIF,
                        String enterprise,
                        String function,
                        String address,
                        String enterpriseNIF){
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.username = username;
        this.privacy = privacy;
        this.accountStatus = status;
        this.ROLE = role;
        this.CC = CC;
        this.NIF = NIF;
        this.enterprise = enterprise;
        this.function = function;
        this.address = address;
        this.enterpriseNIF = enterpriseNIF;
    }

    public ListUserData(String name,
                        String username,
                        String email){
        this.name = name;
        this.username = username;
        this.email = email;
    }

    /**
    public ListUserData(Entity user) {
        this.username = user.getKey().getName();
        this.email = get(user, "user_email");
        this.name = get(user, "user_name");
        this.phone = get(user, "user_phone");
        this.privacy = get(user, "user_privacy");
        this.ROLE = get(user, "user_role");
        this.accountStatus = get(user, "user_status");
        this.NIF = get(user, "user_nif");
        this.enterprise = get(user, "user_enterprise");
        this.enterpriseNIF = get(user, "user_enterprise_nif");
        this.function = get(user, "user_function");
    }

    private String get(Entity user, String field) {
        return user.contains(field) ? user.getString(field) : "NOT DEFINED";
    }
     */
}
