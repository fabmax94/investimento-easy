package com.investimentoeasy.parser.xperformance

/** Valor de um grupo nomeado obrigatório do regex. */
internal fun MatchResult.grupo(nome: String): String = requireNotNull(groups[nome]) { "Grupo ausente: $nome" }.value
