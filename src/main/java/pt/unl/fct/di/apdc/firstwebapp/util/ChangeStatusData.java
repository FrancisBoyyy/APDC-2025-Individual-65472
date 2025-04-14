package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeStatusData {

    public String username;
    public String newStatus;

    public ChangeStatusData() {}

    public ChangeStatusData(String username, String newStatus) {
        this.username = username;
        this.newStatus = newStatus;
    }

    public boolean isValid(){
        return ((newStatus.equals("ATIVADA")) || (newStatus.equals("SUSPENSA")) || (newStatus.equals("DESATIVADA")));
    }
}
