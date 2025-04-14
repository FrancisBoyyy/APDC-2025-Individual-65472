package pt.unl.fct.di.apdc.firstwebapp.util;

public class PasswordChangeData {
    public String oldPassword;
    public String newPassword;
    public String confirmation;

    public PasswordChangeData() {}

    public PasswordChangeData(String oldPassword, String newPassword, String confirmation) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }
}
