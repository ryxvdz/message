package com.example.SpringCloudKafka.controller;

import com.example.SpringCloudKafka.messaging.KafkaProducer;
import com.example.SpringCloudKafka.model.Transacao;
import com.example.SpringCloudKafka.service.FraudeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraude")
@Tag(name = "Sistema de Detecção de Fraude", description = "API para detecção de fraude em transações financeiras")
public class KafkaProducerController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerController.class);

    private final KafkaProducer producerService;
    private final FraudeService fraudeService;

    public KafkaProducerController(KafkaProducer producerService, FraudeService fraudeService) {
        this.producerService = producerService;
        this.fraudeService = fraudeService;
    }

    @Operation(
        summary = "Submeter Transação Completa",
        description = """
            Submete uma transação completa para análise de fraude. A transação será processada 
            assincronamente através do Kafka e analisada por 5 critérios diferentes de detecção de fraude.
            
            **Score de Risco:**
            - 0-49: Baixo risco (aprovado)
            - 50-79: Alto risco (marcado como fraude)
            - 80+: Risco crítico (fraude + bloqueio automático da conta)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Transação aceita para processamento",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "status": "PROCESSANDO",
                        "mensagem": "Transação recebida e em análise",
                        "transacaoId": "uuid-gerado"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Conta bloqueada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "status": "BLOQUEADA",
                        "mensagem": "Transação rejeitada - Conta bloqueada",
                        "transacaoId": "uuid"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping("/transacao")
    public ResponseEntity<Map<String, Object>> submeterTransacao(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados completos da transação",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = Transacao.class),
                    examples = @ExampleObject(value = """
                        {
                            "contaId": "conta123",
                            "cartaoId": "cartao456",
                            "valor": 2500.00,
                            "comerciante": "Magazine Luiza",
                            "localizacao": "São Paulo",
                            "tipoTransacao": "COMPRA"
                        }
                        """)
                )
            )
            @RequestBody Transacao transacao) {
        try {
            if (transacao.getId() == null || transacao.getId().isEmpty()) {
                transacao.setId(UUID.randomUUID().toString());
            }

            logger.info("Recebendo transação para análise: {}", transacao);

            if (fraudeService.isContaBloqueada(transacao.getContaId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "BLOQUEADA");
                response.put("mensagem", "Transação rejeitada - Conta bloqueada");
                response.put("transacaoId", transacao.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            producerService.enviarTransacao(transacao);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "PROCESSANDO");
            response.put("mensagem", "Transação recebida e em análise");
            response.put("transacaoId", transacao.getId());

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Erro ao processar transação: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERRO");
            response.put("mensagem", "Erro ao processar transação: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
        summary = "Submeter Transação Simples",
        description = """
            Submete uma transação de forma simplificada usando query parameters.
            Ideal para testes rápidos. O ID da transação é gerado automaticamente.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Transação aceita"),
        @ApiResponse(responseCode = "403", description = "Conta bloqueada")
    })
    @PostMapping("/transacao/simples")
    public ResponseEntity<Map<String, Object>> criarTransacaoSimples(
            @Parameter(description = "ID da conta", example = "conta123", required = true)
            @RequestParam String contaId,
            @Parameter(description = "ID do cartão", example = "cartao456", required = true)
            @RequestParam String cartaoId,
            @Parameter(description = "Valor da transação", example = "150.50", required = true)
            @RequestParam BigDecimal valor,
            @Parameter(description = "Nome do comerciante", example = "Amazon", required = true)
            @RequestParam String comerciante,
            @Parameter(description = "Localização da transação", example = "Brasil")
            @RequestParam(defaultValue = "Brasil") String localizacao) {

        Transacao transacao = new Transacao(
                UUID.randomUUID().toString(),
                contaId,
                cartaoId,
                valor,
                comerciante,
                localizacao,
                "COMPRA"
        );

        return submeterTransacao(transacao);
    }

    @Operation(
        summary = "Buscar Histórico de Transações",
        description = "Retorna a lista de transações recentes de uma conta armazenadas em cache (últimos 5 minutos)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de transações retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro ao buscar transações")
    })
    @GetMapping("/transacoes/{contaId}")
    public ResponseEntity<List<Object>> buscarTransacoes(
            @Parameter(description = "ID da conta", example = "conta123", required = true)
            @PathVariable String contaId) {
        try {
            List<Object> transacoes = fraudeService.buscarTransacoesConta(contaId);
            return ResponseEntity.ok(transacoes);
        } catch (Exception e) {
            logger.error("Erro ao buscar transações: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Bloquear Conta",
        description = """
            Bloqueia uma conta manualmente. Contas bloqueadas não podem realizar novas transações.
            O bloqueio expira automaticamente após 24 horas.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conta bloqueada com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "status": "BLOQUEADA",
                        "contaId": "conta123",
                        "motivo": "Bloqueio manual"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Erro ao bloquear conta")
    })
    @PostMapping("/conta/{contaId}/bloquear")
    public ResponseEntity<Map<String, String>> bloquearConta(
            @Parameter(description = "ID da conta a ser bloqueada", example = "conta123", required = true)
            @PathVariable String contaId,
            @Parameter(description = "Motivo do bloqueio", example = "Atividade suspeita detectada")
            @RequestParam(defaultValue = "Bloqueio manual") String motivo) {

        try {
            fraudeService.bloquearConta(contaId, motivo);
            Map<String, String> response = new HashMap<>();
            response.put("status", "BLOQUEADA");
            response.put("contaId", contaId);
            response.put("motivo", motivo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao bloquear conta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Desbloquear Conta",
        description = "Remove o bloqueio de uma conta, permitindo novas transações"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conta desbloqueada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro ao desbloquear conta")
    })
    @PostMapping("/conta/{contaId}/desbloquear")
    public ResponseEntity<Map<String, String>> desbloquearConta(
            @Parameter(description = "ID da conta a ser desbloqueada", example = "conta123", required = true)
            @PathVariable String contaId) {
        try {
            fraudeService.desbloquearConta(contaId);
            Map<String, String> response = new HashMap<>();
            response.put("status", "DESBLOQUEADA");
            response.put("contaId", contaId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao desbloquear conta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Verificar Status da Conta",
        description = "Verifica se uma conta está bloqueada ou ativa"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status da conta retornado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "contaId": "conta123",
                        "bloqueada": false,
                        "status": "ATIVA"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Erro ao verificar status")
    })
    @GetMapping("/conta/{contaId}/status")
    public ResponseEntity<Map<String, Object>> verificarStatusConta(
            @Parameter(description = "ID da conta", example = "conta123", required = true)
            @PathVariable String contaId) {
        try {
            boolean bloqueada = fraudeService.isContaBloqueada(contaId);
            Map<String, Object> response = new HashMap<>();
            response.put("contaId", contaId);
            response.put("bloqueada", bloqueada);
            response.put("status", bloqueada ? "BLOQUEADA" : "ATIVA");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao verificar status da conta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Limpar Cache da Conta",
        description = """
            Remove todos os dados em cache relacionados a uma conta específica, incluindo:
            - Histórico de transações
            - Dados de localização
            - Controle de duplicatas
            
            Útil para testes ou para resetar o histórico de uma conta.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache limpo com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro ao limpar cache")
    })
    @DeleteMapping("/conta/{contaId}/cache")
    public ResponseEntity<Map<String, String>> limparCache(
            @Parameter(description = "ID da conta", example = "conta123", required = true)
            @PathVariable String contaId) {
        try {
            fraudeService.limparCacheConta(contaId);
            Map<String, String> response = new HashMap<>();
            response.put("status", "CACHE_LIMPO");
            response.put("contaId", contaId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao limpar cache: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Health Check",
        description = "Verifica se o serviço está funcionando corretamente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Serviço está funcionando",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "status": "UP",
                        "service": "Fraud Detection Service"
                    }
                    """)
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Fraud Detection Service");
        return ResponseEntity.ok(response);
    }
}
