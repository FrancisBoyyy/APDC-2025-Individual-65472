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
import pt.unl.fct.di.apdc.firstwebapp.util.RemoveUserData;

import java.util.logging.Logger;

@Path("/remove_user")
public class RemoveUserResource {

    private static final String MESSAGE_REMOVE_ATTEMPT = "User removal attempt by user: ";
    private static final String MESSAGE_REMOVE_SUCCESSFUL = "User removal successful by user: ";
    private static final String MESSAGE_REMOVE_FAIL = "User removal failed by user: ";
    private static final String MESSAGE_NO_PERMISSION = "User does not have permission.";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUser(RemoveUserData data,
                                 @Context HttpServletRequest request,
                                 @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        String currentUserRole = token.role;
        String otherUser = data.username;

        LOG.fine(MESSAGE_REMOVE_ATTEMPT + currentUsername + " for user " + otherUser + ".");

        try{
            Key otherUserKey = getUserKeyByUsernameOrEmail(otherUser);
            Entity userEntity = datastore.get(otherUserKey);

            if (userEntity == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\":\"User not found.\"}")
                        .build();
            }

            String otherUserRole = userEntity.getString("user_role");
            if (currentUserRole.equals("ADMIN")) {
                // apaga
                datastore.delete(otherUserKey);
                LOG.info(MESSAGE_REMOVE_SUCCESSFUL + currentUsername + " [Admin] removal of " + otherUser + ".");
                return Response.ok("{\"message\":\"Removed successfully\"}").build();

            } else if (currentUserRole.equals("BACKOFFICE") && (otherUserRole.equals("ENDUSER") || otherUserRole.equals("PARTNER"))) {
                //apaga
                datastore.delete(otherUserKey);
                LOG.info(MESSAGE_REMOVE_SUCCESSFUL + currentUsername + " [Backoffice] remove of " + ".");
                return Response.ok("{\"message\":\"Removed successfully\"}").build();

            } else {
                LOG.warning(MESSAGE_REMOVE_FAIL + currentUsername + " [No Permission] for user " + otherUser + ".");
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(MESSAGE_NO_PERMISSION)
                        .build();
            }
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Key getUserKeyByUsernameOrEmail(String usernameOrEmail) {
        // procura na datastore pelo nome
        Key userKey = userKeyFactory.newKey(usernameOrEmail);
        Entity user = datastore.get(userKey);

        if (user != null) {
            // encontrou por nome, devolve chave
            return userKey;
        }

        // se não encontra por nome, procura por mail
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(StructuredQuery.PropertyFilter.eq("email", usernameOrEmail)) // Check email field
                .build();

        QueryResults<Entity> results = datastore.run(query);

        if (results.hasNext()) {
            Entity userEntity = results.next();
            //encontrou o mail, devolve a key
            return userEntity.getKey();
        }

        //não encontra resultado
        return null;
    }
}
