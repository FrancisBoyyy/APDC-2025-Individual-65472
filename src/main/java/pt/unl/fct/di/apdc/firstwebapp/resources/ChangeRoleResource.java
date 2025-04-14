package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
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
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/change_role")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeRoleResource {

    private static final String MESSAGE_ROLE_CHANGE_ATTEMPT = "User role change attempt by user: ";
    private static final String MESSAGE_ROLE_CHANGE_SUCCESSFUL = "User role change successful by user: ";
    private static final String MESSAGE_ROLE_CHANGE_FAIL = "User role change failed by user: ";
    private static final String MESSAGE_NO_PERMISSION = "User does not have permission.";
    private static final String MESSAGE_WRONG_PARAMETERS = "Wrong parameters.";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @POST
    @Path("/change")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data,
                              @Context HttpServletRequest request,
                              @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        String currentUserRole = token.role;
        String otherUser = data.username;
        String otherUserRole = data.newRole;

        LOG.fine(MESSAGE_ROLE_CHANGE_ATTEMPT + currentUsername + " for user " + otherUser + " to " + otherUserRole + ".");

        if(!data.isValid()){
            LOG.warning(MESSAGE_WRONG_PARAMETERS);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Wrong parameters.")
                    .build();
        }

        try {
            if (currentUserRole.equals("ADMIN")) {
                // change no matter what
                Entity targetUser = getUserByID(otherUser);

                if (targetUser == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Target user not found.")
                            .build();
                }

                Entity updatedUser = Entity.newBuilder(targetUser)
                        .set("user_role", otherUserRole)
                        .build();

                datastore.put(updatedUser);

                LOG.info(MESSAGE_ROLE_CHANGE_SUCCESSFUL + currentUsername + " [Admin] changed role of " + otherUser + " to " + otherUserRole + ".");
                return Response.ok("{\"message\":\"Role updated successfully\"}").build();

            } else if (currentUserRole.equals("BACKOFFICE") && (otherUserRole.equals("ENDUSER") || otherUserRole.equals("PARTNER"))) {
                // change
                Key targetUserKey = userKeyFactory.newKey(otherUser);
                Entity targetUser = datastore.get(targetUserKey);

                if (targetUser == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Target user not found")
                            .build();
                }

                String targetCurrentRole = targetUser.getString("user_role");

                if ((targetCurrentRole.equals("PARTNER") && otherUserRole.equals("ENDUSER")) ||
                        (targetCurrentRole.equals("ENDUSER") && otherUserRole.equals("PARTNER"))) {

                    Entity updatedUser = Entity.newBuilder(targetUser)
                            .set("user_role", otherUserRole)
                            .build();

                    datastore.put(updatedUser);

                    LOG.info(MESSAGE_ROLE_CHANGE_SUCCESSFUL + currentUsername + " [Backoffice] changed role of " + otherUser + " to " + otherUserRole + ".");
                    return Response.ok("{\"message\":\"Role updated successfully\"}").build();
                }
            } else {
                LOG.warning(MESSAGE_ROLE_CHANGE_FAIL + currentUsername + " [No Permission] for user " + otherUser + " to " + otherUserRole + ".");
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(MESSAGE_NO_PERMISSION)
                        .build();
            }
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Unhandled role change case.")
                .build();
    }

    private Entity getUserByID(String userID){
        Key targetUserKey = userKeyFactory.newKey(userID);
        return datastore.get(targetUserKey);
    }
}
