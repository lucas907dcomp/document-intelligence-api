# AI-HANDOFF.md — Memória Arquitetural do Projeto

> **Para o agente de IA que abrir este repositório:** Este documento é sua memória persistente. Leia-o antes de qualquer intervenção técnica. Ele contém o contexto completo do que foi construído, as decisões tomadas e — mais importante — os bugs críticos que já foram resolvidos para que você não os reproduza.

---

## 1. Identidade do Projeto

**Document Intelligence** é uma aplicação Full Stack de RAG (Retrieval-Augmented Generation) que permite ao usuário fazer upload de PDFs pela interface web e consultar seu conteúdo via linguagem natural.

- **Repositório local:** `C:\Users\lucas.damaceno\document-intelligence-api`
- **Branch principal:** `master`
- **Última sessão de trabalho:** 2026-05-04
- **Estado ao encerrar:** Stack Docker 100% funcional. Pipeline RAG testado end-to-end com IRS W-9 (140KB, 48 chunks, READY + query respondida com precisão).

---

## 2. Stack Completa

### Backend
| Componente | Tecnologia | Detalhe |
|---|---|---|
| Framework | Spring Boot 3.3.5 + Java 21 | |
| IA / LLM | Spring AI 1.0.0 + OpenAI | `gpt-4o-mini`, temperatura `0.5` |
| Embeddings | OpenAI `text-embedding-3-small` | 1536 dimensões |
| Mensageria | Spring Kafka + Confluent 7.4.0 | Ack manual, DLQ após 2 retries |
| Banco | PostgreSQL 15 + pgvector 0.1.6 | IVFFlat cosine, `vector(1536)` |
| Migrations | Flyway | V1 documents, V2 chunks, V3 embeddings+pgvector |
| PDF | Apache PDFBox 3.0.3 | |
| Build | Maven, Dockerfile multi-stage | `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine` |

### Frontend
| Componente | Tecnologia | Detalhe |
|---|---|---|
| Framework | React 18 + TypeScript | |
| Build | Vite 5 | Proxy `/documents` → `localhost:8080` |
| Estilos | Tailwind CSS 3 | Design system Vibe Pointer |
| Componentes | shadcn/ui + Radix UI | |
| Animações | framer-motion 11 | |
| Estado servidor | TanStack Query 5 | Polling automático |
| HTTP | Axios 1.7 | |

### Infraestrutura Docker
| Container | Imagem | Porta |
|---|---|---|
| `document-intelligence-postgres` | `pgvector/pgvector:pg15` | `5432` |
| `document-intelligence-kafka` | `confluentinc/cp-kafka:7.4.0` | `9092` |
| `document-intelligence-zookeeper` | `confluentinc/cp-zookeeper:7.4.0` | `2181` |
| `document-intelligence-app` | Build local (Dockerfile) | `8080` |

---

## 3. Arquitetura do Frontend (Vibe Pointer)

### Design System
- **Fundo:** `zinc-950` (`#09090b`) com grid geométrico via `background-image` CSS
- **Acentos:** gradiente mint green `#20EFA4` → cyan `#00FFFF` (bordas, botões, ícones ativos)
- **Painéis:** `zinc-900` com borda `zinc-800`
- **Tipografia:** `font-mono` para IDs e status; `font-sans` para corpo
- **Animações:** framer-motion — `fadeInUp`, `scaleIn`, `slideInLeft` com `staggerChildren`

### State Machine (`App.tsx`)
A aplicação é uma máquina de estados explícita. Não há lógica de estado espalhada — toda transição passa por `setPhase()`:

```
idle
  └─(upload)─→ uploading
                 └─(202 OK)─→ processing ──(polling 2s)──┬─(READY)──→ ready
                               └─(erro HTTP)─→ failed    └─(FAILED)─→ failed
ready
  └─(nova pergunta)─→ ready (reutiliza documentId)
  └─(novo upload)──→ idle (reset completo)
```

### Polling com TanStack Query (`useDocumentStatus.ts`)
```typescript
refetchInterval: (query) => {
  const status = query.state.data?.data.status
  return status === 'PROCESSING' ? 2000 : false
}
```
O polling inicia automaticamente quando `documentId` é definido, dispara a cada 2 segundos e **para sozinho** quando o status muda para `READY` ou `FAILED`. Nenhum `clearInterval` manual necessário.

### Proxy Vite (`vite.config.ts`)
```typescript
server: { proxy: { '/documents': 'http://localhost:8080' } }
```
Em desenvolvimento, todas as requisições `/documents/*` são proxiadas para o backend. Sem CORS, sem configuração extra no Spring.

