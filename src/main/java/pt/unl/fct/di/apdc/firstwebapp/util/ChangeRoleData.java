package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeRoleData {
    public String username;
    public String newRole;

    public ChangeRoleData() {}

    public ChangeRoleData(String username, String newRole) {
        this.username = username;
        this.newRole = newRole;
    }

    public boolean isValid(){
        return ((newRole.equals("ADMIN")) || (newRole.equals("BACKOFFICE")) || (newRole.equals("ENDUSER")) || (newRole.equals("PARTNER")));
    }
}
