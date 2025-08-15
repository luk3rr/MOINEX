-- =================================================================
-- TABELAS SEM DEPENDÊNCIAS EXTERNAS
-- =================================================================

CREATE TABLE category
(
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    archived BOOLEAN     NOT NULL,
    name     VARCHAR(50) NOT NULL
);

CREATE TABLE credit_card_operator
(
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    icon VARCHAR(30),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE wallet_type
(
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    icon VARCHAR(30),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE ticker
(
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    average_unit_value       NUMERIC(38, 2) NOT NULL,
    average_unit_value_count NUMERIC(38, 2) NOT NULL,
    current_quantity         NUMERIC(38, 2) NOT NULL,
    current_unit_value       NUMERIC(38, 2) NOT NULL,
    name                     VARCHAR(255)   NOT NULL,
    symbol                   VARCHAR(255)   NOT NULL UNIQUE,
    archived                 BOOLEAN        NOT NULL DEFAULT FALSE,
    last_update              VARCHAR(255)   NOT NULL,
    type                     VARCHAR(255)   NOT NULL CHECK (type IN ('STOCK', 'FUND', 'CRYPTOCURRENCY'))
);

CREATE TABLE bond
(
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    average_unit_value       NUMERIC(38, 2) NOT NULL,
    average_unit_value_count NUMERIC(38, 2) NOT NULL,
    current_quantity         NUMERIC(38, 2) NOT NULL,
    current_unit_value       NUMERIC(38, 2) NOT NULL,
    name                     VARCHAR(255)   NOT NULL,
    symbol                   VARCHAR(255)   NOT NULL UNIQUE,
    maturity_date            VARCHAR(255),
    archived                 BOOLEAN        NOT NULL DEFAULT FALSE,
    interest_index           TINYINT        NOT NULL CHECK (interest_index BETWEEN 0 AND 5),
    interest_rate            NUMERIC(38, 2) NOT NULL,
    interest_type            TINYINT        NOT NULL CHECK (interest_type BETWEEN 0 AND 2),
    type                     VARCHAR(255)   NOT NULL CHECK (type IN ('CDB', 'LCI', 'LCA', 'TREASURY_PREFIXED',
                                                                     'TREASURY_POSTFIXED', 'INTERNATIONAL', 'OTHER'))
);

CREATE TABLE brazilian_market_indicators
(
    id                        INTEGER PRIMARY KEY AUTOINCREMENT,
    ipca_12_months            NUMERIC(38, 2),
    ipca_last_month_rate      NUMERIC(38, 2),
    ipca_last_month_reference VARCHAR(255),
    last_update               VARCHAR(255),
    selic_target              NUMERIC(38, 2)
);

CREATE TABLE calendar_event
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    date        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    event_type  VARCHAR(255) NOT NULL CHECK (event_type IN ('CREDIT_CARD_STATEMENT_CLOSING', 'CREDIT_CARD_DUE_DATE',
                                                            'DEBT_PAYMENT_DUE_DATE', 'INCOME_RECEIPT_DATE')),
    title       VARCHAR(255) NOT NULL
);

CREATE TABLE market_quotes_and_commodities
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    bitcoin     NUMERIC(38, 2),
    coffee      NUMERIC(38, 2),
    dollar      NUMERIC(38, 2),
    ethereum    NUMERIC(38, 2),
    euro        NUMERIC(38, 2),
    gold        NUMERIC(38, 2),
    ibovespa    NUMERIC(38, 2),
    last_update TIMESTAMP,
    oil_brent   NUMERIC(38, 2),
    soybean     NUMERIC(38, 2),
    wheat       NUMERIC(38, 2)
);


-- =================================================================
-- TABELAS COM DEPENDÊNCIAS
-- =================================================================

CREATE TABLE wallet
(
    id               INTEGER               NOT NULL PRIMARY KEY AUTOINCREMENT,
    archived         BOOLEAN DEFAULT FALSE NOT NULL,
    balance          NUMERIC(38, 2)        NOT NULL,
    name             VARCHAR(50)           NOT NULL UNIQUE,
    type_id          INTEGER,
    master_wallet_id INTEGER,
    CONSTRAINT fk_wallet_type FOREIGN KEY (type_id) REFERENCES wallet_type (id),
    CONSTRAINT fk_master_wallet FOREIGN KEY (master_wallet_id) REFERENCES wallet (id)
);

