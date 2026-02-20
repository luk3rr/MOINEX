CREATE TABLE IF NOT EXISTS bond_interest_calculation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bond_id INTEGER NOT NULL,
    calculation_date VARCHAR(255) NOT NULL,
    start_date VARCHAR(255) NOT NULL,
    end_date VARCHAR(255) NOT NULL,
    quantity NUMERIC(38, 8) NOT NULL,
    invested_amount NUMERIC(38, 2) NOT NULL,
    accumulated_interest NUMERIC(38, 2) NOT NULL,
    final_value NUMERIC(38, 2) NOT NULL,
    calculation_method TEXT,
    created_at VARCHAR(255) NOT NULL,
    FOREIGN KEY (bond_id) REFERENCES bond(id) ON DELETE CASCADE
);

CREATE INDEX idx_bond_interest_bond_id ON bond_interest_calculation(bond_id);
CREATE INDEX idx_bond_interest_calc_date ON bond_interest_calculation(calculation_date);
CREATE UNIQUE INDEX idx_bond_interest_unique ON bond_interest_calculation(bond_id, calculation_date);
