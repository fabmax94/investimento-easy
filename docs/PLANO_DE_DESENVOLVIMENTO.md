# Plano de desenvolvimento — Monitor de Carteira (Android)

> Fonte dos requisitos: documento **"Monitor de Carteira — Regras de negócio e estratégia"** (regras R1–R24, motor de análise, alertas, modelo de dados, protótipo de 6 telas e roadmap em 4 fases).
> Este plano traduz esse documento para um **app Android nativo** e o quebra em tasks pequenas, testáveis e entregáveis de forma incremental.

---

## 1. Decisões de arquitetura (propostas)

O documento sugere React Native + backend. Como o pedido é um **app Android**, a proposta é nativa e *local-first* no MVP, mantendo a porta aberta para um backend depois.

| Tema | Decisão | Por quê |
|---|---|---|
| Linguagem / UI | Kotlin 2.x + Jetpack Compose + Material 3 | Padrão atual do Android; UI declarativa e testável |
| Arquitetura | Clean Architecture + MVVM/UDF (estado imutável, eventos) | Regras de negócio isoladas e testáveis sem Android |
| Modularização | Multi-módulo por camada e feature (ver §2) | Build incremental, fronteiras explícitas, testes rápidos |
| DI | Hilt | Integração nativa com ViewModel, WorkManager e testes |
| Persistência | Room (+ SQLCipher) e DataStore | R7/LGPD: base criptografada em repouso |
| Arquivos originais | Armazenamento interno criptografado (Jetpack Security / Android Keystore) | R7: original guardado só para reprocessamento |
| Rede | Retrofit + OkHttp + kotlinx.serialization | Fontes públicas (B3, CVM, Tesouro, BCB) e API do Claude |
| Agendamento | WorkManager (job diário após 20h, dias úteis) | R13; sobrevive a reboot e respeita bateria |
| Notificações | NotificationManager local (push local gerado pelo job) | Sem backend no MVP; FCM só se houver servidor |
| Dinheiro | `BigDecimal` encapsulado em value class `Money` (nunca `Double`) | Precisão e arredondamento explícitos |
| Datas | `java.time` / kotlinx-datetime + calendário de dias úteis B3 | R13, TWR, vencimentos |
| PDF | PdfBox-Android (extração de texto por página) | Parser determinístico do XPerformance (R2) |
| XLSX | Leitor próprio mínimo (xlsx = zip + XML) ou fastexcel-reader | Apache POI é pesado demais para Android |
| IA | API do Claude com saída JSON validada por esquema | Camada 3; o Claude nunca calcula |
| Chave da API | MVP pessoal: chave informada pelo usuário e guardada no Keystore. Multiusuário: proxy no backend | **Depende da pergunta em aberto do doc** (§6) |

Princípios transversais que viram código (não só documentação):
- **Regra zero:** todo valor numérico é um `Sourced<T>` com `origem ∈ {RELATORIO, MERCADO, CALCULADO, ESTIMADO, IA}`; ausência é `null`, nunca interpolação.
- **Snapshot é âncora, acompanhamento é estimativa:** tipos distintos (`SnapshotValue` × `EstimatedValue`) para o compilador impedir mistura.
- **Sem previsão de preço:** os modelos de saída simplesmente não têm campo de tendência/preço-alvo.

---

## 2. Estrutura de módulos

```
:app                         → Activity, navegação, DI raiz
:build-logic                 → convention plugins (android, compose, jvm, test, quality)
:core:model                  → entidades puras (Kotlin/JVM): Ativo, Posicao, Snapshot, Money, Sourced…
:core:domain                 → casos de uso e regras R1–R24 (Kotlin/JVM, 100% testável na JVM)
:core:analytics              → Camada 1: alocação, Ritmo, Cenário, FGC, FII, TWR (Kotlin/JVM)
:core:alerts                 → Camada 2: motor de regras de alerta e ciclo de vida (Kotlin/JVM)
:core:database               → Room + SQLCipher, DAOs, migrações
:core:datastore              → perfil, limites configuráveis (R23), preferências
:core:network                → clientes HTTP, fontes de mercado plugáveis, cache diário
:core:ai                     → cliente Claude, prompts versionados, validador de números
:core:designsystem           → tema "Castanha", componentes, tipografia, formatação R$/%
:core:testing                → fakes, builders de fixtures, regras de teste, relógio fixo
:parser:xperformance         → parser PDF XP (Kotlin/JVM sobre texto extraído)
:parser:xlsx                 → parser .xlsx de posição XP
:parser:generic              → extração genérica via Claude (R2 reserva)
:feature:portfolio           → Tela 1 Carteira
:feature:upload              → Tela 2 Enviar carteira
:feature:review              → Tela 3 Revisar extração
:feature:changes             → Tela 4 O que mudou
:feature:analysis            → Tela 5 Análise (7 abas)
:feature:alerts              → Tela 6 Alertas
:feature:onboarding          → perfil e alocação-alvo
:sync                        → Workers do WorkManager (remarcação, resumo semanal)
```

