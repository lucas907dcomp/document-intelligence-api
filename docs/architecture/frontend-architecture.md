# Frontend Architecture — Document Intelligence UI

**Status:** Specification  
**Version:** 1.0.0  
**Author:** Aria (Architect)  
**Date:** 2026-05-04  
**Story:** DIS-002  

---

## 1. Overview

Frontend SPA (Single Page Application) que expõe a RAG pipeline do **Document Intelligence API** através de uma interface visual de alto impacto. O usuário faz upload de um PDF, acompanha o processamento em tempo real e interage com o documento via chat de linguagem natural.

### 1.1 Escopo

| Inclui | Exclui |
|--------|--------|
| Upload de PDF (drag-and-drop) | Autenticação/login |
| Polling de status em tempo real | Multi-documento simultâneo |
| Chat com respostas + source chunks | Histórico persistente |
| Feedback visual de estado (PROCESSING / READY / FAILED) | Configurações de RAG expostas ao usuário |
| Design system "Vibe Pointer" completo | Backend changes |

---

## 2. Tech Stack

| Camada | Tecnologia | Versão | Justificativa |
|--------|-----------|--------|---------------|
| Framework UI | React | 18.x | Ecossistema maduro, concurrent features |
| Linguagem | TypeScript | 5.x | Type safety com as DTOs da API |
| Build Tool | Vite | 5.x | HMR < 300ms, ESM nativo |
| Estilização | Tailwind CSS | 3.x | Utility-first, compatível com shadcn |
| Componentes Base | shadcn/ui | latest | Headless + acessível, customizável |
| Ícones | lucide-react | latest | Tree-shakeable, consistente |
| Animações | framer-motion | 11.x | API declarativa para animações fluidas |
| Server State | @tanstack/react-query | 5.x | Cache, polling, retry automáticos |
| HTTP Client | axios | 1.x | Interceptors, timeout, upload progress |
| Tipagem de Formulário | react-hook-form | 7.x | Performance + validação |

### 2.1 Por que React Query para polling?

O status de processamento requer polling de `GET /documents/{id}/status` até o estado ser `READY` ou `FAILED`. React Query tem `refetchInterval` nativo com invalidação automática na mudança de estado — elimina a necessidade de lógica manual com `setInterval`.

---

## 3. Design System — Vibe Pointer

### 3.1 Paleta de Cores

```css
/* Tokens Globais — definir em tailwind.config.ts */
:root {
  --color-bg:            #000000;     /* Fundo principal */
  --color-panel:         #09090b;     /* zinc-950 — painéis */
  --color-border:        #18181b;     /* zinc-900 — bordas sutis */
  --color-accent-mint:   #20EFA4;     /* Verde menta (acento primário) */
  --color-accent-cyan:   #00FFFF;     /* Ciano (acento secundário) */
  --color-text-primary:  #FFFFFF;     /* Títulos */
  --color-text-secondary:#d4d4d8;     /* zinc-300 — descrições */
  --color-text-muted:    #71717a;     /* zinc-500 — placeholders */
}
```

### 3.2 Gradientes

```css
/* Glow de fundo dos painéis */
.panel-glow {
  background: radial-gradient(
    ellipse 80% 50% at 50% 0%,
    rgba(32, 239, 164, 0.12) 0%,
    transparent 70%
  );
}

/* Borda gradiente (aplicada via pseudo-elemento ou wrapper) */
.gradient-border {
  background: linear-gradient(135deg, #20EFA4 0%, #00FFFF 100%);
  padding: 1px; /* espessura da borda */
  border-radius: inherit;
}

/* Gradiente de texto */
.gradient-text {
  background: linear-gradient(90deg, #20EFA4, #00FFFF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
```

### 3.3 Padrão Geométrico de Fundo

Pequenos quadrados/losangos em baixíssima opacidade cobrindo o fundo. Implementado como SVG inline ou CSS `background-image` com `background-size` repetindo:

