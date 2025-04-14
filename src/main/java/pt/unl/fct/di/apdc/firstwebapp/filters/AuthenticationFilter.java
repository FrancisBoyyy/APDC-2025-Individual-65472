package pt.unl.fct.di.apdc.firstwebapp.filters;

// Removidos imports não utilizados de resources.*
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken; // Certifique-se que este import está correto para a sua classe AuthToken

import java.io.IOException;
import java.util.logging.Level; // Import necessário para Level.WARNING
import java.util.logging.Logger;

@Provider // Garante que esta anotação está presente para descoberta (se usar scan de pacotes)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
    private static final Gson g = new Gson();
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    // Injeção do HttpServletRequest para poder usar setAttribute
    @Context
    private HttpServletRequest httpRequest;

    public AuthenticationFilter() {
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Usa LOG.info ou LOG.fine conforme a sua configuração de logging
        LOG.info("AuthFilter: Executando filtro...");
        String path = requestContext.getUriInfo().getPath();
        // Log para depurar o path exato recebido
        LOG.info("AuthFilter: Path relativo recebido: " + path);

        // CORREÇÃO: Comparar apenas com o nome do recurso, sem o prefixo do servlet mapping
        if ("login/login_user".equals(path) || "register/new_user".equals(path)) {
            LOG.info("AuthFilter: Path " + path + " não requer autenticação. Bypass.");
            return; // Não aplica filtro para login/register
        }

        // Obter o cabeçalho Authorization
        String authHeader = requestContext.getHeaderString("Authorization");
        LOG.info("AuthFilter: Cabeçalho Authorization: " + authHeader);

        // Validar presença e formato do cabeçalho
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.warning("AuthFilter: Cabeçalho Authorization ausente ou malformado.");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid Authorization header").build());
            return;
        }

        try {
            // Extrair a string do token
            String tokenStr = authHeader.substring("Bearer ".length()).trim(); // Adicionado trim() por segurança

            // Desserializar o token
            AuthToken token = g.fromJson(tokenStr, AuthToken.class);

            // Verificação de Nulo (Opcional mas recomendado) - Adapte conforme a estrutura de AuthToken
            if (token == null || token.tokenID == null) {
                LOG.warning("AuthFilter: Token JSON desserializado, mas com campos essenciais nulos.");
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid token structure").build());
                return;
            }

            /**
            // Verificar expiração
            // A comparação parece correta: se now > validity.to, então expirou.
            if (System.currentTimeMillis() > (token.VALID_TO)) {
                LOG.warning("AuthFilter: Token expirado. User: " + token.username);
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Token expired").build());
                return;
            }
             */

            // Verificar se o token foi revogado (lista negra)
            Key revokedKey = datastore.newKeyFactory().setKind("RevokedToken")
                    .newKey(token.tokenID);
            LOG.info("AuthFilter: Verificando revogação para chave: " + revokedKey.toString());
            if (datastore.get(revokedKey) != null) {
                LOG.warning("AuthFilter: Token foi revogado. User: " + token.username + ", Verificador: " + token.tokenID);
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Token has been revoked").build());
                return; // Esqueci-me do return aqui na versão anterior, adicionado agora.
            }

            // Verificar expiração
            // A comparação parece correta: se now > validity.to, então expirou.
            if (System.currentTimeMillis() > (token.VALID_TO)) {
                Key revokedTokenKey = datastore.newKeyFactory()
                        .setKind("SessionToken")
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
                LOG.warning("AuthFilter: Token expirado. User: " + token.username);
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Token expired").build());
                return;
            }

            // Token válido! Colocar no contexto do pedido para o Resource usar
            LOG.info("AuthFilter: Token válido para user: " + token.username + ". Adicionando ao request attribute.");
            httpRequest.setAttribute("authToken", token);
            // requestContext.setProperty("authToken", token); // Removido por ser redundante com setAttribute

        } catch (com.google.gson.JsonSyntaxException jsonEx) {
            // Captura específica para erro de JSON
            LOG.log(Level.WARNING, "AuthFilter: Falha ao fazer parse do JSON do token: " + authHeader.substring("Bearer ".length()), jsonEx);
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token format").build());
        } catch (Exception e) {
            // MELHORIA: Logar a exceção para depuração no servidor
            LOG.log(Level.SEVERE, "AuthFilter: Erro inesperado ao validar o token: " + authHeader.substring("Bearer ".length()), e);
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token").build());
        }
    }
}