# Investimento Easy — Monitor de Carteira

App Android pessoal que transforma cada upload da carteira (PDF XPerformance ou .xlsx da XP) num
snapshot confiável e, entre um upload e outro, acompanha a carteira com dados públicos.

- Plano e andamento: [`docs/PLANO_DE_DESENVOLVIMENTO.md`](docs/PLANO_DE_DESENVOLVIMENTO.md)
- Decisões: [`docs/adr/`](docs/adr/)

## Módulos

| Módulo | Conteúdo |
|---|---|
| `app` | Activity, navegação (barra inferior do protótipo), Hilt |
| `feature:upload` | Telas Enviar carteira, Revisar extração (R1, R3, R6, R8, R15) e Conferir planilha |
| `feature:portfolio` | Tela Carteira (base confirmada e alocação) |
| `feature:analysis` | Tela Análise: 7 abas, Ritmo/Cenário, alertas e a leitura do Claude |
| `parser:xlsx` | Leitor .xlsx sem POI (zip + XML, sem DTD, com limites) e parser da Posição Detalhada da XP |
| `core:ai` | Camada 3: entrada sem dado pessoal, chamada ao Claude, validação de cada número citado |
| `core:seguranca` | Chave da API cifrada (AES-GCM, Android Keystore) |
| `core:designsystem` | Tema Castanha (cores geradas do protótipo), tipografia, formatação pt-BR, componentes |
| `core:ui` | UI compartilhada que conhece o domínio (lista de alocação) |
| `core:database` | Room; histórico imutável (R16) no DAO e em triggers do SQLite |
| `core:documentos` | Texto do PDF (PdfBox-Android) e leitura do arquivo escolhido |
| `core:importacao` | Arquivo → texto → parser → revisão, com as falhas previstas |
| `core:common` | Relógio e dispatchers injetáveis |
| `core:model` | `Money`, `Percent`, `Sourced`/`Origem`, `ChaveAtivo` (R4), `Snapshot`, `ExtracaoCarteira` |
| `core:domain` | Classificação (R5), validação (R1, R3, R6), confirmação (R8, R15, R16), alocação, motor de análise (Camadas 1 e 2) |
| `core:testing` | Fakes e builders para testes |
| `parser:xperformance` | Parser determinístico do relatório XPerformance (R2, R7) |

Regra de dependência: `feature → core:ui → core:designsystem`, `feature → core:domain → core:model`;
os módulos de domínio e parser são Kotlin/JVM puros.

## Desenvolvimento

```bash
./scripts/setup-android-sdk.sh   # uma vez: licenças do SDK e local.properties
./gradlew check                  # ktlint, detekt, Android Lint, testes, screenshots e cobertura (Kover, 85%)
./gradlew :app:assembleDebug     # APK em app/build/outputs/apk/debug/
./gradlew recordRoborazziDebug   # regrava os screenshots depois de uma mudança visual intencional
./gradlew ktlintFormat
```

Requer JDK 17+ e acesso a `dl.google.com`/`maven.google.com` (o AGP baixa plataforma e build-tools).

### Dados pessoais

Relatórios reais **nunca** entram no repositório (`*.pdf`, `*.xlsx` e `local-fixtures/` estão no
`.gitignore`). Para validar o parser com um relatório seu, coloque o PDF em `local-fixtures/` e rode
`./gradlew :parser:xperformance:test`: o teste `XPerformanceRelatorioRealTest` confere as invariantes
(data, subtotais, soma dentro de 0,5%, nenhuma linha sem interpretação) sem registrar valores.
