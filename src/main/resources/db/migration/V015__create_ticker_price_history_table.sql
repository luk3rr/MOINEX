-- Create ticker_price_history table to store historical price data
CREATE TABLE ticker_price_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticker_id INTEGER NOT NULL,
    price_date VARCHAR(255) NOT NULL,
    closing_price NUMERIC(38,2) NOT NULL,
    is_month_end BOOLEAN NOT NULL DEFAULT 0,
    FOREIGN KEY (ticker_id) REFERENCES ticker(id) ON DELETE CASCADE,
    UNIQUE(ticker_id, price_date)
);

-- Create index for faster queries by ticker and date
CREATE INDEX idx_ticker_price_history_ticker_date ON ticker_price_history(ticker_id, price_date);

-- Create index for month-end prices (used for monthly calculations)
CREATE INDEX idx_ticker_price_history_month_end ON ticker_price_history(ticker_id, is_month_end, price_date);
