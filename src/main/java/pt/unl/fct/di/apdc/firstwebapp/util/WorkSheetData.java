package pt.unl.fct.di.apdc.firstwebapp.util;

public class WorkSheetData {

    private String[] propriedadeAlvo = {"Propriedade Pública", "Propriedade Privada"};
    private String[] WorkStates = {"NÃO INICIADO", "EM CURSO", "CONCLUÍDO"};
    private String[] Adjudicações = {"ADJUDICADO", "NÃO ADJUDICADO"};

    public String referencia;
    public String descricao;
    public String tipoAlvo;
    public String estadoAdjudicacao;

    public String dataAdjudicacao;
    public String inicioObra;
    public String contaEntidade;
    public String fimObra;
    public String nomeEmpresa;
    public String nifEmpresa;
    public String estadoObra;
    public String observacoes;



    public WorkSheetData(){}

    public WorkSheetData (String referencia,
                          String descricao,
                          String tipoAlvo,
                          String estadoAdjudicacao,
                          String dataAdjudicacao,
                          String inicioObra,
                          String contaEntidade,
                          String fimObra,
                          String nomeEmpresa,
                          String nifEmpresa,
                          String estadoObra,
                          String observacoes){
        this.referencia = referencia;
        this.descricao = descricao;
        this.tipoAlvo = tipoAlvo;
        this.estadoAdjudicacao = estadoAdjudicacao;

        this.dataAdjudicacao = dataAdjudicacao;
        this.inicioObra = inicioObra;
        this.contaEntidade = contaEntidade;
        this.fimObra = fimObra;
        this.nomeEmpresa = nomeEmpresa;
        this.nifEmpresa = nifEmpresa;
        this.estadoObra = estadoObra;
        this.observacoes = observacoes;
    }

    public boolean validRegistration() {
        return nonEmptyOrBlankField(referencia) &&
                nonEmptyOrBlankField(descricao) &&
                validTargetProperty(tipoAlvo) &&
                validAdjudication(estadoAdjudicacao);
    }

    private boolean validTargetProperty(String target) {
        return (nonEmptyOrBlankField(target) &&
                (target.equals("Propriedade Pública") ||
                        target.equals("Propriedade Privada")));
    }

    private boolean validWorkState(String estadoObra) {
        return nonEmptyOrBlankField(estadoObra) &&
                (estadoObra.equals("NÃO INICIADO") ||
                        estadoObra.equals("EM CURSO") ||
                        estadoObra.equals("CONCLUÍDO"));
    }

    private boolean validAdjudication(String estadoAdjudicacao) {
        if (estadoAdjudicacao != null){
            if (estadoAdjudicacao.equals("ADJUDICADO"))
                return nonEmptyOrBlankField(dataAdjudicacao) &&
                        nonEmptyOrBlankField(inicioObra) &&
                        nonEmptyOrBlankField(fimObra) &&
                        nonEmptyOrBlankField(contaEntidade) &&
                        nonEmptyOrBlankField(nomeEmpresa) &&
                        nonEmptyOrBlankField(nifEmpresa) &&
                        validWorkState(estadoObra) &&
                        nonEmptyOrBlankField(observacoes);
            else if(estadoAdjudicacao.equals("NÃO ADJUDICADO"))
                return ((dataAdjudicacao == null) && (inicioObra == null) && (fimObra == null));
        }
        return false;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validUpdate() {
        return nonEmptyOrBlankField(contaEntidade) && nonEmptyOrBlankField(estadoObra) && nonEmptyOrBlankField(referencia);
    }
}