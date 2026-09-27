# Fixture sintética do XPerformance

Reproduz o layout do texto extraído (PDFBox, ordenado por posição) de um relatório XPerformance real,
com **valores, conta e assessor fictícios**. Nenhum dado de carteira real é versionado (R7/LGPD).

Casos cobertos: espaço não separável depois de `R$`, nome de ativo quebrado em duas linhas,
estratégia continuando em outra página, quantidade com ponto decimal, percentual com milhar
(`22.160,52%`), `-0,00%`, posição fantasma (quantidade com saldo zero), rentabilidade implausível
em renda fixa, FII fora do catálogo, soma das posições 0,17% abaixo do patrimônio informado.

Para testar com relatórios reais, coloque os PDFs em `local-fixtures/` (ignorado pelo git).
