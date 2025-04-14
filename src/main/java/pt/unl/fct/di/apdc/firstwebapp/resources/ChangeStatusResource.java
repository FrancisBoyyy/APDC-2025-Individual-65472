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
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeStatusData;

import java.util.logging.Logger;


@Path("/change_status")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeStatusResource {

    private static final String MESSAGE_STATUS_CHANGE_ATTEMPT = "User status change attempt by user: ";
    private static final String MESSAGE_STATUS_CHANGE_SUCCESSFUL = "User status change successful by user: ";
    private static final String MESSAGE_STATUS_CHANGE_FAIL = "User status change failed by user: ";
    private static final String MESSAGE_NO_PERMISSION = "User does not have permission.";
    private static final String MESSAGE_WRONG_PARAMETERS = "Wrong parameters.";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @POST
    @Path("/change")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeStatus(ChangeStatusData data,
                              @Context HttpServletRequest request,
                              @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        String currentUserRole = token.role;
        String otherUser = data.username;
        String otherUserStatus = data.newStatus;

        LOG.fine(MESSAGE_STATUS_CHANGE_ATTEMPT + currentUsername + " for user " + otherUser + " to " + otherUserStatus + ".");

        if(!data.isValid()){
            LOG.warning(MESSAGE_WRONG_PARAMETERS);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Wrong parameters.")
                    .build();
        }

        try {
            Entity targetUser = getUserByID(otherUser);

            if (targetUser == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Target user not found.")
                        .build();
            }

            if (currentUserRole.equals("ADMIN")) {
                // change no matter what

                Entity updatedUser = Entity.newBuilder(targetUser)
                        .set("user_status", otherUserStatus)
                        .build();

                datastore.put(updatedUser);

                LOG.info(MESSAGE_STATUS_CHANGE_SUCCESSFUL + currentUsername + " [Admin] changed status of " + otherUser + " to " + otherUserStatus + ".");
                return Response.ok("{\"message\":\"Status updated successfully\"}").build();

            } else if (currentUserRole.equals("BACKOFFICE")) {
                // change

                String targetCurrentStatus = targetUser.getString("user_status");

                if ((targetCurrentStatus.equals("ATIVADA") && otherUserStatus.equals("DESATIVADA")) ||
                        (targetCurrentStatus.equals("DESATIVADA") && otherUserStatus.equals("ATIVADA"))) {

                    Entity updatedUser = Entity.newBuilder(targetUser)
                            .set("user_status", otherUserStatus)
                            .build();

                    datastore.put(updatedUser);

                    LOG.info(MESSAGE_STATUS_CHANGE_SUCCESSFUL + currentUsername + " [Backoffice] changed role of " + otherUser + " to " + otherUserStatus + ".");
                    return Response.ok("{\"message\":\"Role updated successfully\"}").build();
                }
            } else {
                LOG.warning(MESSAGE_STATUS_CHANGE_FAIL + currentUsername + " [No Permission] for user " + otherUser + " to " + otherUserStatus + ".");
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
