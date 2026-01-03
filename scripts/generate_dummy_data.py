import sqlite3
from datetime import datetime, timedelta
import argparse
import random

CONFIG = {
    "initial_data": {
        "wallet": [
            # id, type_id, name, initial_balance, archived
            (1, 2, 'Banco XYZ', 8000.00, 0),
            (2, 5, 'Carteira', 350.50, 0),
            (3, 4, 'Emergência', 15000.00, 0),
            (4, 4, 'Economia', 0.00, 0),
            (5, 1, 'Corretora KLM', 5000.00, 0),
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
            (1, 'Plano 50/30/20', 8500.00, 0)
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
            (1, 8500.00, 'Salário Mensal', 'INCOME', 'MONTHLY', (-11, 5), (120, 5), (0, 5), 'ACTIVE', 11, 1),
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
            (6, 1, 1, (-6, 9), 180.00, 2, 'Compra de supermercado'),
            (7, 2, 2, (-5, 14), 400.00, 2, 'Passagem de ônibus'),
            (8, 2, 12, (-4, 1), 450.00, 3, 'Compra online - Roupas'),
            (9, 6, 1, (-5, 20), 85.00, 1, 'Restaurante japonês'),
            (10, 7, 7, (-3, 5), 59.90, 1, 'Assinatura Streaming de Música'),
            (11, 8, 13, (-2, 18), 120.00, 2, 'Ingressos para cinema')
        ],
        # As tuplas representam: (id, wallet_id, debt_id, (meses_atras, dias_atras), amount, installment)
        "credit_card_payment": [
            (1, 1, 1, (-10, 10), 100.00, 1), (2, 1, 1, (-9, 10), 100.00, 2),
            (3, 1, 2, (-10, 10), 75.00, 1), (4, 1, 2, (-9, 10), 75.00, 2),
            (5, 1, 3, (-10, 10), 250.00, 1), (6, 1, 3, (-9, 10), 250.00, 2),
            (7, 1, 4, (-10, 10), 150.00, 1), (8, 1, 4, (-9, 10), 150.00, 2),
            (9, 1, 5, (-10, 10), 125.00, 1), (10, 1, 5, (-9, 10), 125.00, 2),
            (11, 1, 6, (-5, 10), 90.00, 1), (12, 1, 6, (-4, 10), 90.00, 2),
            (13, 1, 7, (-4, 10), 200.00, 1), (14, 1, 7, (-3, 10), 200.00, 2),
            (15, 1, 8, (-3, 15), 150.00, 1), (16, 1, 8, (-2, 15), 150.00, 2),
            (17, 1, 8, (-1, 15), 150.00, 3),
            (18, 1, 9, (-4, 10), 85.00, 1),
            (19, 1, 10, (-2, 11), 59.90, 1),
            (20, 1, 11, (-1, 5), 60.00, 1),
            (21, 1, 11, (0, 5), 60.00, 2)
        ],
        "possible_transactions": [
            # --- Rendas (INCOME) ---
            (1, 11, 'INCOME', 'CONFIRMED', 8500.00, 'Salário'),
            (1, 0, 'INCOME', 'CONFIRMED', 150.00, 'Devolução de fiança'),
            (1, 0, 'INCOME', 'CONFIRMED', 200.00, 'Devolução de empréstimo'),
            (1, 12, 'INCOME', 'CONFIRMED', 1200.00, 'Trabalho Freelance'),

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
            {'month_offset': -11, 'target_income': 8500, 'target_expense': 3000},
            {'month_offset': -10, 'target_income': 8500, 'target_expense': 3200},
            {'month_offset': -9,  'target_income': 9700, 'target_expense': 3500},
            {'month_offset': -8,  'target_income': 8500, 'target_expense': 3000},
            {'month_offset': -7,  'target_income': 8500, 'target_expense': 3500},
            {'month_offset': -6,  'target_income': 9200, 'target_expense': 2000},
            {'month_offset': -5,  'target_income': 8500, 'target_expense': 3300},
            {'month_offset': -4,  'target_income': 8500, 'target_expense': 3100},
            {'month_offset': -3,  'target_income': 10000, 'target_expense': 2000},
            {'month_offset': -2,  'target_income': 8500, 'target_expense': 3000},
            {'month_offset': -1,  'target_income': 8800, 'target_expense': 3000},
            {'month_offset': 0,   'target_income': 8500, 'target_expense': 3000},
        ],
        "ticker": [
            # id, name, symbol, type, current_quantity, current_unit_value, average_unit_value, average_unit_value_count, archived, last_update_offset
            (1, 'Petrobras PN', 'PETR4.SA', 'STOCK', 100.00, 38.50, 35.20, 100.00, 0, (-1, 0)),
            (2, 'Vale ON', 'VALE3.SA', 'STOCK', 50.00, 62.30, 58.75, 50.00, 0, (-1, 0)),
            (3, 'Itaú Unibanco PN', 'ITUB4.SA', 'STOCK', 200.00, 28.90, 26.50, 200.00, 0, (-1, 0)),
            (4, 'Magazine Luiza ON', 'MGLU3.SA', 'STOCK', 150.00, 12.45, 15.80, 150.00, 0, (-1, 0)),
            (5, 'Bitcoin', 'BTC-USD', 'CRYPTOCURRENCY', 0.25, 95000.00, 82000.00, 0.25, 0, (0, 0)),
            (6, 'Ethereum', 'ETH-USD', 'CRYPTOCURRENCY', 2.50, 3500.00, 2800.00, 2.50, 0, (0, 0)),
            (7, 'Cardano', 'ADA-USD', 'CRYPTOCURRENCY', 5000.00, 0.45, 0.38, 5000.00, 0, (0, 0)),
            (8, 'IVVB11', 'IVVB11.SA', 'FUND', 30.00, 285.50, 270.00, 30.00, 0, (-1, 0)),
        ],
        "bond": [
            # id, name, symbol, type, issuer, maturity_date_offset, interest_type, interest_index, interest_rate, archived
            (1, 'CDB Banco XYZ 120% CDI', 'CDB-XYZ-001', 'CDB', 'Banco XYZ', (24, 0), 'FLOATING', 'CDI', 120.00, 0),
            (2, 'LCI Banco ABC 95% CDI', 'LCI-ABC-002', 'LCI', 'Banco ABC', (36, 0), 'FLOATING', 'CDI', 95.00, 0),
            (3, 'Tesouro Selic 2027', 'TESOURO-SELIC-2027', 'TREASURY_POSTFIXED', 'Tesouro Nacional', (36, 0), 'FLOATING', 'SELIC', 100.00, 0),
            (4, 'Tesouro Prefixado 2028', 'TESOURO-PRE-2028', 'TREASURY_PREFIXED', 'Tesouro Nacional', (48, 0), 'FIXED', 'OTHER', 12.50, 0),
            (5, 'Tesouro IPCA+ 2030', 'TESOURO-IPCA-2030', 'TREASURY_POSTFIXED', 'Tesouro Nacional', (72, 0), 'FLOATING', 'IPCA', 6.25, 0),
            (6, 'LCA Banco DEF 90% CDI', 'LCA-DEF-003', 'LCA', 'Banco DEF', (24, 0), 'FLOATING', 'CDI', 90.00, 0),
        ],
        "ticker_purchase": [
            # id, ticker_id, quantity, unit_price, wallet_id, (month_offset, day_offset), description
            (1, 1, 50.00, 32.00, 5, (-8, 5), 'Compra de ações PETR4'),
            (2, 1, 50.00, 38.40, 5, (-3, 12), 'Compra adicional PETR4'),
            (3, 2, 50.00, 58.75, 5, (-6, 8), 'Compra de ações VALE3'),
            (4, 3, 100.00, 25.00, 5, (-9, 15), 'Compra de ações ITUB4'),
            (5, 3, 100.00, 28.00, 5, (-2, 20), 'Compra adicional ITUB4'),
            (6, 4, 200.00, 15.80, 5, (-5, 10), 'Compra de ações MGLU3'),
            (7, 5, 0.02, 75000.00, 5, (-10, 3), 'Compra de Bitcoin'),
            (8, 5, 0.02, 92000.00, 5, (-2, 8), 'Compra adicional Bitcoin'),
            (9, 6, 0.50, 2500.00, 5, (-7, 12), 'Compra de Ethereum'),
            (10, 6, 0.30, 3200.00, 5, (-1, 5), 'Compra adicional Ethereum'),
            (11, 7, 500.00, 0.38, 5, (-4, 18), 'Compra de Cardano'),
            (12, 8, 10.00, 270.00, 5, (-6, 22), 'Compra de IVVB11'),
        ],
        "ticker_sale": [
            # id, ticker_id, quantity, unit_price, average_cost, wallet_id, (month_offset, day_offset), description
            (1, 4, 50.00, 18.50, 15.80, 5, (-1, 15), 'Venda parcial MGLU3'),
        ],
        "bond_operation": [
            # id, bond_id, operation_type, quantity, unit_price, fees, taxes, net_profit, wallet_id, (month_offset, day_offset), description
            (1, 1, 'BUY', 2.00, 1000.00, 5.00, 0.00, None, 5, (-9, 5), 'Aplicação em CDB'),
            (2, 2, 'BUY', 1.00, 2000.00, 3.00, 0.00, None, 5, (-7, 10), 'Aplicação em LCI'),
            (3, 3, 'BUY', 2.00, 1250.00, 0.00, 0.00, None, 5, (-6, 15), 'Compra Tesouro Selic'),
            (4, 4, 'BUY', 1.00, 1666.67, 0.00, 0.00, None, 5, (-5, 8), 'Compra Tesouro Prefixado'),
            (5, 5, 'BUY', 1.00, 2500.00, 0.00, 0.00, None, 5, (-4, 12), 'Compra Tesouro IPCA+'),
            (6, 6, 'BUY', 2.00, 833.33, 2.50, 0.00, None, 5, (-3, 20), 'Aplicação em LCA'),
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
    tables_to_clear = [
        "dividend", "ticker_sale", "ticker_purchase", "sale", "purchase", "crypto_exchange",
        "bond_operation", "bond", "ticker",
        "recurring_transaction", "budget_group_categories", "budget_group", "financial_plan", 
        "goal", "credit_card_payment", "credit_card_debt", "credit_card", "wallet_transaction", "wallet"
    ]
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

def populate_tickers(cursor, data):
    """Insere os tickers (ações, fundos, criptomoedas) no banco de dados."""
    print("Inserting tickers...")
    for row in data["ticker"]:
        ticker_id, name, symbol, ticker_type, current_quantity, current_unit_value, average_unit_value, average_unit_value_count, archived, last_update_offsets = row
        last_update = get_dynamic_date(last_update_offsets[0], last_update_offsets[1])
        params = (ticker_id, name, symbol, ticker_type, current_quantity, current_unit_value, average_unit_value, average_unit_value_count, archived, last_update)
        execute_query(cursor, "INSERT INTO ticker (id, name, symbol, type, current_quantity, current_unit_value, average_unit_value, average_unit_value_count, archived, last_update) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", params)
    print("Tickers inserted successfully.")

def populate_bonds(cursor, data):
    """Insere os bonds no banco de dados."""
    print("Inserting bonds...")
    for row in data["bond"]:
        bond_id, name, symbol, bond_type, issuer, maturity_date_offsets, interest_type, interest_index, interest_rate, archived = row
        maturity_date = get_dynamic_date(maturity_date_offsets[0], maturity_date_offsets[1])
        params = (bond_id, name, symbol, bond_type, issuer, maturity_date, interest_type, interest_index, interest_rate, archived)
        execute_query(cursor, "INSERT INTO bond (id, name, symbol, type, issuer, maturity_date, interest_type, interest_index, interest_rate, archived) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", params)
    print("Bonds inserted successfully.")

def populate_ticker_operations(cursor, data):
    """Insere as operações de compra e venda de tickers."""
    print("Inserting ticker operations...")
    
    # Insert ticker purchases
    for row in data["ticker_purchase"]:
        purchase_id, ticker_id, quantity, unit_price, wallet_id, date_offsets, description = row
        dynamic_date = get_dynamic_date(date_offsets[0], date_offsets[1])
        
        # Create wallet transaction for the purchase
        total_amount = quantity * unit_price
        wt_params = (None, wallet_id, 9, 'EXPENSE', 'CONFIRMED', dynamic_date, total_amount, description)
        execute_query(cursor, "INSERT INTO wallet_transaction (id, wallet_id, category_id, type, status, date, amount, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", wt_params)
        
        # Get the wallet_transaction_id
        cursor.execute("SELECT last_insert_rowid();")
        wt_id = cursor.fetchone()[0]
        
        # Insert ticker purchase
        tp_params = (purchase_id, quantity, unit_price, wt_id, ticker_id)
        execute_query(cursor, "INSERT INTO ticker_purchase (id, quantity, unit_price, wallet_transaction_id, ticker_id) VALUES (?, ?, ?, ?, ?);", tp_params)
    
    # Insert ticker sales
    for row in data["ticker_sale"]:
        sale_id, ticker_id, quantity, unit_price, average_cost, wallet_id, date_offsets, description = row
        dynamic_date = get_dynamic_date(date_offsets[0], date_offsets[1])
        
        # Create wallet transaction for the sale
        total_amount = quantity * unit_price
        wt_params = (None, wallet_id, 9, 'INCOME', 'CONFIRMED', dynamic_date, total_amount, description)
        execute_query(cursor, "INSERT INTO wallet_transaction (id, wallet_id, category_id, type, status, date, amount, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", wt_params)
        
        # Get the wallet_transaction_id
        cursor.execute("SELECT last_insert_rowid();")
        wt_id = cursor.fetchone()[0]
        
        # Insert ticker sale
        ts_params = (sale_id, quantity, unit_price, average_cost, wt_id, ticker_id)
        execute_query(cursor, "INSERT INTO ticker_sale (id, quantity, unit_price, average_cost, wallet_transaction_id, ticker_id) VALUES (?, ?, ?, ?, ?, ?);", ts_params)
    
    print("Ticker operations inserted successfully.")

def populate_bond_operations(cursor, data):
    """Insere as operações de bonds."""
    print("Inserting bond operations...")
    
    for row in data["bond_operation"]:
        operation_id, bond_id, operation_type, quantity, unit_price, fees, taxes, net_profit, wallet_id, date_offsets, description = row
        dynamic_date = get_dynamic_date(date_offsets[0], date_offsets[1])
        
        # Create wallet transaction for the operation
        total_amount = (quantity * unit_price) + (fees or 0.0)
        trans_type = 'EXPENSE' if operation_type == 'BUY' else 'INCOME'
        wt_params = (None, wallet_id, 9, trans_type, 'CONFIRMED', dynamic_date, total_amount, description)
        execute_query(cursor, "INSERT INTO wallet_transaction (id, wallet_id, category_id, type, status, date, amount, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", wt_params)
        
        # Get the wallet_transaction_id
        cursor.execute("SELECT last_insert_rowid();")
        wt_id = cursor.fetchone()[0]
        
        # Insert bond operation
        bo_params = (operation_id, bond_id, operation_type, quantity, unit_price, fees, taxes, net_profit, wt_id)
        execute_query(cursor, "INSERT INTO bond_operation (id, bond_id, operation_type, quantity, unit_price, fees, taxes, net_profit, wallet_transaction_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);", bo_params)
    
    print("Bond operations inserted successfully.")

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

        # Insere tickers e bonds
        populate_tickers(cursor, CONFIG["initial_data"])
        populate_bonds(cursor, CONFIG["initial_data"])

        # Gera as transações dinamicamente
        wallet_transactions = generate_wallet_transactions(
            CONFIG["initial_data"]["monthly_rules"],
            CONFIG["initial_data"]["possible_transactions"]
        )
        # Insere as transações geradas
        insert_wallet_transactions(cursor, wallet_transactions)

        # Insere operações de tickers e bonds
        populate_ticker_operations(cursor, CONFIG["initial_data"])
        populate_bond_operations(cursor, CONFIG["initial_data"])

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