CREATE TABLE wallet_transaction
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    amount      NUMERIC(38, 2) NOT NULL,
    description VARCHAR(255),
    type        VARCHAR(255)   NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    date        VARCHAR(255)   NOT NULL,
    status      VARCHAR(255)   NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED')),
    category_id INTEGER        NOT NULL,
    wallet_id   INTEGER        NOT NULL,
    CONSTRAINT fk_wallet_transaction_to_category
        FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_wallet_transaction_to_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

CREATE TABLE credit_card
(
    id                        INTEGER PRIMARY KEY AUTOINCREMENT,
    archived                  BOOLEAN        NOT NULL DEFAULT FALSE,
    billing_due_day           INTEGER        NOT NULL,
    closing_day               INTEGER        NOT NULL,
    last_four_digits          VARCHAR(4),
    max_debt                  NUMERIC(38, 2) NOT NULL,
    name                      VARCHAR(50)    NOT NULL UNIQUE,
    available_rebate          NUMERIC(38, 2) NOT NULL DEFAULT 0.00,
    default_billing_wallet_id INTEGER,
    operator_id               INTEGER,
    CONSTRAINT fk_credit_card_to_wallet
        FOREIGN KEY (default_billing_wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_credit_card_to_operator
        FOREIGN KEY (operator_id) REFERENCES credit_card_operator (id)
);

CREATE TABLE credit_card_debt
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    date         VARCHAR(255)   NOT NULL,
    description  VARCHAR(255),
    installments INTEGER        NOT NULL,
    amount       NUMERIC(38, 2) NOT NULL,
    category_id  INTEGER        NOT NULL,
    crc_id       INTEGER        NOT NULL,
    CONSTRAINT fk_credit_card_debt_to_category
        FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_credit_card_debt_to_credit_card
        FOREIGN KEY (crc_id) REFERENCES credit_card (id)
);

CREATE TABLE credit_card_payment
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    amount      NUMERIC(38, 2) NOT NULL,
    date        VARCHAR(255)   NOT NULL,
    installment INTEGER        NOT NULL,
    rebate_used NUMERIC(38, 2) NOT NULL DEFAULT 0.00,
    debt_id     INTEGER        NOT NULL,
    wallet_id   INTEGER,
    CONSTRAINT fk_credit_card_payment_to_debt
        FOREIGN KEY (debt_id) REFERENCES credit_card_debt (id),
    CONSTRAINT fk_credit_card_payment_to_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

CREATE TABLE credit_card_credit
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    amount      NUMERIC(38, 2) NOT NULL,
    date        VARCHAR(255)   NOT NULL,
    description VARCHAR(255),
    type        VARCHAR(255)   NOT NULL,
    crc_id      INTEGER        NOT NULL,
    CONSTRAINT fk_credit_card_credit_to_credit_card
        FOREIGN KEY (crc_id) REFERENCES credit_card (id)
);

CREATE TABLE recurring_transaction
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    amount        NUMERIC(38, 2) NOT NULL,
    description   VARCHAR(255),
    type          VARCHAR(255)   NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    end_date      VARCHAR(255)   NOT NULL,
    frequency     VARCHAR(255)   NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    next_due_date VARCHAR(255)   NOT NULL,
    start_date    VARCHAR(255)   NOT NULL,
    status        VARCHAR(255)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    category_id   INTEGER        NOT NULL,
    wallet_id     INTEGER        NOT NULL,
    CONSTRAINT fk_recurring_transaction_to_category
        FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_recurring_transaction_to_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

CREATE TABLE transfer
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    amount             NUMERIC(38, 2) NOT NULL,
    date               VARCHAR(255)   NOT NULL,
    description        VARCHAR(255),
    receiver_wallet_id INTEGER        NOT NULL,
    sender_wallet_id   INTEGER        NOT NULL,
    CONSTRAINT fk_transfer_to_receiver_wallet
        FOREIGN KEY (receiver_wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_transfer_to_sender_wallet
        FOREIGN KEY (sender_wallet_id) REFERENCES wallet (id)
);

