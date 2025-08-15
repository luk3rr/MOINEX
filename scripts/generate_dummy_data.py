import sqlite3
from datetime import datetime, timedelta
import argparse
import random

CONFIG = {
    "initial_data": {
        "wallet": [
            # id, type_id, name, initial_balance, archived
            (1, 2, 'Banco XYZ', 100.00, 0),
            (2, 5, 'Carteira', 350.50, 0),
            (3, 4, 'Emergência', 10000.00, 0),
            (4, 4, 'Economia', 0.00, 0),
            (5, 1, 'Corretora KLM', 5012.00, 0),
            (6, 6, 'Comprar uma bike', 980.00, 0),
            (7, 6, 'Comprar um celular novo', 1320.00, 0),
            (8, 6, 'Investir 10k em ações', 0.00, 0)
        ],
        "goal": [
            # wallet_id, motivation, initial_balance, target_balance, target_date_offsets, completion_date_offsets (None if not completed)
            (6, 'Andar de bike é uma das melhores coisas do mundo', 250.00, 1500.00, (6, 0), None),
            (7, 'O atual já está dando sinais de que não aguentará muito tempo', 1500.00, 3000.00, (12, 0), None),
            (8, 'Aplicar parte do meu patrimônio em renda variável', 0.00, 1200.00, (-2, 0), (-3, 0))
        ],
        "financial_plan": [
            # id, name, base_income, archived
            (1, 'Plano 50/30/20', 3531.00, 0)
        ],
        "budget_group": [
            # id, name, target_percentage, plan_id
            (1, 'Essentials', 50.00, 1),
            (2, 'Wants', 30.00, 1),
            (3, 'Investments', 20.00, 1)
        ],
        "budget_group_categories": [
            # budget_group_id, category_id
            (1, 1), # Necessidades -> Supermercado
            (1, 6), # Necessidades -> Aluguel
            (1, 7), # Necessidades -> Internet
            (1, 2), # Necessidades -> Transporte
            (1, 4), # Necessidades -> Saúde
            (2, 3), # Desejos -> Viagem
            (2, 5), # Desejos -> Livros
            (2, 8), # Desejos -> Veterinário
            (2, 13),# Desejos -> Cinema
        ],
        "recurring_transaction": [
            # id, amount, description, type, frequency, start_date, end_date, next_due_date, status, category_id, wallet_id
            (1, 3531.00, 'Salário Mensal', 'INCOME', 'MONTHLY', (-11, 5), (120, 5), (0, 5), 'ACTIVE', 11, 1),
            (2, 830.00, 'Compra Mensal Supermercado', 'EXPENSE', 'MONTHLY', (-11, 15), (120, 15), (0, 15), 'ACTIVE', 1, 1),
            (3, 140.00, 'Mensalidade Academia', 'EXPENSE', 'MONTHLY', (-11, 10), (120, 10), (0, 10), 'ACTIVE', 4, 1)
        ],
        "credit_card": [
            # id, operator_id, name, billing_due_day, closing_day, max_debt, last_four_digits, default_billing_wallet_id
            (1, 1, 'Visa Gold', 10, 3, 5000.00, '1234', 1),
            (2, 2, 'MC Platinum', 15, 7, 7500.00, '5678', 1),
            (3, 3, 'Amex Green', 20, 10, 3000.00, '9101', 1),
            (4, 4, 'Discover Cashback', 25, 20, 2000.00, '1121', 1),
            (5, 5, 'Diners Club Rewards', 25, 19, 8000.00, '3141', 1),
            (6, 6, 'JCB', 10, 2, 1000.00, '3830', 1),
            (7, 7, 'Elo', 11, 3, 2300.00, '4301', 1),
            (8, 8, 'Hipercard', 5, 1, 500.00, '9031', 1),
            (9, 0, 'Cartaozin', 5, 1, 1.00, '1683', 1),
            (10, 0, 'Cartaozin2', 5, 1, 2.00, '3031', 1)
        ],
        # As tuplas representam: (id, crc_id, category_id, (meses_atras, dias_atras), total_amount, installments, description)
        "credit_card_debt": [
            (1, 1, 1, (-11, 9), 200.00, 2, 'Compra de supermercado'),
            (2, 1, 2, (-11, 9), 150.00, 2, 'Transporte público'),
            (3, 2, 3, (-11, 14), 500.00, 2, 'Viagem de férias'),
            (4, 3, 4, (-11, 19), 300.00, 2, 'Consulta médica'),
            (5, 4, 5, (-11, 24), 250.00, 2, 'Material escolar'),
            (6, 1, 1, (0, 9), 180.00, 2, 'Compra de supermercado'),
            (7, 2, 2, (0, 14), 400.00, 2, 'Passagem de ônibus'),
            (8, 3, 3, (12, 19), 600.00, 2, 'Viagem planejada'),
            (9, 4, 4, (12, 24), 320.00, 2, 'Consulta médica'),
            (10, 5, 5, (12, 29), 270.00, 10, 'Compra de livros'),
            (11, 2, 12, (0, 1), 450.00, 3, 'Compra online - Roupas'),
            (12, 6, 1, (-5, 20), 85.00, 1, 'Restaurante japonês'),
            (13, 5, 6, (1, 5), 2400.00, 12, 'Reforma da cozinha'),
            (14, 7, 7, (0, 5), 59.90, 1, 'Assinatura Streaming de Música'),
            (15, 8, 13, (-1, 18), 120.00, 2, 'Ingressos para cinema')
        ],
        # As tuplas representam: (id, wallet_id, debt_id, (meses_atras, dias_atras), amount, installment)
        "credit_card_payment": [
            (1, 1, 1, (-16, 10), 100.00, 1), (2, 1, 1, (-15, 10), 100.00, 2),
            (3, 1, 2, (-14, 10), 75.00, 1), (4, 1, 2, (-13, 10), 75.00, 2),
            (5, 2, 3, (-13, 10), 250.00, 1), (6, 2, 3, (-12, 10), 250.00, 2),
            (7, 3, 4, (-12, 10), 150.00, 1), (8, 3, 4, (-11, 10), 150.00, 2),
            (9, 4, 5, (-11, 10), 125.00, 1), (10, 4, 5, (-10, 10), 125.00, 2),
            (11, None, 6, (0, 10), 90.00, 1), (12, None, 6, (1, 10), 90.00, 2),
            (13, None, 7, (0, 10), 200.00, 1), (14, None, 7, (1, 10), 200.00, 2),
            (15, None, 8, (0, 10), 300.00, 1), (16, None, 8, (1, 10), 300.00, 2),
            (17, None, 9, (0, 10), 320.00, 1),
            (19, None, 10, (0, 10), 27.00, 1), (20, None, 10, (1, 10), 27.00, 2),
            (21, None, 10, (2, 10), 27.00, 3), (22, None, 10, (3, 10), 27.00, 4),
            (23, None, 10, (4, 10), 27.00, 5), (24, None, 10, (5, 10), 27.00, 6),
            (25, None, 10, (6, 10), 27.00, 7), (26, None, 10, (7, 10), 27.00, 8),
            (27, None, 10, (8, 10), 27.00, 9), (28, None, 10, (9, 10), 27.00, 10),
            (29, None, 11, (0, 15), 150.00, 1),
            (30, None, 11, (1, 15), 150.00, 2),
            (31, None, 11, (2, 15), 150.00, 3),
            (32, 1, 12, (-4, 10), 85.00, 1),
            (33, None, 13, (1, 25), 200.00, 1),
            (34, None, 13, (2, 25), 200.00, 2),
            (35, None, 13, (3, 25), 200.00, 3),
            (36, None, 13, (4, 25), 200.00, 4),
            (37, None, 13, (5, 25), 200.00, 5),
            (38, None, 13, (6, 25), 200.00, 6),
            (39, None, 13, (7, 25), 200.00, 7),
            (40, None, 13, (8, 25), 200.00, 8),
            (41, None, 13, (9, 25), 200.00, 9),
            (42, None, 13, (10, 25), 200.00, 10),
            (43, None, 13, (11, 25), 200.00, 11),
            (44, None, 13, (12, 25), 200.00, 12),
            (45, None, 14, (0, 11), 59.90, 1),
            (46, 1, 15, (0, 5), 60.00, 1),
            (47, None, 15, (1, 5), 60.00, 2)
        ],
        "possible_transactions": [
            # --- Rendas (INCOME) ---
            (1, 11, 'INCOME', 'CONFIRMED', 3531.00, 'Salário'),
            (1, 0, 'INCOME', 'CONFIRMED', 150.00, 'Devolução de fiança'),
            (1, 0, 'INCOME', 'CONFIRMED', 200.00, 'Devolução de empréstimo'),
            (1, 12, 'INCOME', 'CONFIRMED', 500.00, 'Trabalho Freelance'),

            # --- Despesas (EXPENSE) ---
            (1, 1, 'EXPENSE', 'CONFIRMED', 830.00, 'Compra de alimentos no supermercado'),
            (1, 6, 'EXPENSE', 'CONFIRMED', 2500.00, 'Aluguel'),
            (1, 7, 'EXPENSE', 'CONFIRMED', 150.00, 'Pagamento de serviço de internet'),
            (1, 4, 'EXPENSE', 'PENDING', 140.00, 'Pagamento de academia'),
            (1, 1, 'EXPENSE', 'CONFIRMED', 130.00, 'Jantar em restaurante'),
            (1, 1, 'EXPENSE', 'CONFIRMED', 45.00, 'Café da manhã em padaria'),
            (1, 8, 'EXPENSE', 'CONFIRMED', 860.00, 'Veterinário'),
            (1, 2, 'EXPENSE', 'CONFIRMED', 97.00, 'Táxi para o aeroporto'),
            (1, 5, 'EXPENSE', 'CONFIRMED', 270.00, 'Compra de livro'),
            (1, 1, 'EXPENSE', 'CONFIRMED', 330.00, 'Jantar fora de casa'),
            (1, 0, 'EXPENSE', 'PENDING', 1200.00, 'Presentes e comemorações de final de ano'),
        ],
        "monthly_rules": [
            {'month_offset': -11, 'target_income': 3550, 'target_expense': 3000},
            {'month_offset': -10, 'target_income': 3550, 'target_expense': 3200},
            {'month_offset': -9,  'target_income': 4000, 'target_expense': 3500},
            {'month_offset': -8,  'target_income': 3550, 'target_expense': 3000},
            {'month_offset': -7,  'target_income': 3550, 'target_expense': 3500},
            {'month_offset': -6,  'target_income': 3800, 'target_expense': 2000},
            {'month_offset': -5,  'target_income': 3550, 'target_expense': 3300},
            {'month_offset': -4,  'target_income': 3550, 'target_expense': 3100},
            {'month_offset': -3,  'target_income': 4200, 'target_expense': 2000},
            {'month_offset': -2,  'target_income': 3550, 'target_expense': 3000},
            {'month_offset': -1,  'target_income': 3700, 'target_expense': 3000},
            {'month_offset': 0,   'target_income': 3550, 'target_expense': 3000},
        ]
    }
}

