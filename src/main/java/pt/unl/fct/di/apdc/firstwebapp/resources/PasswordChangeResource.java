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
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.EditAccountData;
import pt.unl.fct.di.apdc.firstwebapp.util.PasswordChangeData;

import java.util.logging.Logger;

@Path("/change_password")
public class PasswordChangeResource {

    private static final String MESSAGE_CHANGE_PASSWORD_ATTEMPT = "User password change attempt by user: ";
    private static final String MESSAGE_CHANGE_PASSWORD_SUCCESSFUL = "User password change successful by user: ";
    private static final String MESSAGE_CHANGE_PASSWORD_FAIL = "User password change failed by user: ";
    private static final String MESSAGE_PASSWORD_MISMATCH_FAIL = "User password change failed by password mismatch by user: ";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @POST
    @Path("/change")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(PasswordChangeData data,
                                @Context HttpServletRequest request,
                                @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        Entity currentUser = getUserByID(currentUsername);
        LOG.fine(MESSAGE_CHANGE_PASSWORD_ATTEMPT + currentUsername + ".");

        try{
            String hashedPWD = (String) currentUser.getString("user_pwd");
            String newHashedPassword = DigestUtils.sha512Hex(data.newPassword);
            String oldHashedPassword = DigestUtils.sha512Hex(data.oldPassword);
            if (hashedPWD.equals(oldHashedPassword)) {
                if(data.newPassword.equals(data.confirmation)){
                    Entity updatedUser = Entity.newBuilder(currentUser)
                            .set("user_pwd", newHashedPassword)
                            .build();
                    datastore.put(updatedUser);
                    LOG.fine(MESSAGE_CHANGE_PASSWORD_SUCCESSFUL + currentUsername + ".");
                    return Response.status(Response.Status.OK).build();
                }
                LOG.warning(MESSAGE_PASSWORD_MISMATCH_FAIL + currentUsername + ".");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            LOG.warning(MESSAGE_CHANGE_PASSWORD_FAIL + currentUsername + ".");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(Exception e) {
            LOG.severe(MESSAGE_CHANGE_PASSWORD_FAIL + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private Entity getUserByID(String userID){
        Key targetUserKey = userKeyFactory.newKey(userID);
        return datastore.get(targetUserKey);
    }
}