Regra de dependência: `feature → domain → model`; `data (database/network/ai) → domain` via interfaces (inversão de dependência). Módulos `core:model`, `core:domain`, `core:analytics`, `core:alerts` e os parsers **não dependem do Android** — rodam na JVM pura, rápidos e fáceis de testar.

---

## 3. Estratégia de testes e qualidade

**Pirâmide de testes**

| Nível | Ferramentas | Onde | Meta |
|---|---|---|---|
| Unitário | JUnit 5, Kotest assertions, MockK, Turbine (Flow) | domain, analytics, alerts, parsers, ViewModels | ≥ 90% de linhas nos módulos de regra |
| Propriedade | Kotest property testing | `Money`, TWR, soma confere (R3), reconciliação | Invariantes (ex.: soma de alocação = 100%) |
| Golden files | Fixtures anonimizadas de PDF/XLSX + JSON esperado | parsers | Qualquer mudança de parse é visível no diff |
| Integração | Room in-memory, MockWebServer, WorkManager TestDriver | database, network, sync | Contratos das fontes e migrações |
| UI | Compose UI Test + Robolectric | features | Estados: carregando, vazio, erro, conteúdo |
| Screenshot | Roborazzi (JVM) | designsystem e telas | Regressão visual, claro/escuro, fonte grande |
| E2E | Compose + Hilt test em emulador (CI) | fluxo upload → revisar → confirmar → análise | Caminho feliz das 5 telas do protótipo |
| Avaliação de IA | Conjunto de casos + validador de números | core:ai | 0 números sem origem; esquema sempre válido |

**Práticas**
- TDD nas regras de negócio: cada regra Rn tem testes nomeados com o identificador (`R3_bloqueia_confirmacao_quando_divergencia_maior_que_meio_por_cento`).
- Relógio injetável (`Clock`) e calendário B3 injetável — nada de `now()` escondido.
- Fixtures reais **anonimizadas** (sem conta/assessor — R7); nunca commitar dado pessoal.
- Qualidade estática: ktlint (formatação), detekt (complexidade/code smells), Android Lint com `warningsAsErrors`, Kover com limite mínimo de cobertura por módulo.
- CI (GitHub Actions): build + lint + detekt + testes JVM + Roborazzi verify + Kover em todo PR; testes instrumentados em emulador no merge para `main`.
- Dependências: version catalog (`libs.versions.toml`) + Renovate/Dependabot.
- Segurança: `allowBackup=false`, sem logs de dado financeiro, R8/ProGuard em release, network security config só HTTPS.
- Acessibilidade: content descriptions, contraste, alvo mínimo 48dp, testado nos screenshots com fonte 200%.
- Convenção: Conventional Commits, PRs pequenos (1 task ≈ 1 PR), ADRs curtos em `docs/adr/` para decisões relevantes.

**Definition of Done de cada task**
1. Código + testes do nível adequado passando localmente e no CI.
2. ktlint, detekt e lint sem avisos novos; cobertura do módulo não cai.
3. Regras Rn envolvidas referenciadas no código/testes.
4. Nenhum número sem origem; nenhum dado sensível em log ou fixture.
5. PR revisado com descrição do que foi feito e como testar.

---

## 4. Tasks

Legenda de tamanho: **P** ≤ 1 dia · **M** 2–3 dias · **G** 4–5 dias. Dependências entre colchetes.

### Fase 0 — Fundação (antes de qualquer feature)

| # | Task | Tam. | Entregável / critério de aceite |
|---|---|---|---|
| T0.1 | Setup do repositório: Gradle wrapper, version catalog, `build-logic` com convention plugins, módulos vazios de §2 | M | `./gradlew build` verde com todos os módulos |
| T0.2 | Qualidade: ktlint, detekt, Android Lint, Kover com limites; `./gradlew check` agrega tudo | P [T0.1] | `check` falha em violação proposital |
| T0.3 | CI GitHub Actions (build, check, testes JVM, cache Gradle) + template de PR | P [T0.2] | Pipeline verde no primeiro PR |
| T0.4 | Script de setup do ambiente (Android SDK/cmdline-tools) para CI e sessões remotas | P [T0.1] | Build Android roda do zero num container limpo |
| T0.5 | `core:testing`: relógio fixo, dispatcher de teste, builders de fixtures (Ativo, Posição, Snapshot) | P [T0.1] | Usado pelos testes das tasks seguintes |
| T0.6 | `core:designsystem`: tema Castanha (cores, tipografia, claro/escuro), formatadores R$/%/pp/data, componentes base (card, chip de origem, badge de severidade, estado vazio/erro) + Roborazzi | M [T0.1] | Catálogo de componentes com screenshot tests |
| T0.7 | ADR-001 (arquitetura), ADR-002 (local-first e chave da API), ADR-003 (dinheiro e datas) | P | Documentos em `docs/adr/` |

