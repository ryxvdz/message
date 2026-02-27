package com.example.SpringCloudKafka.model.dto;

public record TransacaoResponseDTO(String status, String mensagem, String transacaoId) {}