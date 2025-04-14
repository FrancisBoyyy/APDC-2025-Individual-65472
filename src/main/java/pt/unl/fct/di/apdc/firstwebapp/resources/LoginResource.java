package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";
	private static final String MESSAGE_NEXT_PARAMETER_INVALID = "Request parameter 'next' must be greater or equal to 0.";

	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
	private static final String LOG_MESSAGE_UNKNOW_USER = "Failed login attempt for username: ";

	private static final String USER_PWD = "user_pwd";
	private static final String USER_LOGIN_TIME = "user_login_time";

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();

	public LoginResource() {

	}

	@POST
	@Path("/login_user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data,
							  @Context HttpServletRequest request,
							  @Context HttpHeaders headers) {
		LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.username);


		// Check if the input matches a username or email directly
		Key userKey = null;
		userKey = getUserKeyByUsernameOrEmail(data.username);


		// If userKey is null or doesn't exist, return error
		if (userKey == null) {
			LOG.warning(LOG_MESSAGE_LOGIN_ATTEMP + data.username);
			return Response.status(Status.FORBIDDEN)
					.entity(MESSAGE_INVALID_CREDENTIALS)
					.build();
		}


		Key ctrsKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.username))
				.setKind("UserStats")
				.newKey("counters");


		// Generate automatically a key for logs
		Key logKey = datastore.allocateId(
				datastore.newKeyFactory()
						.addAncestors(PathElement.of("User", data.username))
						.setKind("UserLog").newKey());


		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if (user == null) {
				// Username/email does not exist
				LOG.warning(LOG_MESSAGE_LOGIN_ATTEMP + data.username);
				return Response.status(Status.FORBIDDEN)
						.entity(MESSAGE_INVALID_CREDENTIALS)
						.build();
			}

			// We get the user stats from the storage
			Entity stats = txn.get(ctrsKey);
			if (stats == null) {
				stats = Entity.newBuilder(ctrsKey)
						.set("user_stats_logins", 0L)
						.set("user_stats_failed", 0L)
						.set("user_first_login", Timestamp.now())
						.set("user_last_login", Timestamp.now())
						.build();
			}
			String hashedPWD = (String) user.getString(USER_PWD);
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				// Login successful, construct log and update stats
				String cityLatLong = headers.getHeaderString("X-AppEngine-CityLatLong");
				Entity log = null;
				if(cityLatLong != null) {
					log = Entity.newBuilder(logKey)
							.set("user_login_ip", request.getRemoteAddr())
							.set("user_login_host", request.getRemoteHost())
							.set("user_login_latlon", cityLatLong != null
									? StringValue.newBuilder(cityLatLong).setExcludeFromIndexes(true).build()
									: StringValue.newBuilder("").setExcludeFromIndexes(true).build())
							.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
							.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
							.set("user_login_time", Timestamp.now())
							.build();
				} else{
					log = Entity.newBuilder(logKey)
							.set("user_login_ip", request.getRemoteAddr())
							.set("user_login_host", request.getRemoteHost())
							.set("user_login_time", Timestamp.now())
							.build();
				}

				// Get the user statistics and update it
				Entity ustats = Entity.newBuilder(ctrsKey)
						.set("user_stats_logins", stats.getLong("user_stats_logins") + 1)
						.set("user_stats_failed", 0L)
						.set("user_first_login", stats.getTimestamp("user_first_login"))
						.set("user_last_login", Timestamp.now())
						.build();

				// Batch operation
				txn.put(log, ustats);
				txn.commit();
				// Return token
				AuthToken token = new AuthToken(data.username, user.getString("user_role"));
				LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.username);
				return Response.ok(g.toJson(token)).build();
			} else {
				// Incorrect password, update failed stats
				Entity ustats = Entity.newBuilder(ctrsKey)
						.set("user_stats_logins", stats.getLong("user_stats_logins"))
						.set("user_stats_failed", stats.getLong("user_stats_failed") + 1L)
						.set("user_first_login", stats.getTimestamp("user_first_login"))
						.set("user_last_login", stats.getTimestamp("user_last_login"))
						.set("user_last_attempt", Timestamp.now())
						.build();

				txn.put(ustats);
				txn.commit();
				LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.username);
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
			}
		} catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
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

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLatestLogins(LoginData data) {

		Key userKey = userKeyFactory.newKey(data.username);

		Entity user = datastore.get(userKey);
		if (user != null && user.getString(USER_PWD).equals(DigestUtils.sha512Hex(data.password))) {

			// Get the date of yesterday
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			Timestamp yesterday = Timestamp.of(cal.getTime());

			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind("UserLog")
					.setFilter(
							CompositeFilter.and(
									PropertyFilter.hasAncestor(
											datastore.newKeyFactory().setKind("User").newKey(data.username)),
									PropertyFilter.ge(USER_LOGIN_TIME, yesterday)))
					.setOrderBy(OrderBy.desc(USER_LOGIN_TIME))
					.setLimit(3)
					.build();
			QueryResults<Entity> logs = datastore.run(query);

			List<Date> loginDates = new ArrayList<Date>();
			logs.forEachRemaining(userlog -> {
				loginDates.add(userlog.getTimestamp(USER_LOGIN_TIME).toDate());
			});

			return Response.ok(g.toJson(loginDates)).build();
		}
		return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS)
				.build();
	}

	@POST
	@Path("/user/pagination")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLatestLogins(@QueryParam("next") String nextParam, LoginData data) {

		int next;

		// Checking for valid request parameter values
		try {
			next = Integer.parseInt(nextParam);
			if (next < 0)
				return Response.status(Status.BAD_REQUEST).entity(MESSAGE_NEXT_PARAMETER_INVALID).build();
		} catch (NumberFormatException e) {
			return Response.status(Status.BAD_REQUEST).entity(MESSAGE_NEXT_PARAMETER_INVALID).build();
		}

		Key userKey = userKeyFactory.newKey(data.username);

		Entity user = datastore.get(userKey);
		if (user != null && user.getString(USER_PWD).equals(DigestUtils.sha512Hex(data.password))) {

			// Get the date of yesterday
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			Timestamp yesterday = Timestamp.of(cal.getTime());

			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind("UserLog")
					.setFilter(
							CompositeFilter.and(
									PropertyFilter.hasAncestor(
											datastore.newKeyFactory().setKind("User").newKey(data.username)),
									PropertyFilter.ge(USER_LOGIN_TIME, yesterday)))
					.setOrderBy(OrderBy.desc(USER_LOGIN_TIME))
					.setLimit(3)
					.setOffset(next)
					.build();
			QueryResults<Entity> logs = datastore.run(query);

			List<Date> loginDates = new ArrayList<Date>();
			logs.forEachRemaining(userlog -> {
				loginDates.add(userlog.getTimestamp(USER_LOGIN_TIME).toDate());
			});

			return Response.ok(g.toJson(loginDates)).build();
		}
		return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS)
				.build();
	}

}
