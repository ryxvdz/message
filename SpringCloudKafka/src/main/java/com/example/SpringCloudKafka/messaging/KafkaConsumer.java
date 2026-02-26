package com.example.SpringCloudKafka.messaging;

import com.example.SpringCloudKafka.model.ResultadoFraude;
import com.example.SpringCloudKafka.model.Transacao;
import com.example.SpringCloudKafka.service.FraudeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private final FraudeService fraudeService;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    public KafkaConsumer(FraudeService fraudeService, KafkaProducer kafkaProducer, ObjectMapper objectMapper) {
        this.fraudeService = fraudeService;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> fraudTransactionConsumer() {
        return message -> {
            try {
                String payload = message.getPayload();
                logger.info("Transação recebida para análise: {}", payload);

                Transacao transacao = objectMapper.readValue(payload, Transacao.class);

                if (fraudeService.isContaBloqueada(transacao.getContaId())) {
                    logger.warn("Transação rejeitada - Conta bloqueada: {}", transacao.getContaId());
                    ResultadoFraude resultado = new ResultadoFraude(transacao, true, 100.0);
                    resultado.adicionarMotivo("Conta bloqueada por atividade suspeita anterior");
                    kafkaProducer.enviarResultadoFraude(resultado);
                    return;
                }

                ResultadoFraude resultado = fraudeService.analisarTransacao(transacao);

                if (resultado.isFraude() && resultado.getScoreRisco() >= 80.0) {
                    fraudeService.bloquearConta(transacao.getContaId(),
                        "Bloqueio automático - Score de risco: " + resultado.getScoreRisco());
                }

                kafkaProducer.enviarResultadoFraude(resultado);

                logger.info("Análise concluída - Transação: {}, Fraude: {}, Score: {}",
                           transacao.getId(), resultado.isFraude(), resultado.getScoreRisco());

            } catch (Exception e) {
                logger.error("Erro ao processar transação: {}", e.getMessage(), e);
            }
        };
    }

}
