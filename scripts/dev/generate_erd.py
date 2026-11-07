# Filename: generate_erd.py
# Created on: November 7, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""Gera docs/database-erd.md, o diagrama entidade-relacionamento do banco em Mermaid.

O schema vem das migrations (src/main/resources/db/migration/V*.sql), aplicadas em ordem a um SQLite em memória, então
o diagrama reflete o que o Flyway cria de fato e nunca depende de uma lista mantida à mão. Uma tabela que não esteja em
nenhum domínio de DOMAINS cai em "Outras tabelas", e o teste de src/test/python confere que nenhuma ficou de fora.

Uso: python3 scripts/dev/generate_erd.py [--output docs/database-erd.md]
"""

import argparse
import glob
import os
import re
import sqlite3

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MIGRATIONS = os.path.join(ROOT, "src", "main", "resources", "db", "migration")
DEFAULT_OUTPUT = os.path.join(ROOT, "docs", "database-erd.md")

DOMAINS = [
    (
        "Carteiras e transações",
        ["wallet_type", "wallet", "category", "wallet_transaction", "transfer", "recurring_transaction", "goal"],
    ),
    (
        "Cartões de crédito",
        [
            "credit_card_operator",
            "credit_card",
            "credit_card_debt",
            "credit_card_payment",
            "credit_card_credit",
            "recurring_credit_card_debt",
        ],
    ),
    (
        "Investimentos",
        [
            "ticker",
            "ticker_purchase",
            "ticker_sale",
            "purchase",
            "sale",
            "dividend",
            "crypto_exchange",
            "ticker_price_history",
            "fundamental_analysis",
            "bond",
            "bond_operation",
            "bond_interest_calculation",
            "investment_target",
            "investment_performance_snapshot",
            "benchmark_price_history",
        ],
    ),
    (
        "Planejamento e outros",
        [
            "financial_plan",
            "budget_group",
            "budget_group_categories",
            "fire_calculator_settings",
            "wishlist_item",
            "wishlist_item_link",
            "net_worth_snapshot",
            "notification",
            "calendar_event",
        ],
    ),
    (
        "Mercado",
        ["brazilian_market_indicators", "market_quotes_and_commodities", "market_indicator_history"],
    ),
    (
        "Conversas com o assistente de IA",
        ["ai_chat", "ai_chat_message", "ai_chat_change_set", "ai_chat_change"],
    ),
]


def migrated_database():
    """Aplica as migrations em ordem de versão a um banco em memória."""
    database = sqlite3.connect(":memory:")
    files = sorted(
        glob.glob(os.path.join(MIGRATIONS, "V*.sql")),
        key=lambda path: int(re.search(r"V(\d+)", os.path.basename(path)).group(1)),
    )
    for path in files:
        with open(path, encoding="utf-8") as handle:
            database.executescript(handle.read())
    return database


def read_schema(database):
    """Devolve {tabela: {"columns": [...], "foreign_keys": [...]}} sem as tabelas internas do SQLite."""
    names = [
        row[0]
        for row in database.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
        )
    ]
    schema = {}
    for name in names:
        columns = [
            {"name": row[1], "type": row[2], "not_null": bool(row[3]), "primary_key": bool(row[5])}
            for row in database.execute(f"PRAGMA table_info({name})")
        ]
        foreign_keys = [
            {"column": row[3], "table": row[2]} for row in database.execute(f"PRAGMA foreign_key_list({name})")
        ]
        unique_columns = unique_single_columns(database, name)
        schema[name] = {"columns": columns, "foreign_keys": foreign_keys, "unique": unique_columns}
    return schema


def unique_single_columns(database, table):
    """Colunas com índice único de uma coluna só, usadas para reconhecer relações um para um."""
    unique = set()
    for index in database.execute(f"PRAGMA index_list({table})"):
        if index[2]:
            columns = [row[2] for row in database.execute(f"PRAGMA index_info({index[1]})")]
            if len(columns) == 1:
                unique.add(columns[0])
    return unique


def simple_type(declared):
    """Tipo de uma palavra só, sem tamanho, que o Mermaid aceita como tipo de atributo."""
    base = re.sub(r"\(.*\)", "", declared or "").strip().lower()
    return re.sub(r"\s+", "_", base) or "text"


def relationship(name, foreign_key, schema):
    """Linha Mermaid da relação, do pai para o filho; um para um quando a coluna é única."""
    column = next(c for c in schema[name]["columns"] if c["name"] == foreign_key["column"])
    one_to_one = foreign_key["column"] in schema[name]["unique"]
    parent_side = "|o" if not column["not_null"] else "||"
    child_side = "o|" if one_to_one else "o{"
    return f'  {foreign_key["table"].upper()} {parent_side}--{child_side} {name.upper()} : "{foreign_key["column"]}"'


def entity_block(name, schema):
    foreign = {fk["column"] for fk in schema[name]["foreign_keys"]}
    lines = [f"  {name.upper()} {{"]
    for column in schema[name]["columns"]:
        keys = []
        if column["primary_key"]:
            keys.append("PK")
        if column["name"] in foreign:
            keys.append("FK")
        lines.append(f'    {simple_type(column["type"])} {column["name"]}{" " + ",".join(keys) if keys else ""}')
    lines.append("  }")
    return "\n".join(lines)


def domain_diagram(tables, schema):
    blocks = [entity_block(name, schema) for name in tables]
    relations = [relationship(name, fk, schema) for name in tables for fk in schema[name]["foreign_keys"]]
    return "```mermaid\nerDiagram\n" + "\n".join(blocks + relations) + "\n```"


def overview_diagram(schema):
    relations = [relationship(name, fk, schema) for name in schema for fk in schema[name]["foreign_keys"]]
    isolated = [name.upper() for name in schema if not schema[name]["foreign_keys"] and not any(
        fk["table"] == name for other in schema.values() for fk in other["foreign_keys"]
    )]
    lines = ["```mermaid", "erDiagram"] + relations
    lines += [f"  {name} {{\n  }}" for name in isolated]
    lines.append("```")
    return "\n".join(lines)


def render(schema):
    grouped = {name for _, tables in DOMAINS for name in tables}
    others = sorted(name for name in schema if name not in grouped)
    domains = [(title, [t for t in tables if t in schema]) for title, tables in DOMAINS]
    if others:
        domains.append(("Outras tabelas", others))

    parts = [
        "# Banco de dados: diagrama entidade-relacionamento",
        "",
        "Gerado por `scripts/dev/generate_erd.py` a partir das migrations Flyway "
        "(`src/main/resources/db/migration/`). Não edite à mão: rode `make erd` depois de criar uma migration.",
        "",
        f"O banco tem {len(schema)} tabelas. Datas são guardadas como texto ISO, e valores monetários como `numeric`. "
        "O SQLite do app não liga `PRAGMA foreign_keys`, então as chaves estrangeiras documentam as relações, mas o "
        "banco não as impõe; exclusões em cascata são feitas pelo código.",
        "",
        "Legenda: `PK` chave primária, `FK` chave estrangeira. `||--o{` é um para muitos; `|o--o{` quando a chave "
        "estrangeira aceita nulo; `|o--o|` é um para um.",
        "",
        "## Visão geral das relações",
        "",
        overview_diagram(schema),
        "",
    ]
    for title, tables in domains:
        parts += [f"## {title}", "", domain_diagram(tables, schema), ""]
    return "\n".join(parts)


def main():
    parser = argparse.ArgumentParser(description="Gera o diagrama ER do banco em Mermaid.")
    parser.add_argument("--output", default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    schema = read_schema(migrated_database())
    with open(args.output, "w", encoding="utf-8") as handle:
        handle.write(render(schema))
    print(f"{len(schema)} tables written to {args.output}")


if __name__ == "__main__":
    main()
