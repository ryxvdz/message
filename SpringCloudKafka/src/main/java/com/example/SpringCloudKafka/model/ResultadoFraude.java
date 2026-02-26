package com.example.SpringCloudKafka.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Resultado da análise de fraude de uma transação")
public class ResultadoFraude implements Serializable {

    @Schema(description = "Transação analisada")
    private Transacao transacao;

    @Schema(description = "Indica se a transação foi classificada como fraude (score >= 50)",
            example = "true")
    private boolean isFraude;

    @Schema(description = "Score de risco calculado (0-100+)",
            example = "85.0")
    private double scoreRisco;

    @Schema(description = "Lista de motivos que contribuíram para o score",
            example = "[\"Valor acima do limite suspeito: 15000.0\", \"Múltiplas transações em curto período de tempo\"]")
    private List<String> motivos;

    @Schema(description = "Data e hora da análise",
            example = "2026-02-26T14:30:00")
    private LocalDateTime dataAnalise;

    public ResultadoFraude() {
        this.motivos = new ArrayList<>();
        this.dataAnalise = LocalDateTime.now();
    }

    public ResultadoFraude(Transacao transacao, boolean isFraude, double scoreRisco) {
        this.transacao = transacao;
        this.isFraude = isFraude;
        this.scoreRisco = scoreRisco;
        this.motivos = new ArrayList<>();
        this.dataAnalise = LocalDateTime.now();
    }

    public void adicionarMotivo(String motivo) {
        this.motivos.add(motivo);
    }

    public Transacao getTransacao() {
        return transacao;
    }

    public void setTransacao(Transacao transacao) {
        this.transacao = transacao;
    }

    public boolean isFraude() {
        return isFraude;
    }

    public void setFraude(boolean fraude) {
        isFraude = fraude;
    }

    public double getScoreRisco() {
        return scoreRisco;
    }

    public void setScoreRisco(double scoreRisco) {
        this.scoreRisco = scoreRisco;
    }

    public List<String> getMotivos() {
        return motivos;
    }

    public void setMotivos(List<String> motivos) {
        this.motivos = motivos;
    }

    public LocalDateTime getDataAnalise() {
        return dataAnalise;
    }

    public void setDataAnalise(LocalDateTime dataAnalise) {
        this.dataAnalise = dataAnalise;
    }

    @Override
    public String toString() {
        return "ResultadoFraude{" +
                "transacao=" + transacao +
                ", isFraude=" + isFraude +
                ", scoreRisco=" + scoreRisco +
                ", motivos=" + motivos +
                ", dataAnalise=" + dataAnalise +
                '}';
    }
}


