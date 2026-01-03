CREATE TABLE IF NOT EXISTS investment_target (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL UNIQUE,
    target_percentage NUMERIC(38,2) NOT NULL CHECK(target_percentage >= 0 AND target_percentage <= 100),
    is_active BOOLEAN NOT NULL DEFAULT 1
);

INSERT INTO investment_target (type, target_percentage, is_active) VALUES
    ('STOCK', 30.00, 1),
    ('FUND', 30.00, 1),
    ('CRYPTOCURRENCY', 2.5, 1),
    ('BOND', 25.00, 1),
    ('REIT', 10.00, 1),
    ('ETF', 2.5, 1);