### Fase 1 — Upload e análise (MVP de valor no primeiro upload)

**Domínio e dados**

| # | Task | Tam. | Regras | Critério de aceite |
|---|---|---|---|---|
| T1.1 | `core:model`: `Money`, `Percent`, `Sourced<T>`/`Origem`, `Ativo`, `ChaveAtivo`, `ClasseAtivo`, `Posicao`, `Snapshot`, `Carteira`, `Perfil` | M [T0.5] | Regra zero, R4 | Testes de propriedade de `Money`; chave R4 por tipo de ativo |
| T1.2 | Classificação automática de ativos (classe, gestora, emissor, indexador) | M [T1.1] | R5 | Tabela de casos cobrindo todas as classes |
| T1.3 | Validações de extração: data obrigatória, soma confere (0,5%), inconsistências plausíveis, mascaramento | M [T1.1] | R1, R3, R6, R7 | Cada regra com testes de borda (0,49% / 0,51%, etc.) |
| T1.4 | `core:database`: Room + SQLCipher, entidades Carteira/Snapshot/Posição/Ativo/Análise, snapshot imutável após confirmado, migrações testadas | G [T1.1] | R8, R16 | Testes de DAO e de imutabilidade; teste de migração |
| T1.5 | Armazenamento criptografado do arquivo original (Keystore) | P [T0.1] | R7 | Teste instrumentado: arquivo em disco não é legível em claro |
| T1.6 | Caso de uso `ConfirmarSnapshot` + regra "só o mais recente vira base" | M [T1.3, T1.4] | R8, R15, R16 | Mais novo substitui; mais antigo vai ao histórico; mesma data pergunta |

**Parsers**

| # | Task | Tam. | Regras | Critério de aceite |
|---|---|---|---|---|
| T1.7 | Obter e anonimizar fixtures reais (PDF XPerformance e .xlsx XP) | P | R7 | Fixtures sem conta/assessor, versionadas em `src/test/resources` |
| T1.8 | Extração de texto de PDF por página (PdfBox-Android) atrás de interface `PdfTextSource` | M [T0.1] | R2 | Parser testável na JVM com texto pré-extraído |
| T1.9 | Parser XPerformance: p.2 patrimônio, p.3 série mensal, p.4 composição, p.6–9 posições, p.10 movimentações | G [T1.7, T1.8] | R2, R4 | Golden tests; soma confere nas fixtures |
| T1.10 | Parser .xlsx XP por aba (preço médio, taxa, vencimento RF, proventos) | G [T1.7] | R2, R4 | Golden tests |
| T1.11 | Detecção de formato + orquestrador de extração (parser determinístico → reserva IA) | P [T1.9, T1.10] | R2 | Arquivo desconhecido cai na reserva com campos marcados "extraído por IA" |

**Camada 1 — cálculos**

| # | Task | Tam. | Critério de aceite |
|---|---|---|---|
| T1.12 | Alocação por classe, gestora, emissor, geografia, indexador | M [T1.2] | Soma = 100% (propriedade) |
| T1.13 | Ritmo (ano anualizado − 24M anualizado; ▲ > +3pp, → entre, ▼ < −3pp) e Cenário (tabela classe → driver) | M [T1.1] | Testes nos limites exatos ±3pp |
| T1.14 | Recortes: concentração por gestora, sobreposição de exposição, composição interna de FIIs, FGC por emissor, posições simbólicas (< R$ 500), vencimentos, desvio do alvo | G [T1.12] | Um teste por recorte com carteira de fixture |
| T1.15 | Indicadores de FII: P/VP, DY 12M × rendimento corrente anualizado, upside até VP | M [T1.1] | Dados ausentes resultam em vazio, não zero |

**Camada 3 — IA**

