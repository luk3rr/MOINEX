-- Create net_worth_snapshot table for caching expensive PL calculations
CREATE TABLE IF NOT EXISTS net_worth_snapshot (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    assets NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    liabilities NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    net_worth NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    wallet_balances NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    investments NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    credit_card_debt NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    negative_wallet_balances NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    calculated_at VARCHAR(255) NOT NULL,
    UNIQUE(month, year)
);

CREATE INDEX IF NOT EXISTS idx_net_worth_snapshot_month_year ON net_worth_snapshot(month, year);