def get_dynamic_date(months_offset, days_offset):
    """Calcula uma data dinâmica baseada em offsets de meses e dias a partir do início do mês atual."""
    total_days_offset = (months_offset * 30) + days_offset
    date = datetime.now().replace(day=1) + timedelta(days=total_days_offset)
    return date.strftime('%Y-%m-%dT%H:%M:%S')

def execute_query(cursor, query, params=()):
    """Executa uma query e ignora erros de forma silenciosa."""
    try:
        cursor.execute(query, params)
    except sqlite3.Error as e:
        print(f"Ignored error executing query: {e}")

def clear_data(cursor):
    """Limpa os dados das tabelas para uma nova inserção."""
    print("Erasing old data from the database...")
    tables_to_clear = ["recurring_transaction", "budget_group_categories", "budget_group", "financial_plan", "goal", "credit_card_payment", "credit_card_debt", "credit_card", "wallet_transaction", "wallet"]
    for table in tables_to_clear:
        execute_query(cursor, f"DELETE FROM {table};")
        if table not in ["goal", "budget_group_categories"]:
            execute_query(cursor, f"UPDATE sqlite_sequence SET seq = 0 WHERE name = '{table}';")
    print("All old data erased.")

def populate_initial_data(cursor, data):
    """Insere os dados iniciais que não são transações de carteira."""
    print("Inserting initial static data...")
    for row in data["wallet"]:
        execute_query(cursor, "INSERT INTO wallet (id, type_id, name, balance, archived) VALUES (?, ?, ?, ?, ?);", row)

    for row in data["goal"]:
        wallet_id, motivation, initial_balance, target_balance, target_date_offsets, completion_date_offsets = row
        target_date = get_dynamic_date(target_date_offsets[0], target_date_offsets[1])
        completion_date = None
        if completion_date_offsets:
            completion_date = get_dynamic_date(completion_date_offsets[0], completion_date_offsets[1])

        params = (wallet_id, completion_date, initial_balance, motivation, target_balance, target_date)
        execute_query(cursor, "INSERT INTO goal (wallet_id, completion_date, initial_balance, motivation, target_balance, target_date) VALUES (?, ?, ?, ?, ?, ?);", params)

    for row in data["financial_plan"]:
        execute_query(cursor, "INSERT INTO financial_plan (id, name, base_income, archived) VALUES (?, ?, ?, ?);", row)

    for row in data["budget_group"]:
        execute_query(cursor, "INSERT INTO budget_group (id, name, target_percentage, plan_id) VALUES (?, ?, ?, ?);", row)

    for row in data["budget_group_categories"]:
        execute_query(cursor, "INSERT INTO budget_group_categories (budget_group_id, category_id) VALUES (?, ?);", row)

    for row in data["recurring_transaction"]:
        rec_id, amount, desc, type, freq, start_offsets, end_offsets, next_due_offsets, status, cat_id, wallet_id = row
        start_date = get_dynamic_date(start_offsets[0], start_offsets[1])
        end_date = get_dynamic_date(end_offsets[0], end_offsets[1])
        next_due_date = get_dynamic_date(next_due_offsets[0], next_due_offsets[1])
        params = (rec_id, amount, desc, type, end_date, freq, next_due_date, start_date, status, cat_id, wallet_id)
        execute_query(cursor, "INSERT INTO recurring_transaction (id, amount, description, type, end_date, frequency, next_due_date, start_date, status, category_id, wallet_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", params)

    for row in data["credit_card"]:
        execute_query(cursor, "INSERT INTO credit_card (id, operator_id, name, billing_due_day, closing_day, max_debt, last_four_digits, default_billing_wallet_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", row)
    for row in data["credit_card_debt"]:
        debt_id, crc_id, cat_id, date_offsets, amount, inst, desc = row
        dynamic_date = get_dynamic_date(date_offsets[0], date_offsets[1])
        params = (debt_id, crc_id, cat_id, dynamic_date, amount, inst, desc)
        execute_query(cursor, "INSERT INTO credit_card_debt (id, crc_id, category_id, date, amount, installments, description) VALUES (?, ?, ?, ?, ?, ?, ?);", params)
    for row in data["credit_card_payment"]:
        pay_id, wallet_id, debt_id, date_offsets, amount, inst = row
        dynamic_date = get_dynamic_date(date_offsets[0], date_offsets[1])
        params = (pay_id, wallet_id, debt_id, dynamic_date, amount, inst)
        execute_query(cursor, "INSERT INTO credit_card_payment (id, wallet_id, debt_id, date, amount, installment) VALUES (?, ?, ?, ?, ?, ?);", params)
    print("Initial data inserted successfully.")