| # | Task | Tam. | Critério de aceite |
|---|---|---|---|
| T1.16 | `core:ai`: cliente da API do Claude, timeouts/retry, guarda da chave no Keystore | M [T0.1] | MockWebServer cobre sucesso, 429, 5xx, timeout |
| T1.17 | Prompt de sistema versionado a partir da skill `portfolio-analysis` + montagem do contexto (snapshot + camadas 1/2 + mercado + perfil + análise anterior), sem dado identificável | M [T1.14] | Snapshot test do payload; nenhum campo sensível |
| T1.18 | Esquema JSON das 7 abas (Mercado, Alocação, Fundos, FIIs, Ações e ETFs, Alertas, O que fazer) + desserialização estrita | M [T1.16] | Sem campo de tendência/preço-alvo por construção |
| T1.19 | **Validador de números**: todo número citado precisa existir na entrada com origem; sugestão de compra aponta lacuna e valor em R$; venda tem destino e respeita travas | G [T1.18] | Análise inválida é rejeitada e refeita (máx. N tentativas) |
| T1.20 | Extração genérica via IA para formato desconhecido (esquema fixo) | M [T1.16, T1.11] | Todos os campos com origem IA até confirmação |

**Telas**

| # | Task | Tam. | Regras | Critério de aceite |
|---|---|---|---|---|
| T1.21 | Navegação (Compose Navigation type-safe) e shell do app | P [T0.6] | — | Rotas das 6 telas + onboarding |
| T1.22 | Tela 2 — Enviar carteira: seletor de arquivo (SAF), etapas da leitura, erros | M [T1.11] | R1, R2, R3, R7 | UI tests por estado; screenshot |
| T1.23 | Tela 3 — Revisar extração: resumo, posições por classe, itens a conferir, edição de campo, bloqueio de confirmação por divergência | G [T1.6, T1.22] | R3, R6, R8 | Botão Confirmar desabilitado > 0,5% até aceite explícito |
| T1.24 | Onboarding de perfil: alocação-alvo, colchão, objetivo (opcional) | M [T1.21] | R23 | Perfil ausente usa regras de concentração padrão |
| T1.25 | Tela 5 — Análise: 7 abas, veredicto, plano de 30 dias, Ritmo/Cenário, chip de origem em cada número, disclaimer | G [T1.19] | Regra zero | Screenshot por aba; disclaimer sempre presente |
| T1.26 | E2E Fase 1: upload → revisar → confirmar → análise, com fake da IA | M [T1.25] | — | Teste instrumentado verde no CI |

**Critério de saída da Fase 1 (do documento):** a análise do app bate com a gerada hoje pela skill no Claude, com o mesmo arquivo, e passa no validador de números.

### Fase 2 — Acompanhamento diário

| # | Task | Tam. | Regras | Critério de aceite |
|---|---|---|---|---|
| T2.1 | **Spike:** confirmar fontes — API de cotações B3 (preço, limite, termos), formato atual do informe diário CVM, preços do Tesouro, SGS 12 e 433 do BCB | P | — | ADR-004 com fonte escolhida por classe |
| T2.2 | Camada de fontes plugável (`MarketDataSource` por classe) + cache diário + entidade Cotação | M [T2.1] | R11 | Falha de fonte mantém última cotação com data; nunca interpola |
| T2.3 | Clientes: cotações B3, cota CVM por CNPJ, Tesouro, CDI/IPCA (BCB) | G [T2.2] | — | Testes de contrato com MockWebServer e payloads reais gravados |
| T2.4 | Motor de remarcação por classe (quantidade congelada × preço; CDI × %; curva IPCA+/pré; congelado) + nível de confiança | G [T2.3] | R9, R10 | Um teste por linha da tabela de classes do documento |
| T2.5 | Eventos corporativos: desdobramento/grupamento e proventos "a receber" | M [T2.4] | R12 | Quantidade ajustada + aviso; provento fora do patrimônio |
| T2.6 | Calendário de dias úteis B3 + `RemarcacaoWorker` (WorkManager, após 20h, dias úteis, restrições de rede) | M [T2.4] | R13 | WorkManager TestDriver; não roda em feriado |
| T2.7 | Camada 2 — motor de regras de alerta com as regras da tabela de alertas e limites configuráveis | G [T1.14, T2.4] | R23, R24 | Um teste por regra/severidade; limites vindos do perfil |
| T2.8 | Ciclo de vida do alerta (aberto → visto → resolvido/silenciado com motivo) e deduplicação por fato/faixa | M [T2.7] | R21, R22 | Mesmo alerta não reenvia; piorar de faixa reenvia |
| T2.9 | Notificações locais (canais por severidade, permissão POST_NOTIFICATIONS, deep link) — push só Urgente/Crítico | M [T2.8] | R24 | Oscilação diária nunca notifica |
| T2.10 | Camada 3 curta para alerta crítico novo (explica e sugere) | P [T2.8, T1.19] | — | Passa no validador |
| T2.11 | Resumo semanal (segunda-feira): variação, maiores altas/quedas, alertas abertos | M [T2.6, T1.19] | — | Worker semanal + notificação |
| T2.12 | Tela 1 — Carteira: patrimônio estimado, variação desde o upload, barra de confiança, alocação × alvo, alertas, dias desde o upload (30 sugere / 45 desatualiza) | G [T2.4] | R9, R10, R11 | Aviso quando congelado > 20% |
| T2.13 | Tela 6 — Alertas: abertos, silenciados, resolvidos, silenciar com motivo | M [T2.8] | R21, R22 | UI tests por estado |

