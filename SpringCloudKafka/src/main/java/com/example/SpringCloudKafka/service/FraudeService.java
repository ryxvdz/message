package com.example.SpringCloudKafka.service;

import com.example.SpringCloudKafka.model.ResultadoFraude;
import com.example.SpringCloudKafka.model.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class FraudeService {

    private static final Logger logger = LoggerFactory.getLogger(FraudeService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${fraud.detection.max-amount:10000.0}")
    private Double limiteSeguro;

    @Value("${fraud.detection.time-window-minutes:5}")
    private Integer janelaTempo;

    @Value("${fraud.detection.max-transactions-per-window:3}")
    private Integer maximoTransacoesPeriodo;

    @Value("${fraud.detection.velocity-check-minutes:10}")
    private Integer velocidadeChecagem;

    public FraudeService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ResultadoFraude analisarTransacao(Transacao transacao) {
        ResultadoFraude resultado = new ResultadoFraude(transacao, false, 0.0);
        double scoreRisco = 0.0;

        logger.info("Analisando transação: {}", transacao.getId());

        if (isValorSuspeito(transacao.getValor())) {
            scoreRisco += 30.0;
            resultado.adicionarMotivo("Valor acima do limite suspeito: " + transacao.getValor());
        }

        if (isVelocidadeSuspeita(transacao.getContaId())) {
            scoreRisco += 40.0;
            resultado.adicionarMotivo("Múltiplas transações em curto período de tempo");
        }

        if (isTransacaoDuplicada(transacao)) {
            scoreRisco += 50.0;
            resultado.adicionarMotivo("Possível transação duplicada");
        }

        if (isMudancaGeograficaSuspeita(transacao.getContaId(), transacao.getLocalizacao())) {
            scoreRisco += 35.0;
            resultado.adicionarMotivo("Mudança geográfica suspeita detectada");
        }

        if (isHorarioSuspeito(transacao.getDataHora())) {
            scoreRisco += 15.0;
            resultado.adicionarMotivo("Transação em horário incomum");
        }

        resultado.setScoreRisco(scoreRisco);
        resultado.setFraude(scoreRisco >= 50.0);

        registrarTransacao(transacao);

        logger.info("Resultado da análise - ID: {}, Fraude: {}, Score: {}",
                   transacao.getId(), resultado.isFraude(), resultado.getScoreRisco());

        return resultado;
    }

    private boolean isValorSuspeito(BigDecimal valor) {
        return valor.compareTo(BigDecimal.valueOf(limiteSeguro)) > 0;
    }

    private boolean isVelocidadeSuspeita(String contaId) {
        String key = "transacao:conta:" + contaId;
        Long count = redisTemplate.opsForList().size(key);

        if (count != null && count >= maximoTransacoesPeriodo) {
            logger.warn("Velocidade suspeita detectada para conta: {} - {} transações", contaId, count);
            return true;
        }
        return false;
    }

    private boolean isTransacaoDuplicada(Transacao transacao) {
        String key = "transacao:duplicata:" + transacao.getCartaoId() + ":" +
                     transacao.getValor() + ":" + transacao.getComerciante();

        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            logger.warn("Transação duplicada detectada: {}", key);
            return true;
        }

        redisTemplate.opsForValue().set(key, transacao.getId(), janelaTempo, TimeUnit.MINUTES);
        return false;
    }

    private boolean isMudancaGeograficaSuspeita(String contaId, String novaLocalizacao) {
        String key = "transacao:localizacao:" + contaId;
        String ultimaLocalizacao = (String) redisTemplate.opsForValue().get(key);

        if (ultimaLocalizacao != null && !ultimaLocalizacao.equals(novaLocalizacao)) {
            String timestampKey = "transacao:localizacao:timestamp:" + contaId;
            Long timestamp = (Long) redisTemplate.opsForValue().get(timestampKey);

            if (timestamp != null) {
                long minutosDesdeUltimaTransacao = (System.currentTimeMillis() - timestamp) / (1000 * 60);
                if (minutosDesdeUltimaTransacao < velocidadeChecagem) {
                    logger.warn("Mudança geográfica rápida: {} -> {} em {} minutos",
                               ultimaLocalizacao, novaLocalizacao, minutosDesdeUltimaTransacao);
                    return true;
                }
            }
        }

        redisTemplate.opsForValue().set(key, novaLocalizacao, 1, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("transacao:localizacao:timestamp:" + contaId,
                                       System.currentTimeMillis(), 1, TimeUnit.HOURS);
        return false;
    }

    private boolean isHorarioSuspeito(LocalDateTime dataHora) {
        int hora = dataHora.getHour();
        return hora >= 2 && hora <= 5; // Entre 2h e 5h da manhã
    }

    private void registrarTransacao(Transacao transacao) {
        String key = "transacao:conta:" + transacao.getContaId();

        redisTemplate.opsForList().leftPush(key, transacao);

        redisTemplate.expire(key, janelaTempo, TimeUnit.MINUTES);

        String transacaoKey = "transacao:id:" + transacao.getId();
        redisTemplate.opsForValue().set(transacaoKey, transacao, 24, TimeUnit.HOURS);
    }

    public List<Object> buscarTransacoesConta(String contaId) {
        String key = "transacao:conta:" + contaId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void bloquearConta(String contaId, String motivo) {
        String key = "conta:bloqueada:" + contaId;
        redisTemplate.opsForValue().set(key, motivo, 24, TimeUnit.HOURS);
        logger.warn("Conta bloqueada: {} - Motivo: {}", contaId, motivo);
    }

    public boolean isContaBloqueada(String contaId) {
        String key = "conta:bloqueada:" + contaId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void desbloquearConta(String contaId) {
        String key = "conta:bloqueada:" + contaId;
        redisTemplate.delete(key);
        logger.info("Conta desbloqueada: {}", contaId);
    }

    public void limparCacheConta(String contaId) {
        Set<String> keys = redisTemplate.keys("*:" + contaId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete("transacao:conta:" + contaId);
        logger.info("Cache limpo para conta: {}", contaId);
    }
}