### Arquivos principais do frontend
```
frontend/src/
├── App.tsx                    # State machine + QueryClientProvider
├── components/
│   ├── layout/GeometricBackground.tsx
│   ├── hero/HeroSection.tsx
│   ├── upload/UploadZone.tsx  # Drag & drop + progress bar
│   └── chat/ChatPanel.tsx     # Pergunta + resposta + sourceChunks colapsáveis
├── hooks/
│   ├── useDocumentUpload.ts   # useMutation → POST /documents
│   ├── useDocumentStatus.ts   # useQuery com polling condicional
│   └── useDocumentQuery.ts    # useMutation → POST /documents/{id}/query
└── services/api.ts            # Axios client base URL '/'
```

---

## 4. Bugs Críticos Resolvidos (Não Reproduzir)

### BUG-001 — Docker Volume: Permission Denied no Upload (HTTP 503)

**Sintoma:** Upload retorna `503 Service Unavailable` com mensagem "Serviço temporariamente indisponível".

**Causa raiz:** O `Dockerfile` original criava um usuário não-root (`appuser`) e rodava o container com ele. O volume Docker nomeado `pdf-storage` é criado pelo daemon Docker como `root:root` com permissão `drwxr-xr-x`. Volumes são montados **após** as layers da imagem — qualquer `RUN chown` no Dockerfile é ineficaz. O `appuser` não tinha permissão de escrita.

**Diagnóstico confirmado por:**
```bash
docker exec document-intelligence-app id
# uid=1001(appuser) — não-root

docker exec document-intelligence-app ls -la /app/pdf-storage
# drwxr-xr-x  root root — sem write para outros

docker exec document-intelligence-app touch /app/pdf-storage/test.txt
# touch: /app/pdf-storage/test.txt: Permission denied
```

**Fix aplicado (`Dockerfile`):** Removidas todas as linhas de criação de usuário não-root. Container agora roda como `root`. Volumes Docker com root são a norma em ambientes de desenvolvimento controlado.

```dockerfile
# REMOVIDO — não fazer:
# RUN addgroup -S appgroup && adduser -S appuser -G appgroup
# RUN chown -R appuser:appgroup /app
# USER appuser

# MANTIDO:
RUN mkdir -p /app/pdf-storage
```

---

### BUG-002 — UUID Mismatch: ID da Resposta HTTP ≠ ID no Banco (404 no Status)

**Sintoma:** Upload retorna `202` com um `documentId`. Polling imediato do status retorna `404 Not Found`.

**Causa raiz:** A entidade `Document` tinha `@GeneratedValue(strategy = GenerationType.UUID)` no campo `id`. O `DocumentService.upload()` gerava um UUID manualmente com `UUID.randomUUID()`, atribuía ao entity builder e chamava `documentRepository.save()`. O Hibernate 6 com `GenerationType.UUID` **ignora o valor pré-definido** e gera um novo UUID próprio ao persistir — resultando em IDs diferentes entre:
- O `documentId` retornado na resposta HTTP
- O `documentId` publicado no evento Kafka
- O `id` efetivamente salvo no banco PostgreSQL

**Diagnóstico confirmado por:**
```
Log: "Document uploaded documentId=fe8425ad-..."   ← UUID gerado pela app
DB:  id=989cb56f-...                               ← UUID gerado pelo Hibernate
```

**Fix aplicado (`Document.java`):**
```java
// ANTES — BUGADO:
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;

// DEPOIS — CORRETO:
@Id
private UUID id;  // App sempre define o UUID via builder, @GeneratedValue removido
```

**ADR derivado:** ADR-005 — UUID deve ser gerado pela aplicação (`UUID.randomUUID()` no service), não pelo framework ORM, para garantir consistência entre HTTP response, evento Kafka e registro no banco.

---

### BUG-003 — pgvector Bytea Mismatch: Embedding FAILED com `bytea` em coluna `vector`

**Sintoma:** PDFs com texto são processados (chunks extraídos, embeddings gerados pela OpenAI), mas o status final é `FAILED`. Log mostra:
```
ERROR: column "vector" is of type vector but expression is of type bytea
```

**Causa raiz:** A entidade `Embedding` usava:
```java
@JdbcTypeCode(SqlTypes.VECTOR)
@Column(name = "vector", columnDefinition = "vector(1536)", nullable = false)
private float[] vector;
```

O `@JdbcTypeCode(SqlTypes.VECTOR)` foi introduzido no Hibernate 6.4 e teoricamente instrui o Hibernate a usar o tipo pgvector. Na prática, sem configuração adicional do driver (`PGvector.addVectorType(connection)` por conexão), o Hibernate 6.5 envia o `float[]` como `bytea` (serialização Java nativa) ao invés de formatar como vetor pgvector `[x,y,z,...]`. O PostgreSQL rejeita a inserção.

**Fix aplicado:** Substituído `embeddingRepository.save(Embedding entity)` por método nativo em `EmbeddingRepository`:

