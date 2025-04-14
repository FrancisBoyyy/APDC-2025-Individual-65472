package pt.unl.fct.di.apdc.firstwebapp.util;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.apache.commons.codec.digest.DigestUtils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.unl.fct.di.apdc.firstwebapp.resources.LoginResource;

import java.util.logging.Logger;

@WebListener
public class AppStartup implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartup.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.fine("Démarrage - utilisateur admin en création...");

        String username = "root";
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);

        Entity user = datastore.get(userKey);
        if (user == null) {
            Entity root = Entity.newBuilder(userKey)
                    .set("user_name", "Root Admin")
                    .set("user_phone", "+351000000000")
                    .set("user_pwd", DigestUtils.sha512Hex("rootadmin2025!"))
                    .set("user_email", "root@root.com")
                    .set("user_creation_time", System.currentTimeMillis())
                    .set("user_username", username)
                    .set("user_privacy", "privado")
                    .set("user_status", "ATIVADA")
                    .set("user_role", "ADMIN")
                    .build();

            datastore.put(root);
            LOG.fine("Utilisateur root crée.");
        } else {
            LOG.fine("Utilisateur root éxiste déjâ.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nada a fazer no fim
    }
}