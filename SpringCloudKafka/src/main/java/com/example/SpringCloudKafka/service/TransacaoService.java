package com.example.SpringCloudKafka.service;

import com.example.SpringCloudKafka.model.dto.TransacaoResponseDTO;
import com.example.SpringCloudKafka.messaging.KafkaProducer;
import com.example.SpringCloudKafka.model.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoService.class);
    private final KafkaProducer producerService;
    private final FraudeService fraudeService;

    public TransacaoService(KafkaProducer producerService, FraudeService fraudeService) {
        this.producerService = producerService;
        this.fraudeService = fraudeService;
    }

    public TransacaoResponseDTO processarNovaTransacao(Transacao transacao) {
        logger.info("Analisando pre-requisitos da transacao: {}", transacao.getId());

        if (fraudeService.isContaBloqueada(transacao.getContaId())) {
            return new TransacaoResponseDTO("BLOQUEADA", "Transação rejeitada - Conta bloqueada", transacao.getId());
        }

        producerService.enviarTransacao(transacao);
        return new TransacaoResponseDTO("PROCESSANDO", "Transação enviada para análise assíncrona", transacao.getId());
    }
}