```java
// EmbeddingRepository.java — MÉTODO ADICIONADO:
@Modifying
@Transactional
@Query(value = "INSERT INTO embeddings (id, chunk_id, vector) " +
               "VALUES (gen_random_uuid(), :chunkId, CAST(:vector AS vector))",
       nativeQuery = true)
void insertEmbedding(@Param("chunkId") UUID chunkId, @Param("vector") String vector);
```

```java
// DocumentEventConsumer.java — SUBSTITUIÇÃO:
// ANTES (bugado):
embeddingRepository.save(Embedding.builder().chunk(chunk).vector(vector).build());

// DEPOIS (correto):
embeddingRepository.insertEmbedding(chunk.getId(), toVectorString(vector));

// Helper:
private static String toVectorString(float[] vector) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
        if (i > 0) sb.append(',');
        sb.append(vector[i]);
    }
    return sb.append(']').toString();
}
```

O `CAST(:vector AS vector)` é consistente com o operador `<=>` já usado na busca:
```sql
ORDER BY e.vector <=> CAST(:queryVector AS vector)
```

**ADR derivado:** ADR-006 — Toda interação com a coluna `vector` deve usar SQL nativo com `CAST(... AS vector)`. Nunca usar `JPA save()` para entidades com colunas pgvector sem configuração explícita do driver.

---

## 5. Fluxo de Ordem Crítica no Upload (CRIT-1)

```
upload(file):
  1. store(file)           → grava PDF em /app/pdf-storage/{uuid}/{filename}
  2. kafka.publish(event)  → se falhar: delete(filePath), throw 503
  3. db.save(document)     → persiste PROCESSING com o mesmo uuid
  4. return uuid
```

Esta ordem garante: nunca há registro no banco sem arquivo no storage e evento publicado. Kafka é publicado antes do banco porque é o ponto mais provável de falha — assim o rollback (delete do arquivo) é simples.

---

## 6. Configuração de IA (Ajustável sem Rebuild)

Arquivo: `src/main/resources/application.yml`

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o-mini    # gpt-4o para mais qualidade (mais caro)
          temperature: 0.5      # 0.0=determinístico, 0.7=criativo, 1.0=máximo
      embedding:
        options:
          model: text-embedding-3-small  # ATENÇÃO: trocar muda dimensão do vetor
```

**Atenção ao trocar modelo de embedding:** `text-embedding-3-small` = 1536 dims. Se trocar para `text-embedding-3-large` (3072 dims) ou `ada-002` (1536 dims), deve verificar o migration `V3__create_embeddings_pgvector.sql` e o `columnDefinition = "vector(1536)"` na entidade `Embedding.java`.

---

## 7. Comandos Operacionais

```bash
# Subir toda a stack
docker compose up --build -d

# Rebuild apenas do app (após mudança de código)
docker compose build app && docker compose up -d app

# Ver logs do app em tempo real
docker logs document-intelligence-app -f

# Testar saúde
curl http://localhost:8080/actuator/health

# Frontend (terminal separado)
cd frontend && npm run dev

# Parar tudo
docker compose down

# Destruir dados (volumes)
docker compose down -v
```

---

## 8. Estado dos Testes

| Suite | Qtd | Observação |
|---|---|---|
| Unitários | 33 | Sem Docker, MockitoExtension |
| Integração HTTP | 1 | Testcontainers |
| Integração Kafka | 1 | Testcontainers — usa `DocumentEventProducer.TOPIC` (public) |
| pgvector search | 2 | @DataJpaTest com Testcontainers pgvector |

**Nota:** `DocumentEventProducer.TOPIC` é `public static final` (corrigido de package-private) para ser acessível nos testes de integração que ficam em pacote diferente.

---

## 9. Autoridade Arquitetural — Princípios para Continuar

1. **Não adicionar `@GeneratedValue` a nenhuma entidade** cujo ID seja gerado pela camada de serviço. O UUID deve ser único e consistente entre response/Kafka/DB.

2. **Toda interação com colunas `vector` do pgvector via JPA deve usar queries nativas** com `CAST(... AS vector)`. Não usar `save()` de entidade com campo `float[]` para pgvector.

3. **O Dockerfile não deve criar usuário não-root** enquanto usar volumes Docker nomeados. Se segurança exigir usuário não-root em produção, usar `docker run --user` ou Kubernetes `securityContext`, não `USER` no Dockerfile.

4. **O frontend é um SPA estático em desenvolvimento** — o `npm run dev` do Vite é o servidor de dev. Em produção, o `frontend/dist/` pode ser servido pelo Nginx ou copiado para o container Spring Boot como `static/`.

5. **O system prompt do RAG é restritivo por design** (`Answer exclusively from the context below`). O modelo responderá "não sei" para perguntas fora do documento — isso é correto, não é um bug de inteligência.

---

*Documento gerado em 2026-05-04 pela Aria (Architect Agent — AIOX). Atualizar após cada sessão de trabalho que modifique arquitetura, entidades, ou resolva bugs críticos.*
