# ADR-0003 — Dinheiro, datas e origem do dado

- **Status:** aceito
- **Data:** 2026-09-27

## Decisão

- Dinheiro é `Money` (`BigDecimal`, 2 casas, arredondamento bancário). Nunca `Double`.
- Percentual é `Percent` em pontos percentuais (`0.73` = 0,73%).
- Todo valor exibido é `Sourced<T>` com `Origem` (RELATORIO, MERCADO, CALCULADO, ESTIMADO, IA).
  Ausência é `null`, nunca zero implícito nem interpolação (regra zero, R11).
- Datas com `java.time`; relógio sempre injetado (`Clock`) para testes determinísticos.
- Vencimento na chave de crédito privado (R4) usa `YearMonth`: o PDF XPerformance só traz mês/ano.
- Fundos sem CNPJ no arquivo (o XPerformance não traz) usam o nome normalizado como chave
  (`ChaveAtivo.FundoPorNome`) até o CNPJ ser resolvido.
