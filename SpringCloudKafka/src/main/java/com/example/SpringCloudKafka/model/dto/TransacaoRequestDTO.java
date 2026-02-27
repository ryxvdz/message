package com.example.SpringCloudKafka.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransacaoRequestDTO(
        @NotBlank(message = "ID da conta obrigatorio") String contaId,
        @NotBlank(message = "ID do cartao obrigatorio") String cartaoId,
        @NotNull(message = "Valor obrigatorio") @Positive(message = "Valor deve ser positivo") BigDecimal valor,
        @NotBlank(message = "Comerciante obrigatorio") String comerciante,
        String localizacao
) {}