CREATE TABLE goal
(
    wallet_id       INTEGER        NOT NULL,
    completion_date VARCHAR(255),
    initial_balance NUMERIC(38, 2) NOT NULL,
    motivation      VARCHAR(500),
    target_balance  NUMERIC(38, 2) NOT NULL,
    target_date     VARCHAR(255)   NOT NULL,
    PRIMARY KEY (wallet_id),
    CONSTRAINT fk_goal_to_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

CREATE TABLE bond_purchase
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    quantity              NUMERIC(20, 8) NOT NULL,
    unit_price            NUMERIC(38, 2) NOT NULL,
    wallet_transaction_id INTEGER        NOT NULL,
    bond_id               INTEGER,
    CONSTRAINT fk_bond_purchase_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id),
    CONSTRAINT fk_bond_purchase_to_bond
        FOREIGN KEY (bond_id) REFERENCES bond (id)
);

CREATE TABLE bond_sale
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    quantity              NUMERIC(20, 8) NOT NULL,
    unit_price            NUMERIC(38, 2) NOT NULL,
    wallet_transaction_id INTEGER        NOT NULL,
    bond_id               INTEGER,
    CONSTRAINT fk_bond_sale_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id),
    CONSTRAINT fk_bond_sale_to_bond
        FOREIGN KEY (bond_id) REFERENCES bond (id)
);

CREATE TABLE crypto_exchange
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    date               VARCHAR(255)   NOT NULL,
    description        VARCHAR(255),
    received_quantity  NUMERIC(38, 2) NOT NULL,
    sold_quantity      NUMERIC(38, 2) NOT NULL,
    received_crypto_id INTEGER        NOT NULL,
    sold_crypto_id     INTEGER        NOT NULL,
    CONSTRAINT fk_crypto_exchange_to_received_crypto
        FOREIGN KEY (received_crypto_id) REFERENCES ticker (id),
    CONSTRAINT fk_crypto_exchange_to_sold_crypto
        FOREIGN KEY (sold_crypto_id) REFERENCES ticker (id)
);

CREATE TABLE dividend
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    ticker_id             INTEGER NOT NULL,
    wallet_transaction_id INTEGER NOT NULL,
    CONSTRAINT fk_dividend_to_ticker
        FOREIGN KEY (ticker_id) REFERENCES ticker (id),
    CONSTRAINT fk_dividend_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id)
);

CREATE TABLE purchase
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    quantity              NUMERIC(20, 8) NOT NULL,
    unit_price            NUMERIC(38, 2) NOT NULL,
    ticker_id             INTEGER,
    wallet_transaction_id INTEGER        NOT NULL,
    CONSTRAINT fk_purchase_to_ticker
        FOREIGN KEY (ticker_id) REFERENCES ticker (id),
    CONSTRAINT fk_purchase_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id)
);

CREATE TABLE sale
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    quantity              NUMERIC(20, 8) NOT NULL,
    unit_price            NUMERIC(38, 2) NOT NULL,
    average_cost          NUMERIC(38, 2) NOT NULL,
    ticker_id             INTEGER,
    wallet_transaction_id INTEGER        NOT NULL,
    CONSTRAINT fk_sale_to_ticker
        FOREIGN KEY (ticker_id) REFERENCES ticker (id),
    CONSTRAINT fk_sale_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id)
);

CREATE TABLE ticker_purchase
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    quantity              NUMERIC(20, 8) NOT NULL,
    unit_price            NUMERIC(38, 2) NOT NULL,
    wallet_transaction_id INTEGER        NOT NULL,
    ticker_id             INTEGER,
    CONSTRAINT fk_ticker_purchase_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id),
    CONSTRAINT fk_ticker_purchase_to_ticker
        FOREIGN KEY (ticker_id) REFERENCES ticker (id)
);

CREATE TABLE ticker_sale
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    quantity              NUMERIC(20, 8) NOT NULL,
    unit_price            NUMERIC(38, 2) NOT NULL,
    average_cost          NUMERIC(38, 2) NOT NULL,
    wallet_transaction_id INTEGER        NOT NULL,
    ticker_id             INTEGER,
    CONSTRAINT fk_ticker_sale_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id),
    CONSTRAINT fk_ticker_sale_to_ticker
        FOREIGN KEY (ticker_id) REFERENCES ticker (id)
);