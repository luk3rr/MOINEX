CREATE TABLE IF NOT EXISTS investment_performance_snapshot (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    invested_value NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    portfolio_value NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    accumulated_capital_gains NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    monthly_capital_gains NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    calculated_at TEXT NOT NULL,
    UNIQUE(month, year)
);

CREATE INDEX idx_investment_performance_snapshot_month_year ON investment_performance_snapshot(month, year);
