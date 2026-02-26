# Sistema de Detecção de Fraude em Transações

Sistema completo de detecção de fraude em tempo real usando Spring Boot, Kafka e Redis.

## Tecnologias

- **Spring Boot 4.0.3** - Framework principal
- **Spring Cloud Stream** - Integração com Kafka
- **Apache Kafka** - Mensageria para processamento assíncrono
- **Redis** - Cache e controle de taxa (rate limiting)
- **Java 21** - Linguagem de programação

## Funcionalidades

### Detecção de Fraude
O sistema analisa transações em tempo real utilizando múltiplos critérios:

1. **Valor Suspeito** - Detecta transações acima de limites configuráveis
2. **Velocidade de Transações** - Identifica múltiplas transações em curto período
3. **Transações Duplicadas** - Detecta possíveis duplicatas (mesmo cartão, valor e comerciante)
4. **Mudança Geográfica Suspeita** - Alerta sobre transações em locais diferentes rapidamente
5. **Horário Suspeito** - Identifica transações em horários incomuns (madrugada)

### Sistema de Pontuação
- Cada critério adiciona pontos ao score de risco (0-100)
- Score >= 50: Transação marcada como fraude
- Score >= 80: Conta bloqueada automaticamente

### Gerenciamento de Contas
- Bloqueio/desbloqueio manual de contas
- Verificação de status de conta
- Histórico de transações
- Limpeza de cache

## Arquitetura

```
Cliente → REST API → Kafka Producer → Tópico Kafka
                                           ↓
                     Kafka Consumer ← Análise de Fraude
                           ↓                    ↓
                    Redis Cache          Resultado
                                              ↓
                                    Tópico de Fraudes
```

## Configuração

### application.yaml

```yaml
fraud:
  detection:
    max-amount: 10000.0              # Valor máximo suspeito
    time-window-minutes: 5           # Janela de tempo para análise
    max-transactions-per-window: 3   # Máx. transações na janela
    velocity-check-minutes: 10       # Tempo para mudança geográfica
```

## Como Executar

### 1. Iniciar Infraestrutura (Docker)

```bash
cd docker
docker-compose up -d
```

Isso iniciará:
- Kafka (porta 9092)
- Kafka UI (porta 8080)
- Redis (porta 6379)
- Redis Insight (porta 5540)

### 2. Compilar o Projeto

```bash
./gradlew clean build
```

### 3. Executar a Aplicação

```bash
./gradlew bootRun
```

A aplicação estará disponível em: `http://localhost:8081`

## Endpoints da API

### Documentação Interativa (Swagger)

Acesse a documentação completa e interativa em:
```
http://localhost:8081/swagger-ui.html
```

**Features do Swagger:**
- Documentação completa de todos os endpoints
- Teste os endpoints diretamente no navegador
- Exemplos de request e response
- Schemas dos modelos de dados
- Try it out - Execute requisições em tempo real

Para mais informações, consulte [SWAGGER-GUIDE.md](SWAGGER-GUIDE.md)

### Health Check
```bash
GET /api/fraude/health
```

### Submeter Transação Completa
```bash
POST /api/fraude/transacao
Content-Type: application/json

{
  "contaId": "conta123",
  "cartaoId": "cartao456",
  "valor": 5000.00,
  "comerciante": "Loja ABC",
  "localizacao": "São Paulo",
  "tipoTransacao": "COMPRA"
}
```

### Submeter Transação Simples
```bash
POST /api/fraude/transacao/simples?contaId=conta123&cartaoId=cartao456&valor=150.50&comerciante=Amazon&localizacao=Brasil
```

### Buscar Transações de uma Conta
```bash
GET /api/fraude/transacoes/conta123
```

### Bloquear Conta
```bash
POST /api/fraude/conta/conta123/bloquear?motivo=Atividade suspeita
```

### Desbloquear Conta
```bash
POST /api/fraude/conta/conta123/desbloquear
```

### Verificar Status da Conta
```bash
GET /api/fraude/conta/conta123/status
```

### Limpar Cache da Conta
```bash
DELETE /api/fraude/conta/conta123/cache
```

## Testando o Sistema

### Teste 1: Transação Normal
```bash
curl -X POST "http://localhost:8081/api/fraude/transacao/simples?contaId=user1&cartaoId=card1&valor=100&comerciante=Padaria&localizacao=Brasil"
```

### Teste 2: Valor Alto (Suspeito)
```bash
curl -X POST "http://localhost:8081/api/fraude/transacao/simples?contaId=user1&cartaoId=card1&valor=15000&comerciante=JoiasLux&localizacao=Brasil"
```

### Teste 3: Múltiplas Transações (Velocidade)
Execute 4 vezes rapidamente:
```bash
curl -X POST "http://localhost:8081/api/fraude/transacao/simples?contaId=user2&cartaoId=card2&valor=50&comerciante=Store&localizacao=Brasil"
```

### Teste 4: Mudança Geográfica Rápida
```bash
# Primeira transação
curl -X POST "http://localhost:8081/api/fraude/transacao/simples?contaId=user3&cartaoId=card3&valor=100&comerciante=Store1&localizacao=Brasil"

# Segunda transação (imediatamente) em local diferente
curl -X POST "http://localhost:8081/api/fraude/transacao/simples?contaId=user3&cartaoId=card3&valor=100&comerciante=Store2&localizacao=EUA"
```

### Teste 5: Transação Duplicada
Execute 2 vezes rapidamente (mesmo cartão, valor e comerciante):
```bash
curl -X POST "http://localhost:8081/api/fraude/transacao/simples?contaId=user4&cartaoId=card4&valor=99.99&comerciante=Amazon&localizacao=Brasil"
```

## Monitoramento

### Kafka UI
Acesse: `http://localhost:8080`
- Visualize tópicos
- Monitore mensagens
- Veja consumers

### Redis Insight
Acesse: `http://localhost:5540`
- Visualize dados em cache
- Monitore chaves
- Analise performance

## Tópicos Kafka

1. **transacao-topic** - Recebe transações para análise
2. **fraude-detectada-topic** - Publica resultados de análise

## Estrutura de Dados no Redis

```
transacao:conta:{contaId}              → Lista de transações recentes
transacao:duplicata:{dados}            → Controle de duplicatas
transacao:localizacao:{contaId}        → Última localização
transacao:localizacao:timestamp:{id}   → Timestamp da localização
conta:bloqueada:{contaId}              → Status de bloqueio
transacao:id:{transacaoId}             → Dados da transação individual
```

## Regras de Detecção

| Critério | Score | Descrição |
|----------|-------|-----------|
| Valor Alto | +30 | Acima do limite configurado |
| Velocidade Alta | +40 | Múltiplas transações rápidas |
| Transação Duplicada | +50 | Mesmas características em pouco tempo |
| Mudança Geográfica | +35 | Local diferente rapidamente |
| Horário Suspeito | +15 | Transações de madrugada (2h-5h) |

**Threshold de Fraude**: Score >= 50
**Bloqueio Automático**: Score >= 80

## Logs

A aplicação gera logs detalhados de todas as operações:
- Transações recebidas
- Análises realizadas
- Fraudes detectadas
- Contas bloqueadas

## Segurança

- Validação de dados de entrada
- Bloqueio automático de contas suspeitas
- Histórico de transações com TTL configurável
- Cache com expiração automática

