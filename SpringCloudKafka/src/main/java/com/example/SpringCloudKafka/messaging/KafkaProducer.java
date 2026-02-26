package com.example.SpringCloudKafka.messaging;

import com.example.SpringCloudKafka.model.ResultadoFraude;
import com.example.SpringCloudKafka.model.Transacao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public KafkaProducer(StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    public void enviarTransacao(Transacao transacao) {
        try {
            String message = objectMapper.writeValueAsString(transacao);
            streamBridge.send("transacao-topic", message);
            logger.info("Transação enviada para análise: {}", transacao.getId());
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar transação: {}", e.getMessage(), e);
        }
    }

    public void enviarResultadoFraude(ResultadoFraude resultado) {
        try {
            String message = objectMapper.writeValueAsString(resultado);
            streamBridge.send("fraude-detectada-topic", message);
            logger.info("Resultado de fraude enviado - Transação: {}, Fraude: {}",
                       resultado.getTransacao().getId(), resultado.isFraude());
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar resultado de fraude: {}", e.getMessage(), e);
        }
    }

    public void sendMessage(String topic, String message) {
        streamBridge.send(topic, message);
        logger.info("Mensagem enviada para tópico {}: {}", topic, message);
    }
}
