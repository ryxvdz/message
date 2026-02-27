package com.example.SpringCloudKafka.controller;

import com.example.SpringCloudKafka.model.dto.TransacaoRequestDTO;
import com.example.SpringCloudKafka.model.dto.TransacaoResponseDTO;
import com.example.SpringCloudKafka.model.Transacao;
import com.example.SpringCloudKafka.service.FraudeService;
import com.example.SpringCloudKafka.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraude")
public class KafkaProducerController {

    private final TransacaoService transacaoService;
    private final FraudeService fraudeService;

    public KafkaProducerController(TransacaoService transacaoService, FraudeService fraudeService) {
        this.transacaoService = transacaoService;
        this.fraudeService = fraudeService;
    }

    @PostMapping("/transacao")
    public ResponseEntity<TransacaoResponseDTO> submeterTransacao(@Valid @RequestBody TransacaoRequestDTO dto) {
        Transacao transacao = new Transacao(
                UUID.randomUUID().toString(), dto.contaId(), dto.cartaoId(),
                dto.valor(), dto.comerciante(),
                dto.localizacao() != null ? dto.localizacao() : "Desconhecida", "COMPRA"
        );

        TransacaoResponseDTO response = transacaoService.processarNovaTransacao(transacao);
        HttpStatus status = "BLOQUEADA".equals(response.status()) ? HttpStatus.FORBIDDEN : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/transacao/simples")
    public ResponseEntity<TransacaoResponseDTO> criarTransacaoSimples(
            @RequestParam String contaId, @RequestParam String cartaoId,
            @RequestParam BigDecimal valor, @RequestParam String comerciante,
            @RequestParam(defaultValue = "Brasil") String localizacao) {

        Transacao transacao = new Transacao(UUID.randomUUID().toString(), contaId, cartaoId, valor, comerciante, localizacao, "COMPRA");
        TransacaoResponseDTO response = transacaoService.processarNovaTransacao(transacao);

        HttpStatus status = "BLOQUEADA".equals(response.status()) ? HttpStatus.FORBIDDEN : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/transacoes/{contaId}")
    public ResponseEntity<List<Object>> buscarTransacoes(@PathVariable String contaId) {
        return ResponseEntity.ok(fraudeService.buscarTransacoesConta(contaId));
    }

    @PostMapping("/conta/{contaId}/bloquear")
    public ResponseEntity<Map<String, String>> bloquearConta(@PathVariable String contaId, @RequestParam(defaultValue = "Bloqueio manual") String motivo) {
        return ResponseEntity.ok(fraudeService.bloquearConta(contaId, motivo));
    }

    @PostMapping("/conta/{contaId}/desbloquear")
    public ResponseEntity<Map<String, String>> desbloquearConta(@PathVariable String contaId) {
        return ResponseEntity.ok(fraudeService.desbloquearConta(contaId));
    }

    @GetMapping("/conta/{contaId}/status")
    public ResponseEntity<Map<String, Object>> verificarStatusConta(@PathVariable String contaId) {
        return ResponseEntity.ok(fraudeService.verificarStatusConta(contaId));
    }

    @DeleteMapping("/conta/{contaId}/cache")
    public ResponseEntity<Map<String, String>> limparCache(@PathVariable String contaId) {
        return ResponseEntity.ok(fraudeService.limparCacheConta(contaId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Fraud Detection Service"));
    }
}