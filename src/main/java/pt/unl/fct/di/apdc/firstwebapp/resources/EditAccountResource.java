package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeStatusData;
import pt.unl.fct.di.apdc.firstwebapp.util.EditAccountData;

import javax.mail.Message;
import java.util.logging.Logger;

@Path("/edit_account")
public class EditAccountResource {
    private static final String MESSAGE_EDIT_ATTEMPT = "User information edit attempt by user: ";
    private static final String MESSAGE_EDIT_SUCCESSFUL = "User information edit successful by user: ";
    private static final String MESSAGE_EDIT_FAIL = "User information edit failed by user: ";
    private static final String MESSAGE_NO_PERMISSION = "User does not have permission.";
    private static final String MESSAGE_USER_NOT_FOUND = "User not found: ";
    private static final String MESSAGE_WRONG_PARAMETERS = "Wrong parameters.";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @POST
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editAccount(EditAccountData data,
                                 @Context HttpServletRequest request,
                                 @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        String currentUserRole = token.role;

        LOG.fine(MESSAGE_EDIT_ATTEMPT + currentUsername);

        if (!data.validEdit()) {
            LOG.warning(MESSAGE_EDIT_FAIL + currentUsername + " [Wrong parameters]");
            return Response.status(Response.Status.BAD_REQUEST).entity("Wrong parameters.").build();
        }

        try{
            Entity currentUser = getUserByID(currentUsername);
            String currentUserStatus = currentUser.getString("user_status");

            Entity targetUser = getUserByID(data.targetUsername);

            if (targetUser == null) {
                LOG.warning(MESSAGE_USER_NOT_FOUND + data.targetUsername);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Target user not found.")
                        .build();
            }

            String targetUserRole = targetUser.getString("user_role");

            if(currentUserRole.equals("PARTNER")){
                //erro direto
                LOG.warning(MESSAGE_NO_PERMISSION);
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("User can't edit accounts.")
                        .build();

            } else if(currentUserRole.equals("ENDUSER")){
                //tem restrições
                if((currentUserStatus!="ATIVADA") || (currentUsername!=data.targetUsername) || (data.accountStatus!=null) || (data.ROLE!=null)){
                    LOG.warning(MESSAGE_EDIT_FAIL + currentUsername + " [Enduser]");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(MESSAGE_EDIT_FAIL + currentUsername + " [Enduser]")
                            .build();
                }

            } else if(currentUserRole.equals("BACKOFFICE")){
                //tem poucas restrições
                if((currentUserStatus!="ATIVADA") || ((targetUserRole!="ENDUSER") && (targetUserRole!="PARTNER"))){
                    // erro
                    LOG.warning(MESSAGE_EDIT_FAIL + currentUsername + " [Backoffice].");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(MESSAGE_EDIT_FAIL + currentUsername + " [Backoffice].")
                            .build();
                }
            }

            Entity.Builder updatedUserBuilder = Entity.newBuilder(targetUser);
            if (data.phone != null) {
                updatedUserBuilder.set("user_phone", data.phone);
            }
            if (data.name != null) {
                updatedUserBuilder.set("user_name", data.name);
            }
            if (data.privacy != null) {
                updatedUserBuilder.set("user_privacy", data.privacy);
            }
            if (data.CC != null) {
                updatedUserBuilder.set("user_cc", data.CC);
            }
            if (data.ROLE != null) {
                updatedUserBuilder.set("user_role", data.ROLE);
            }
            if (data.NIF != null) {
                updatedUserBuilder.set("user_nif", data.NIF);
            }
            if (data.enterprise != null) {
                updatedUserBuilder.set("user_enterprise", data.enterprise);
            }
            if (data.function != null) {
                updatedUserBuilder.set("user_function", data.function);
            }
            if (data.address != null) {
                updatedUserBuilder.set("user_address", data.address);
            }
            if (data.enterpriseNIF != null) {
                updatedUserBuilder.set("user_enterprise_nif", data.enterpriseNIF);
            }
            if (data.accountStatus != null) {
                updatedUserBuilder.set("user_status", data.accountStatus);
            }

            datastore.put(updatedUserBuilder.build());
            LOG.info(MESSAGE_EDIT_SUCCESSFUL + currentUsername + " [" + currentUserRole + "] " +  " updated user " + data.targetUsername);
            return Response.ok("{\"message\":\"User updated successfully.\"}").build();

        } catch(Exception e) {
            LOG.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Entity getUserByID(String userID){
        Key targetUserKey = userKeyFactory.newKey(userID);
        return datastore.get(targetUserKey);
    }
}
