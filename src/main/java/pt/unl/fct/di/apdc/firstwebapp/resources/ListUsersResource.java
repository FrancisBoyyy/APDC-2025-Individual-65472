package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
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
import pt.unl.fct.di.apdc.firstwebapp.util.ListUserData;
import pt.unl.fct.di.apdc.firstwebapp.util.RemoveUserData;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/list_users")
public class ListUsersResource {
    private static final String MESSAGE_LIST_ATTEMPT = "User listing attempt by user: ";
    private static final String MESSAGE_LIST_SUCCESSFUL = "User listing successful by user: ";
    private static final String MESSAGE_LIST_FAIL = "User listing failed by user: ";
    private static final String MESSAGE_NO_PERMISSION = "User does not have permission.";

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@Context HttpServletRequest request,
                              @Context HttpHeaders headers) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String currentUsername = token.username;
        String currentUserRole = token.role;

        LOG.fine(MESSAGE_LIST_ATTEMPT + currentUsername + ".");

        try {

            if(currentUserRole.equals("PARTNER")){
                LOG.warning(MESSAGE_LIST_FAIL + currentUsername + " [No Permission].");
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(MESSAGE_NO_PERMISSION)
                        .build();
            }

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();
            QueryResults<Entity> results = datastore.run(query);
            List<ListUserData> userArray = new ArrayList<ListUserData>();

            while (results.hasNext()) {
                Entity user = results.next();
                String role = user.getString("user_role");

                if (currentUserRole.equals("ADMIN")) {
                    // List everything for all users
                    ListUserData newUser = new ListUserData(
                            user.getString("user_name"),
                            user.getString("user_phone"),
                            user.getString("user_email"),
                            user.getString("user_username"),
                            user.getString("user_privacy"),
                            user.getString("user_status"),
                            role,
                            (user.contains("user_cc") ? user.getString("user_cc") : ""),
                            (user.contains("user_nif") ? user.getString("user_nif") : ""),
                            (user.contains("user_enterprise") ? user.getString("user_enterprise") : ""),
                            (user.contains("user_function") ? user.getString("user_function") : ""),
                            (user.contains("user_address") ? user.getString("user_address") : ""),
                            (user.contains("user_enterprise_nif") ? user.getString("user_enterprise_nif") : ""));
                    userArray.add(newUser);
                } else if (currentUserRole.equals("BACKOFFICE") && role.equals("ENDUSER")) {
                    // List everything for ENDUSERs only
                    ListUserData newUser = new ListUserData(
                            user.getString("user_name"),
                            user.getString("user_phone"),
                            user.getString("user_email"),
                            user.getString("user_username"),
                            user.getString("user_privacy"),
                            user.getString("user_status"),
                            role,
                            (user.contains("user_cc") ? user.getString("user_cc") : ""),
                            (user.contains("user_nif") ? user.getString("user_nif") : ""),
                            (user.contains("user_enterprise") ? user.getString("user_enterprise") : ""),
                            (user.contains("user_function") ? user.getString("user_function") : ""),
                            (user.contains("user_address") ? user.getString("user_address") : ""),
                            (user.contains("user_enterprise_nif") ? user.getString("user_enterprise_nif") : ""));
                    userArray.add(newUser);
                } else if (currentUserRole.equals("ENDUSER") &&
                        role.equals("ENDUSER") &&
                        "público".equalsIgnoreCase(user.getString("user_privacy")) &&
                        "ACTIVADA".equalsIgnoreCase(user.getString("user_status"))) {

                    // Only name, username, and email for public + active endusers
                    ListUserData newUser = new ListUserData(
                            user.getString("user_name"),
                            user.getString("user_username"),
                            user.getString("user_email"));
                }
            }
            LOG.info(MESSAGE_LIST_SUCCESSFUL + currentUsername + " [" + currentUserRole + "].");
            return Response.ok(g.toJson(userArray)).build();

        } catch (Exception e) {
            LOG.severe("Error listing users: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"An error occurred while listing users.\"}")
                    .build();
        }
    }
}
