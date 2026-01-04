-- Drop the old CHECK constraint and recreate the ticker table with updated constraint

-- Create a temporary table with the new constraint
CREATE TABLE ticker_new
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
    type                     VARCHAR(255)   NOT NULL CHECK (type IN ('STOCK', 'FUND', 'CRYPTOCURRENCY', 'REIT', 'ETF'))
);

-- Copy data from old table to new table
INSERT INTO ticker_new (id, average_unit_value, average_unit_value_count, current_quantity, current_unit_value, name, symbol, archived, last_update, type)
SELECT id, average_unit_value, average_unit_value_count, current_quantity, current_unit_value, name, symbol, archived, last_update, type
FROM ticker;

-- Drop the old table
DROP TABLE ticker;

-- Rename the new table to the original name
ALTER TABLE ticker_new RENAME TO ticker;
