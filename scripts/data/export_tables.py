# Filename: export_tables.py
# Created on: October 23, 2024
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sqlite3
import csv
import os
import argparse


def export_tables_to_csv(db_file, output_dir):
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Get all table names
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()

    for table_name in tables:
        table_name = table_name[0]
        output_file = os.path.join(output_dir, f"{table_name}.csv")

        cursor.execute(f"SELECT * FROM {table_name};")
        rows = cursor.fetchall()

        column_names = [description[0] for description in cursor.description]

        with open(output_file, mode="w", newline="", encoding="utf-8") as file:
            csv_writer = csv.writer(file)
            csv_writer.writerow(column_names)
            csv_writer.writerows(rows)

        print(f"Tabela {table_name} exportada para {output_file}")

    conn.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Exporta todas as tabelas de um arquivo .db SQLite para arquivos .csv"
    )
    parser.add_argument("db_file", help="Caminho para o arquivo .db do SQLite")
    parser.add_argument(
        "output_dir", help="Diretório onde os arquivos .csv serão salvos"
    )

    args = parser.parse_args()
    export_tables_to_csv(args.db_file, args.output_dir)
