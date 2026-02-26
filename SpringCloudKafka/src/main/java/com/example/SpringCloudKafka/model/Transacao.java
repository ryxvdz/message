package com.example.SpringCloudKafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Dados de uma transação financeira")
public class Transacao implements Serializable {

    @Schema(description = "ID único da transação (gerado automaticamente se não fornecido)",
            example = "trans-123456")
    private String id;

    @Schema(description = "ID da conta que realizou a transação",
            example = "conta123")
    private String contaId;

    @Schema(description = "ID do cartão utilizado",
            example = "cartao456")
    private String cartaoId;

    @Schema(description = "Valor da transação em reais",
            example = "2500.00")
    private BigDecimal valor;

    @Schema(description = "Nome do comerciante/estabelecimento",
            example = "Magazine Luiza")
    private String comerciante;

    @Schema(description = "Localização onde a transação foi realizada",
            example = "São Paulo")
    private String localizacao;

    @Schema(description = "Tipo de transação",
            example = "COMPRA",
            allowableValues = {"COMPRA", "SAQUE", "TRANSFERENCIA", "PAGAMENTO"})
    private String tipoTransacao;

    @Schema(description = "Data e hora da transação (gerada automaticamente)",
            example = "2026-02-26T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHora;

    public Transacao() {
        this.dataHora = LocalDateTime.now();
    }

    public Transacao(String id, String contaId, String cartaoId, BigDecimal valor,
                     String comerciante, String localizacao, String tipoTransacao) {
        this.id = id;
        this.contaId = contaId;
        this.cartaoId = cartaoId;
        this.valor = valor;
        this.comerciante = comerciante;
        this.localizacao = localizacao;
        this.tipoTransacao = tipoTransacao;
        this.dataHora = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContaId() {
        return contaId;
    }

    public void setContaId(String contaId) {
        this.contaId = contaId;
    }

    public String getCartaoId() {
        return cartaoId;
    }

    public void setCartaoId(String cartaoId) {
        this.cartaoId = cartaoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getComerciante() {
        return comerciante;
    }

    public void setComerciante(String comerciante) {
        this.comerciante = comerciante;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getTipoTransacao() {
        return tipoTransacao;
    }

    public void setTipoTransacao(String tipoTransacao) {
        this.tipoTransacao = tipoTransacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    @Override
    public String toString() {
        return "Transacao{" +
                "id='" + id + '\'' +
                ", contaId='" + contaId + '\'' +
                ", cartaoId='" + cartaoId + '\'' +
                ", valor=" + valor +
                ", comerciante='" + comerciante + '\'' +
                ", localizacao='" + localizacao + '\'' +
                ", tipoTransacao='" + tipoTransacao + '\'' +
                ", dataHora=" + dataHora +
                '}';
    }
}


