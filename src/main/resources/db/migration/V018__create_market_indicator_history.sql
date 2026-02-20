CREATE TABLE IF NOT EXISTS market_indicator_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    indicator_type VARCHAR(50) NOT NULL,
    reference_date VARCHAR(255) NOT NULL,
    rate_value NUMERIC(38, 8) NOT NULL,
    created_at VARCHAR(255) NOT NULL,
    UNIQUE(indicator_type, reference_date)
);

CREATE INDEX idx_market_indicator_type ON market_indicator_history(indicator_type);
CREATE INDEX idx_market_indicator_date ON market_indicator_history(reference_date);
CREATE INDEX idx_market_indicator_type_date ON market_indicator_history(indicator_type, reference_date);
