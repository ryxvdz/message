package com.example.SpringCloudKafka.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class FraudeService {

    private static final Integer TTL_EM_MINUTOS = 5;

    private final Map<String, LocalDateTime> chavesExistentes;

    public FraudeService(Map<String, LocalDateTime> chavesExistentes) {
        this.chavesExistentes = chavesExistentes;
    }

    public boolean isFraude(String chave) {

        LocalDateTime agora = LocalDateTime.now();

        if (!chavesExistentes.containsKey(chave)) {
            updateChavesExistentes(chavesExistentes, chave, agora);
            return false;
        }
        LocalDateTime momentoDoRegistroDaChave = chavesExistentes.get(chave);

        Duration duration = Duration.between(momentoDoRegistroDaChave, agora);
        long minutes = duration.toMinutes();

        updateChavesExistentes(chavesExistentes, chave, agora);

        return minutes < TTL_EM_MINUTOS;
    }

    private void updateChavesExistentes(
            Map<String, LocalDateTime> chavesExistentes,
            String novaChave,
            LocalDateTime novoAgora) {

        chavesExistentes.put(novaChave, novoAgora);

    }
}

