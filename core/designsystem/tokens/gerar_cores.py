#!/usr/bin/env python3
"""Gera CastanhaCores.kt a partir de castanha-tokens.json (design system Castanha do protótipo).

Uso: python3 core/designsystem/tokens/gerar_cores.py
"""
import json
import pathlib
import re

AQUI = pathlib.Path(__file__).parent
SAIDA = AQUI.parent / "src/main/kotlin/com/investimentoeasy/core/designsystem/CastanhaCores.kt"
PREFIXO = "castanha-colors-"


def camel(nome: str) -> str:
    partes = re.split(r"[-_]", nome)
    texto = partes[0] + "".join(p[:1].upper() + p[1:] for p in partes[1:])
    return "c" + texto if texto[0].isdigit() else texto


def cor(valor: str) -> str:
    valor = valor.strip()
    if valor.startswith("#"):
        return f"Color(0xFF{valor[1:].upper()})"
    m = re.match(r"rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)", valor)
    if not m:
        raise ValueError(valor)
    r, g, b, a = int(m[1]), int(m[2]), int(m[3]), float(m[4])
    return f"Color(0x{round(a * 255):02X}{r:02X}{g:02X}{b:02X})"


def main() -> None:
    tokens = json.loads((AQUI / "castanha-tokens.json").read_text())["color"]["tokens"]
    cores = [(camel(t["name"][len(PREFIXO):]), t["value"]) for t in tokens if t["name"].startswith(PREFIXO)]
    linhas = [
        "// ARQUIVO GERADO por core/designsystem/tokens/gerar_cores.py a partir de castanha-tokens.json.",
        "// Não edite à mão: altere os tokens e rode o script de novo.",
        "@file:Suppress(\"MagicNumber\", \"LongParameterList\")",
        "",
        "package com.investimentoeasy.core.designsystem",
        "",
        "import androidx.compose.runtime.Immutable",
        "import androidx.compose.ui.graphics.Color",
        "",
        "/** Todas as cores do design system Castanha, num tema (claro ou escuro). */",
        "@Immutable",
        "public data class CastanhaCores(",
    ]
    linhas += [f"    val {nome}: Color," for nome, _ in cores]
    linhas += [")", ""]
    for tema, chave in (("CastanhaCoresClaras", "light"), ("CastanhaCoresEscuras", "root-dark")):
        linhas.append(f"public val {tema}: CastanhaCores =")
        linhas.append("    CastanhaCores(")
        linhas += [f"        {nome} = {cor(valor[chave])}," for nome, valor in cores]
        linhas += ["    )", ""]
    SAIDA.write_text("\n".join(linhas))
    print(f"{len(cores)} cores -> {SAIDA}")


if __name__ == "__main__":
    main()
