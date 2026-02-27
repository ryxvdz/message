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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        if (isValorSuspeito(transacao.getValor())) {
            scoreRisco += 30.0;
            resultado.adicionarMotivo("Valor acima do limite");
        }

        if (isVelocidadeSuspeita(transacao.getContaId())) {
            scoreRisco += 40.0;
            resultado.adicionarMotivo("Múltiplas transações em curto período");
        }

        if (isTransacaoDuplicada(transacao)) {
            scoreRisco += 50.0;
            resultado.adicionarMotivo("Possível transação duplicada");
        }

        if (isMudancaGeograficaSuspeita(transacao.getContaId(), transacao.getLocalizacao())) {
            scoreRisco += 35.0;
            resultado.adicionarMotivo("Mudança geográfica suspeita");
        }

        if (isHorarioSuspeito(transacao.getDataHora())) {
            scoreRisco += 15.0;
            resultado.adicionarMotivo("Horário incomum");
        }

        resultado.setScoreRisco(scoreRisco);
        resultado.setFraude(scoreRisco >= 50.0);
        registrarTransacao(transacao);

        return resultado;
    }

    private boolean isValorSuspeito(BigDecimal valor) {
        return valor.compareTo(BigDecimal.valueOf(limiteSeguro)) > 0;
    }

    private boolean isVelocidadeSuspeita(String contaId) {
        String key = "transacao:conta:zset:" + contaId;
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - TimeUnit.MINUTES.toMillis(janelaTempo);

        Long count = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
        return count != null && count >= maximoTransacoesPeriodo;
    }

    private boolean isTransacaoDuplicada(Transacao transacao) {
        String key = "transacao:duplicata:" + transacao.getCartaoId() + ":" + transacao.getValor() + ":" + transacao.getComerciante();
        return Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, transacao.getId(), janelaTempo, TimeUnit.MINUTES));
    }

    private boolean isMudancaGeograficaSuspeita(String contaId, String novaLocalizacao) {
        String key = "transacao:localizacao:" + contaId;
        String ultimaLocalizacao = (String) redisTemplate.opsForValue().get(key);

        if (ultimaLocalizacao != null && !ultimaLocalizacao.equals(novaLocalizacao)) {
            Long timestamp = (Long) redisTemplate.opsForValue().get("transacao:localizacao:timestamp:" + contaId);
            if (timestamp != null && (System.currentTimeMillis() - timestamp) / (1000 * 60) < velocidadeChecagem) {
                return true;
            }
        }

        redisTemplate.opsForValue().set(key, novaLocalizacao, 1, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("transacao:localizacao:timestamp:" + contaId, System.currentTimeMillis(), 1, TimeUnit.HOURS);
        return false;
    }

    private boolean isHorarioSuspeito(LocalDateTime dataHora) {
        int hora = dataHora.getHour();
        return hora >= 2 && hora <= 5;
    }

    private void registrarTransacao(Transacao transacao) {
        String key = "transacao:conta:zset:" + transacao.getContaId();
        long currentTime = System.currentTimeMillis();

        redisTemplate.opsForZSet().add(key, transacao, currentTime);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, currentTime - TimeUnit.MINUTES.toMillis(janelaTempo));
        redisTemplate.expire(key, janelaTempo, TimeUnit.MINUTES);
    }

    public List<Object> buscarTransacoesConta(String contaId) {
        Set<Object> transacoes = redisTemplate.opsForZSet().reverseRange("transacao:conta:zset:" + contaId, 0, -1);
        return transacoes != null ? new ArrayList<>(transacoes) : new ArrayList<>();
    }

    public Map<String, String> bloquearConta(String contaId, String motivo) {
        redisTemplate.opsForValue().set("conta:bloqueada:" + contaId, motivo, 24, TimeUnit.HOURS);
        return Map.of("status", "BLOQUEADA", "contaId", contaId, "motivo", motivo);
    }

    public boolean isContaBloqueada(String contaId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("conta:bloqueada:" + contaId));
    }

    public Map<String, String> desbloquearConta(String contaId) {
        redisTemplate.delete("conta:bloqueada:" + contaId);
        return Map.of("status", "DESBLOQUEADA", "contaId", contaId);
    }

    public Map<String, Object> verificarStatusConta(String contaId) {
        boolean bloqueada = isContaBloqueada(contaId);
        return Map.of("contaId", contaId, "bloqueada", bloqueada, "status", bloqueada ? "BLOQUEADA" : "ATIVA");
    }

    public Map<String, String> limparCacheConta(String contaId) {
        redisTemplate.delete(List.of(
                "transacao:conta:zset:" + contaId,
                "transacao:localizacao:" + contaId,
                "transacao:localizacao:timestamp:" + contaId,
                "conta:bloqueada:" + contaId
        ));
        return Map.of("status", "CACHE_LIMPO", "contaId", contaId);
    }
}