def generate_wallet_transactions(rules, possibilities):
    """Gera transações de carteira dinamicamente com base em regras e possibilidades."""
    print("Generating dynamic wallet transactions...")
    possible_incomes = [t for t in possibilities if t[2] == 'INCOME']
    possible_expenses = [t for t in possibilities if t[2] == 'EXPENSE']

    generated_transactions_raw = []
    for rule in rules:
        month = rule['month_offset']
        # Gerar Rendas
        current_income = 0
        while current_income < rule['target_income']:
            transaction_template = random.choice(possible_incomes)
            if current_income + transaction_template[4] < rule['target_income'] * 1.2:
                current_income += transaction_template[4]
                day = random.randint(1, 28)
                generated_transactions_raw.append(transaction_template + ((month, day),))
        # Gerar Despesas
        current_expense = 0
        while current_expense < rule['target_expense']:
            transaction_template = random.choice(possible_expenses)
            if current_expense + transaction_template[4] < rule['target_expense'] * 1.2:
                current_expense += transaction_template[4]
                day = random.randint(1, 28)
                generated_transactions_raw.append(transaction_template + ((month, day),))

    sorted_transactions = sorted(generated_transactions_raw, key=lambda x: x[6])

    final_transactions = []
    for new_id, transaction in enumerate(sorted_transactions, 1):
        wallet_id, category_id, trans_type, status, amount, description, date_tuple = transaction
        final_transactions.append((new_id, wallet_id, category_id, trans_type, status, date_tuple, amount, description))

    print(f"{len(final_transactions)} wallet transactions generated.")
    return final_transactions

