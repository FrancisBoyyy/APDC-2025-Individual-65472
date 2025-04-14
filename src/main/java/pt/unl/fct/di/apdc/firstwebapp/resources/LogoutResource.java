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

import java.util.logging.Logger;


@Path("/logout_user")
public class LogoutResource {

    private static final String MESSAGE_LOGOUT_ATTEMPT = "User logout attempt by user: ";
    private static final String MESSAGE_LOGOUT_SUCCESSFUL = "User logout successful by user: ";
    private static final String MESSAGE_LOGOUT_FAIL = "User logout failed by user: ";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response logOut(@Context HttpServletRequest request,
                           @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        LOG.fine(MESSAGE_LOGOUT_ATTEMPT + currentUsername + ".");
        try {
            Key revokedTokenKey = datastore.newKeyFactory()
                    .setKind("RevokedToken")
                    .newKey(token.tokenID);
            Entity tokenEntity = Entity.newBuilder(revokedTokenKey)
                    .set("token_id", token.tokenID)
                    .set("token_username", token.username)
                    .set("token_user_role", token.role)
                    .set("token_valid_from", token.VALID_FROM)
                    .set("token_valid_to", token.VALID_TO) // stored as long
                    .set("token_revoked_at", System.currentTimeMillis())
                    .build();
            datastore.put(tokenEntity);
            LOG.info(MESSAGE_LOGOUT_SUCCESSFUL + currentUsername + ".");
            return Response.status(Response.Status.OK).build();
        } catch(Exception e){
            LOG.warning(MESSAGE_LOGOUT_FAIL + currentUsername + ".");
            LOG.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