```css
.geometric-bg {
  background-image: 
    linear-gradient(rgba(32, 239, 164, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(32, 239, 164, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}
```

### 3.4 Tipografia

| Elemento | Classe Tailwind |
|----------|----------------|
| Hero title | `text-5xl md:text-7xl font-bold tracking-tight text-white` |
| Hero subtitle | `text-lg text-zinc-300 max-w-lg` |
| Section label | `text-xs font-semibold tracking-widest text-zinc-500 uppercase` |
| Body | `text-sm text-zinc-300` |
| Code/chunk | `text-xs font-mono text-zinc-400` |

### 3.5 Componentes Base (shadcn/ui)

| Componente | Customização |
|-----------|-------------|
| Button (primary) | `bg-white text-black hover:bg-zinc-100 rounded-full` |
| Button (ghost) | `border border-zinc-800 text-white hover:border-zinc-600` |
| Input | `bg-zinc-900 border-zinc-800 text-white placeholder:text-zinc-500` |
| Badge (status) | Variante por estado (vide seção 5.3) |

---

## 4. Estrutura de Arquivos

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/                      # shadcn/ui (gerados via CLI)
│   │   │   ├── button.tsx
│   │   │   ├── input.tsx
│   │   │   └── badge.tsx
│   │   ├── layout/
│   │   │   ├── GeometricBackground.tsx   # Grid SVG de fundo
│   │   │   └── GradientBorder.tsx        # Wrapper de borda gradiente
│   │   ├── hero/
│   │   │   └── HeroSection.tsx           # Título, subtítulo, CTA
│   │   ├── upload/
│   │   │   ├── UploadPanel.tsx           # Painel principal drag-and-drop
│   │   │   ├── DropZone.tsx              # Área de drag-and-drop + validação
│   │   │   └── ProcessingStatus.tsx      # Badge + barra de progresso
│   │   └── chat/
│   │       ├── ChatInterface.tsx         # Container do chat
│   │       ├── MessageBubble.tsx         # Bolha usuário / bolha AI
│   │       ├── SourceChunks.tsx          # Accordion com trechos fonte
│   │       └── ChatInput.tsx             # Input + botão enviar
│   ├── hooks/
│   │   ├── useDocumentUpload.ts          # Mutation: POST /documents
│   │   ├── useDocumentStatus.ts          # Query com polling
│   │   └── useDocumentQuery.ts           # Mutation: POST /documents/{id}/query
│   ├── services/
│   │   └── api.ts                        # Axios instance + typed functions
│   ├── types/
│   │   └── api.types.ts                  # Espelha DTOs do backend
│   ├── lib/
│   │   └── utils.ts                      # cn() helper (clsx + twMerge)
│   ├── App.tsx                           # Orquestrador de estado global
│   ├── main.tsx                          # Entry point + QueryClientProvider
│   └── index.css                         # Tailwind directives + tokens CSS
├── public/
├── .env.example                          # VITE_API_BASE_URL=http://localhost:8080
├── vite.config.ts
├── tailwind.config.ts
├── tsconfig.json
└── package.json
```

---

## 5. Arquitetura de Componentes

### 5.1 Hierarquia

```
App
├── GeometricBackground          (posição: fixed, pointer-events: none)
├── HeroSection                  (acima do fold)
└── InteractionPanel             (painel zinc-950 com gradient-border)
    ├── [estado: idle | uploading]
    │   └── UploadPanel
    │       └── DropZone
    ├── [estado: processing]
    │   └── ProcessingStatus
    └── [estado: ready | failed]
        └── ChatInterface
            ├── MessageList
            │   └── MessageBubble[]
            │       └── SourceChunks (acordeão, visível só em mensagens AI)
            └── ChatInput
```

### 5.2 Máquina de Estados da App

```typescript
type AppState =
  | { phase: 'idle' }
  | { phase: 'uploading'; progress: number }
  | { phase: 'processing'; documentId: string }
  | { phase: 'ready'; documentId: string }
  | { phase: 'failed'; error: string; canRetry: boolean }