**Critério de saída:** erro da estimativa < 1% do patrimônio num ciclo de 30 dias entre dois uploads.

### Fase 3 — Reconciliação e histórico

| # | Task | Tam. | Regras | Critério de aceite |
|---|---|---|---|---|
| T3.1 | Motor de reconciliação por chave R4: nova, zerada, aumento/redução, só preço, vencimento de RF, mudança de classe/gestora | G [T1.6] | R4, R15 | Um teste por situação da tabela do documento |
| T3.2 | Movimentações inferidas × extrato (extrato tem prioridade) | M [T3.1, T1.9] | R17 | Extrato substitui inferidas |
| T3.3 | TWR entre snapshots descontando aportes/resgates | M [T3.2] | R14 | Testes com casos calculados à mão + propriedade |
| T3.4 | Erro de estimativa por ativo e por classe + alerta interno para erro recorrente | M [T3.1, T2.4] | R18 | Só ativos sem mudança de quantidade entram no erro |
| T3.5 | Padrão de saques (> 20% em 12M → objetivo sugerido consumo) | P [T3.2] | R19 | Teste nos limites |
| T3.6 | Comparação entre análises: alertas resolvidos, que continuam e novos | M [T2.8] | R20 | — |
| T3.7 | Tela 4 — O que mudou | G [T3.1–T3.6] | R14, R15, R18, R20 | Screenshot + UI tests |
| T3.8 | Gráfico de evolução com âncoras (ponto cheio) e trechos estimados (tracejado) | M [T2.12] | R9 | Screenshot; nenhum ponto interpolado |

### Fase 4 — Extensões (a priorizar depois)

- T4.1 Outras corretoras via extração com IA (generalizar T1.20).
- T4.2 Perguntas em linguagem natural ("quanto rendi em FII este ano?").
- T4.3 Visão de IR (preço médio, DARF, informe).
- T4.4 Backend + sincronização + FCM (se o app for multiusuário).
- T4.5 Open Finance como alternativa ao upload.

### Transversais (contínuas)

- TX.1 Performance: Baseline Profile e Macrobenchmark da tela inicial.
- TX.2 Observabilidade sem dado pessoal (crash reporting com scrubbing).
- TX.3 Release: assinatura, R8, build de release no CI, canal interno da Play Store.
- TX.4 Revisão de segurança/LGPD antes de qualquer distribuição.

---

## 5. Ordem de execução sugerida

```
Fase 0 ──► T1.1 ─┬─► T1.2 ─► T1.12 ─► T1.14 ─► T1.17 ─► T1.19 ─► T1.25 ─► T1.26
                 ├─► T1.3 ─┐
                 └─► T1.4 ─┴─► T1.6 ─► T1.23
      T1.7 ─► T1.8 ─► T1.9 ─┬─► T1.11 ─► T1.22
             T1.10 ─────────┘
```

Primeira entrega útil: **Fase 0 + T1.1 a T1.6** (modelo, regras de validação e persistência, 100% testados na JVM), seguida dos parsers com golden tests.

---

## 6. Pontos a confirmar antes de começar

1. **App só para você ou para outros usuários?** (em aberto no documento). Define se a chave do Claude fica no aparelho (MVP pessoal) ou exige backend/proxy, cadastro e cuidados regulatórios da CVM.
2. **Fixtures reais:** preciso de um PDF XPerformance e um .xlsx de posição (posso anonimizar) para os golden tests dos parsers.
3. **Fonte de cotações da B3:** aceita uma API paga/com token (ex.: brapi) ou prefere só fontes gratuitas? (Spike T2.1.)
4. **minSdk:** proposta minSdk 26 (Android 8), targetSdk mais recente.
5. **Design system "Castanha":** há tokens/Figma disponíveis para o T0.6, ou derivo do protótipo?
