package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.Role;
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
import pt.unl.fct.di.apdc.firstwebapp.util.WorkSheetData;

import java.util.logging.Logger;

@Path("/worksheet")
public class WorkSheetResources {

    private static final Logger LOG = Logger.getLogger(WorkSheetResources.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory worksheetKeyFactory = datastore.newKeyFactory().setKind("WorkSheet");
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private final Gson g = new Gson();

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(WorkSheetData data, @Context HttpServletRequest request) {

        AuthToken token = (AuthToken) request.getAttribute("authToken");

        if (!data.validRegistration()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        String username = token.username;
        String role = token.role;

        //if (!Roles.valueOf(role).equals(Roles.BACKOFFICE)) {
        if (!role.equals("BACKOFFICE")) {
            return Response.status(Response.Status.FORBIDDEN).entity("Role not authorized").build();
        }

        Key workSheetKey = worksheetKeyFactory.newKey(data.referencia);
        Transaction txn = datastore.newTransaction();

        try {
            String adjudicacao = data.estadoAdjudicacao;
            Entity.Builder builder = Entity.newBuilder(workSheetKey)
                    .set("obra_referencia", data.referencia)
                    .set("obra_descricao", data.descricao)
                    .set("obra_tipo", data.tipoAlvo.toUpperCase())
                    .set("obra_estado_adjudicacao", data.estadoAdjudicacao.toUpperCase());

            LOG.info("ZANGADOPIMPOLHO 9");
            if (adjudicacao.equals("ADJUDICADO")) {
                builder.set("data_adjudicacao", data.dataAdjudicacao)
                        .set("data_inicio_obra", data.inicioObra)
                        .set("data_fim_obra", data.fimObra)
                        .set("conta_entidade", data.contaEntidade)
                        .set("empresa_adjudicada", data.nomeEmpresa)
                        .set("nif_empresa", data.nifEmpresa)
                        .set("estado_obra", data.estadoObra.toUpperCase())
                        .set("observacoes", data.observacoes);
            }

            // BACKOFFICE pode criar/atualizar tudo
            txn.put(builder.build());
            txn.commit();
            return Response.ok("Worksheet saved successfully").build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Erro ao registar folha de obra: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro interno").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkSheet(WorkSheetData data, @Context HttpServletRequest request) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");

        String username = token.username;
        if (!data.validUpdate() || !isValidPartner(data.contaEntidade))
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();

        Key workSheetKey = worksheetKeyFactory.newKey(data.referencia);
        Transaction txn = datastore.newTransaction();

        try {
            if (!token.role.equals("BACKOFFICE") && !token.role.equals("PARTNER")) {
                return Response.status(Response.Status.FORBIDDEN).entity("Role not authorized").build();
            }
            Entity worksheet = txn.get(workSheetKey);
            // PARTNER só pode alterar o estado da obra se for o responsável
            if (token.role.equals("PARTNER")) {
                if (worksheet == null || !worksheet.getString("conta_entidade").equals(username)) {
                    txn.rollback();
                    return Response.status(Response.Status.FORBIDDEN).entity("You cannot update this worksheet").build();
                }
            }
            Entity updated;
            Entity.Builder almostUpdated = Entity.newBuilder(worksheet);
            if(data.estadoAdjudicacao!=null) {
                almostUpdated.set("estado_adjudicacao", data.estadoAdjudicacao);
            }
            if(data.dataAdjudicacao!=null) {
                almostUpdated.set("data_adjudicacao", data.dataAdjudicacao);
            }
            if(data.inicioObra!=null) {
                almostUpdated.set("data_inicio_obra", data.inicioObra);
            }
            if(data.contaEntidade!=null) {
                almostUpdated.set("conta_entidade", data.contaEntidade);
            }
            if(data.fimObra!=null) {
                almostUpdated.set("data_fim_obra", data.fimObra);
            }
            if(data.nomeEmpresa!=null) {
                almostUpdated.set("empresa_adjudicada", data.nomeEmpresa);
            }
            if(data.nifEmpresa!=null) {
                almostUpdated.set("nif_empresa", data.nifEmpresa);
            }
            if(data.estadoObra!=null) {
                almostUpdated.set("estado_obra", data.estadoObra);
            }
            if(data.observacoes!=null) {
                almostUpdated.set("observacoes", data.observacoes);
            }

            updated = almostUpdated.build();

            txn.put(updated);
            txn.commit();
            return Response.ok("Worksheet status updated by " + token.role).build();
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Erro ao registar folha de obra: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro interno").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    private boolean isValidPartner(String contaEntidade) {
        if (contaEntidade != null) {
            Entity user = getUserByID(contaEntidade);

            if (user == null) {
                return false;
            }
            String role = user.getString("user_role");
            return role.equals("PARTNER");
        }
        return false;
    }

    private Entity getUserByID(String userID){
        Key targetUserKey = userKeyFactory.newKey(userID);
        return datastore.get(targetUserKey);
    }
}