// Transições:
// idle        → uploading   (usuário faz drop/seleção do PDF)
// uploading   → processing  (POST /documents retorna 202 + documentId)
// processing  → ready       (GET /documents/{id}/status retorna READY)
// processing  → failed      (GET retorna FAILED ou timeout de 5 min)
// failed      → idle        (usuário clica em "Try Again")
// ready       → idle        (usuário clica em "Upload New Document")
```

### 5.3 Badges de Status

| Estado Backend | Texto UI | Cor | Ícone (lucide) |
|---------------|---------|-----|----------------|
| `PROCESSING` | "Processing…" | `text-yellow-400` | `Loader2` (spin) |
| `READY` | "Ready" | `text-#20EFA4` | `CheckCircle2` |
| `FAILED` | "Failed" | `text-red-400` | `XCircle` |

---

## 6. Integração com a API

### 6.1 Tipos TypeScript (espelhando DTOs do backend)

```typescript
// src/types/api.types.ts

export interface UploadResponse {
  documentId: string; // UUID
}

export type DocumentStatus = 'PROCESSING' | 'READY' | 'FAILED';

export interface StatusResponse {
  documentId: string;
  status: DocumentStatus;
  filename: string;
}

export interface QueryRequest {
  question: string;
}

export interface QueryResponse {
  answer: string;
  sourceChunks: string[];
}

// ProblemDetail (RFC 7807) — formato de erro do backend
export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
}
```

### 6.2 Camada de Serviço

```typescript
// src/services/api.ts

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  timeout: 30_000,
});

export const uploadDocument = (file: File, onProgress?: (pct: number) => void) =>
  api.post<UploadResponse>('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => onProgress?.(Math.round((e.loaded * 100) / (e.total ?? 1))),
  });

export const getDocumentStatus = (id: string) =>
  api.get<StatusResponse>(`/documents/${id}/status`);

export const queryDocument = (id: string, question: string) =>
  api.post<QueryResponse>(`/documents/${id}/query`, { question });
```

### 6.3 Hooks React Query

```typescript
// useDocumentStatus.ts — polling com stop automático
export const useDocumentStatus = (documentId: string | null) =>
  useQuery({
    queryKey: ['document-status', documentId],
    queryFn: () => getDocumentStatus(documentId!),
    enabled: !!documentId,
    refetchInterval: (query) => {
      const status = query.state.data?.data.status;
      return status === 'PROCESSING' ? 2000 : false; // para de postar quando termina
    },
    select: (res) => res.data,
  });
```

### 6.4 Tratamento de Erros

| Código HTTP | Origem | Mensagem UI |
|------------|--------|-------------|
| 400 | Arquivo inválido (não é PDF) | "Please upload a valid PDF file." |
| 413 | Arquivo > 10MB | "File too large. Maximum size is 10MB." |
| 409 | Query em documento não READY | (não ocorre, UI bloqueia) |
| 502 | OpenAI indisponível | "AI service unavailable. Please try again." |
| 503 | Kafka indisponível | "Service temporarily unavailable." |
| Network | Timeout / sem conexão | "Connection failed. Check your network." |

---

## 7. Animações (framer-motion)

### 7.1 Variantes Globais

```typescript
// src/lib/animations.ts

export const fadeInUp = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

export const fadeIn = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: 0.4 } },
};

export const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.1 } },
};

export const scaleIn = {
  hidden: { opacity: 0, scale: 0.95 },
  visible: { opacity: 1, scale: 1, transition: { duration: 0.3, ease: 'easeOut' } },
};
```

### 7.2 Aplicação por Componente

