# Investimento Easy — Monitor de Carteira

App Android pessoal que transforma cada upload da carteira (PDF XPerformance ou .xlsx da XP) num
snapshot confiável e, entre um upload e outro, acompanha a carteira com dados públicos.

- Plano e andamento: [`docs/PLANO_DE_DESENVOLVIMENTO.md`](docs/PLANO_DE_DESENVOLVIMENTO.md)
- Decisões: [`docs/adr/`](docs/adr/)

## Módulos

| Módulo | Conteúdo |
|---|---|
| `core:model` | Tipos de domínio: `Money`, `Percent`, `Sourced`/`Origem`, `ChaveAtivo` (R4), `Snapshot`, `ExtracaoCarteira` |
| `core:domain` | Classificação (R5), validação (R1, R3, R6), revisão e confirmação de snapshot (R8, R15, R16) |
| `core:testing` | Fakes e builders para testes |
| `parser:xperformance` | Parser determinístico do relatório XPerformance (R2, R7) |

Os módulos Android entram quando o ambiente tiver acesso ao Google Maven (ver plano, §7).

## Desenvolvimento

```bash
./gradlew check          # ktlint, detekt, testes e cobertura mínima agregada (Kover, 85%)
./gradlew ktlintFormat   # formata o código
./gradlew koverHtmlReport
```

Requer JDK 17+ (o bytecode é gerado para Java 17).

### Dados pessoais

Relatórios reais **nunca** entram no repositório (`*.pdf`, `*.xlsx` e `local-fixtures/` estão no
`.gitignore`). Para validar o parser com um relatório seu, coloque o PDF em `local-fixtures/` e rode
`./gradlew :parser:xperformance:test`: o teste `XPerformanceRelatorioRealTest` confere as invariantes
(data, subtotais, soma dentro de 0,5%, nenhuma linha sem interpretação) sem registrar valores.
