# ADR-0001 — Arquitetura: app Android nativo, Clean Architecture, núcleo em Kotlin/JVM puro

- **Status:** aceito
- **Data:** 2026-09-27

## Contexto

O documento de produto sugeria React Native ou PWA com backend. O pedido é um app Android, e o
coração do produto são regras de negócio (R1–R24) que precisam de muitos testes.

## Decisão

- App nativo em Kotlin + Jetpack Compose, com Clean Architecture e MVVM/UDF.
- Regras de negócio, cálculos e parsers ficam em módulos **Kotlin/JVM puros** (`core:model`,
  `core:domain`, `parser:*`), sem dependência do Android. Rodam e são testados na JVM, rápido.
- Os módulos Android (`app`, `core:designsystem`, `feature:*`, `core:database`) dependem desses
  módulos, nunca o contrário. Persistência entra por interfaces (ex.: `SnapshotRepository`).
- Convenções de build em `build-logic` (convention plugins) e versões em `gradle/libs.versions.toml`.

## Consequências

- A maior parte do código de valor é testável sem emulador.
- É preciso disciplina para não vazar tipos do Android para o domínio (reforçada pela própria
  estrutura de módulos: os módulos JVM não enxergam o SDK do Android).