def insert_wallet_transactions(cursor, transactions):
    """Insere as transações de carteira geradas no banco de dados."""
    print("Inserting generated wallet transactions into database...")
    for row in transactions:
        trans_id, wallet_id, cat_id, type, status, date_offsets, amount, desc = row
        dynamic_date = get_dynamic_date(date_offsets[0], date_offsets[1])
        params = (trans_id, wallet_id, cat_id, type, status, dynamic_date, amount, desc)
        execute_query(cursor, "INSERT INTO wallet_transaction (id, wallet_id, category_id, type, status, date, amount, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", params)
    print("Wallet transactions inserted.")

def update_wallet_balances(cursor):
    """Calcula e atualiza os saldos finais de cada carteira a partir do saldo inicial."""
    print("Calculating and updating wallet balances...")
    cursor.execute("SELECT id, balance FROM wallet;")
    wallets = cursor.fetchall()

    for wallet_id, initial_balance in wallets:
        cursor.execute("SELECT SUM(amount) FROM wallet_transaction WHERE wallet_id = ? AND type = 'INCOME';", (wallet_id,))
        total_income = cursor.fetchone()[0] or 0.0
        cursor.execute("SELECT SUM(amount) FROM wallet_transaction WHERE wallet_id = ? AND type = 'EXPENSE';", (wallet_id,))
        total_expense = cursor.fetchone()[0] or 0.0

        final_balance = round(initial_balance + total_income - total_expense, 2)

        cursor.execute("UPDATE wallet SET balance = ? WHERE id = ?;", (final_balance, wallet_id))
    print("Wallet balances updated successfully.")

def main():
    """Função principal para executar todo o processo."""
    parser = argparse.ArgumentParser(description="Generate dummy data for the wallet application.")
    parser.add_argument("--db_path", type=str, default="wallet.db", help="Path to the SQLite database file.")
    args = parser.parse_args()

    connection = None
    try:
        connection = sqlite3.connect(args.db_path)
        cursor = connection.cursor()
        cursor.execute("PRAGMA foreign_keys = ON;")

        clear_data(cursor)
        populate_initial_data(cursor, CONFIG["initial_data"])

        # Gera as transações dinamicamente
        wallet_transactions = generate_wallet_transactions(
            CONFIG["initial_data"]["monthly_rules"],
            CONFIG["initial_data"]["possible_transactions"]
        )
        # Insere as transações geradas
        insert_wallet_transactions(cursor, wallet_transactions)

        update_wallet_balances(cursor)

        connection.commit()
        print("\nDemonstration data inserted successfully!")

    except sqlite3.Error as e:
        print(f"Oh no! An error occurred: {e}")
        if connection:
            connection.rollback()
    finally:
        if connection:
            connection.close()

if __name__ == "__main__":
    main()
