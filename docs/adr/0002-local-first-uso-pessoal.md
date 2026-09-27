# ADR-0002 — Local-first, uso pessoal, apenas fontes gratuitas

- **Status:** aceito
- **Data:** 2026-09-27

## Contexto

Pergunta em aberto no documento de produto: o app é só para o dono ou para outros usuários?
**Resposta: só para uso pessoal.** Fontes de cotação: **apenas gratuitas**.

## Decisão

- Sem backend no MVP. Dados ficam no aparelho, criptografados (Room + SQLCipher; arquivo original
  guardado via Android Keystore, R7).
- A chave da API do Claude é informada pelo usuário e guardada no Android Keystore.
- Push é notificação local gerada pelo job diário (WorkManager); sem FCM.
- Fontes de mercado gratuitas: dados abertos da CVM (cota diária), Tesouro Transparente (preços e
  taxas), Banco Central SGS (CDI 12, IPCA 433). Para cotações B3, avaliar no spike T2.1 opções
  gratuitas; na falta de uma confiável, a classe fica "congelada" (R11: sem fonte, sem número).
- Sem cadastro, sem multiusuário e sem as exigências da CVM para recomendação a terceiros.

## Consequências

- Menor superfície de segurança e custo zero de infraestrutura.
- Se um dia houver outros usuários, será necessário um backend (proxy da chave, sincronização).
  A camada de dados por interfaces deixa essa porta aberta.
