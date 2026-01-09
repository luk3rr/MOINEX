-- Table for storing fundamental analysis data
CREATE TABLE IF NOT EXISTS fundamental_analysis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticker_id INTEGER NOT NULL UNIQUE,
    company_name VARCHAR(255),
    sector VARCHAR(255),
    industry VARCHAR(255),
    currency VARCHAR(255) NOT NULL,
    period_type VARCHAR(255) NOT NULL CHECK (period_type IN ('ANNUAL', 'QUARTERLY')),
    data_json TEXT NOT NULL,
    last_update VARCHAR(255) NOT NULL,
    created_at VARCHAR(255) NOT NULL,
    FOREIGN KEY (ticker_id) REFERENCES ticker(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_fundamental_analysis_ticker_id ON fundamental_analysis(ticker_id);
CREATE INDEX IF NOT EXISTS idx_fundamental_analysis_last_update ON fundamental_analysis(last_update);
