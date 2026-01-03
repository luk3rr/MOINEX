-- ============================================================================
-- PASSO 1: Dropar tabelas antigas
-- ============================================================================

DROP TABLE IF EXISTS bond_purchase;
DROP TABLE IF EXISTS bond_sale;
DROP TABLE IF EXISTS bond;

-- ============================================================================
-- PASSO 2: Criar nova tabela bond com estrutura simplificada
-- ============================================================================

CREATE TABLE bond (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    issuer VARCHAR(255),
    maturity_date VARCHAR(255),
    interest_type VARCHAR(255),
    interest_index VARCHAR(255),
    interest_rate NUMERIC(38, 4),
    archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_bond_name ON bond(name);
CREATE INDEX idx_bond_type ON bond(type);
CREATE INDEX idx_bond_archived ON bond(archived);

-- ============================================================================
-- PASSO 3: Criar nova tabela bond_operation
-- ============================================================================

CREATE TABLE bond_operation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bond_id INTEGER NOT NULL,
    operation_type VARCHAR(255) NOT NULL CHECK (operation_type IN ('BUY', 'SELL')),
    quantity NUMERIC(38, 8) NOT NULL,
    unit_price NUMERIC(38, 2) NOT NULL,
    fees NUMERIC(38, 2),
    taxes NUMERIC(38, 2),
    net_profit NUMERIC(38, 2),
    wallet_transaction_id INTEGER,
    FOREIGN KEY (bond_id) REFERENCES bond(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction(id) ON DELETE SET NULL
);

CREATE INDEX idx_bond_operation_bond_id ON bond_operation(bond_id);
CREATE INDEX idx_bond_operation_type ON bond_operation(operation_type);