| Componente | Variante | Trigger |
|-----------|---------|---------|
| `HeroSection` (título) | `fadeInUp` | `initial="hidden" animate="visible"` |
| `HeroSection` (subtítulo) | `fadeInUp` com `delay: 0.15` | mesmo |
| `InteractionPanel` | `scaleIn` | mount |
| `MessageBubble` | `fadeInUp` | ao ser adicionado à lista |
| `SourceChunks` (accordion) | `fadeIn` | on open |
| `ProcessingStatus` | `fadeIn` | transição de estado |

---

## 8. Configuração do Vite

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    proxy: {
      '/documents': 'http://localhost:8080', // evita CORS em dev
    },
  },
});
```

### 8.1 CORS

Em desenvolvimento, o proxy Vite elimina CORS. Em produção, o backend deve incluir o origin do frontend no `CorsConfig.java` (Spring). O dev deve adicionar `VITE_API_BASE_URL` ao `.env` e configurar o Spring CORS correspondente.

---

## 9. Tailwind Config

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss';

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'accent-mint': '#20EFA4',
        'accent-cyan': '#00FFFF',
      },
      backgroundImage: {
        'gradient-accent': 'linear-gradient(135deg, #20EFA4 0%, #00FFFF 100%)',
        'glow-accent':
          'radial-gradient(ellipse 80% 50% at 50% 0%, rgba(32,239,164,0.12), transparent)',
      },
      animation: {
        'spin-slow': 'spin 3s linear infinite',
      },
    },
  },
  plugins: [],
} satisfies Config;
```

---

## 10. Variáveis de Ambiente

```bash
# frontend/.env.example
VITE_API_BASE_URL=http://localhost:8080
```

O proxy Vite (`/documents → localhost:8080`) é suficiente para desenvolvimento local. `VITE_API_BASE_URL` é usado apenas em produção/staging.

---

## 11. Decisões Arquiteturais

### ADR-FE-001 — React Query em vez de Redux para estado de servidor

**Decisão:** Usar `@tanstack/react-query` para todo estado derivado da API (upload, status, chat).  
**Motivo:** O estado da UI é majoritariamente *server state* (assíncrono, cache, refetch). Redux adicionaria boilerplate sem benefício real para este escopo.  
**Consequência:** App.tsx mantém apenas estado local mínimo (`AppState` phase machine).

### ADR-FE-002 — Proxy Vite em vez de CORS no backend durante dev

**Decisão:** Configurar `server.proxy` no Vite para apontar `/documents` ao backend.  
**Motivo:** Evita modificar o `CorsConfig.java` do backend para cada ambiente de dev. Em produção, o frontend e o backend provavelmente estarão em domínios distintos e o CORS será configurado explicitamente.

### ADR-FE-003 — shadcn/ui como base de componentes

**Decisão:** Usar shadcn/ui (não um component library compilada como MUI/Chakra).  
**Motivo:** shadcn/ui gera código no projeto (não é dependência black-box), facilitando customização do design system Vibe Pointer.

### ADR-FE-004 — Frontend como app separado (não acoplado ao backend)

**Decisão:** Pasta `frontend/` na raiz do repositório, com seu próprio `package.json`.  
**Motivo:** Separação de concerns. O backend é um JAR deployável independentemente. O frontend pode ser servido via CDN/Nginx.

---

## 12. Restrições e Limitações Herdadas da API

| Limitação | Impacto no Frontend |
|-----------|-------------------|
| Máx 10MB por PDF | Validar no DropZone antes de upload |
| Apenas PDFs | Validar extensão + MIME type no DropZone |
| Single-document: uma sessão = um documento | "Upload New Document" reseta o estado completo |
| Não há WebSocket/SSE | Polling a cada 2s (React Query `refetchInterval`) |
| Sem autenticação na API | Sem fluxo de login no frontend |

---

## 13. Referências

- API: `http://localhost:8080/swagger-ui.html`
- Backend DTOs: `src/main/java/com/example/documentintelligence/api/dto/`
- ADR do backend: `docs/architecture/ADR-001-claim-check-pattern.md`
- Story de implementação: `docs/stories/DIS-002.story.md`
