package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();


	public RegisterResource() {}	// Default constructor, nothing to do

	// V3
	@POST
	@Path("/new_user")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data) {
		//registo de tentativa
		LOG.fine("Attempt to register user: " + data.username);

		//verifica informação do registo
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		//inicia nova transação
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			// If the entity does not exist null is returned...
			if (user != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT).entity("User already exists.").build();
			} else {
				 // ... otherwise
				Entity.Builder build = Entity.newBuilder(userKey)
						.set("user_name", data.name)
						.set("user_phone", data.phone)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_creation_time", "" + System.currentTimeMillis())
						.set("user_username", data.username)
						.set("user_privacy", data.privacy)
						.set("user_status", data.accountStatus)
						.set("user_role", data.ROLE);
				if(data.CC != null)
					build.set("user_cc", data.CC);
				if(data.NIF != null)
					build.set("user-nif", data.NIF);
				if(data.enterprise != null)
					build.set("user_enterprise", data.enterprise);
				if(data.function != null)
					build.set("user_function", data.function);
				if(data.address != null)
					build.set("user_address", data.address);
				if(data.enterpriseNIF != null)
					build.set("user_enterprise_nif", data.enterpriseNIF);
				user = build.build();
				// get() followed by put() inside a transaction is ok...
				txn.put(user);
				txn.commit();
				LOG.info("User registered " + data.username);
				return Response.ok().build();
			}
		}
		catch (DatastoreException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
