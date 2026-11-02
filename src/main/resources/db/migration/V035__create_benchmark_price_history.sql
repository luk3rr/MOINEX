CREATE TABLE IF NOT EXISTS benchmark_price_history (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol        VARCHAR(50)    NOT NULL,
    price_date    VARCHAR(255)   NOT NULL,
    closing_price NUMERIC(38, 4) NOT NULL,
    UNIQUE (symbol, price_date)
);

CREATE INDEX IF NOT EXISTS idx_benchmark_price_history_symbol_date ON benchmark_price_history (symbol, price_date);
