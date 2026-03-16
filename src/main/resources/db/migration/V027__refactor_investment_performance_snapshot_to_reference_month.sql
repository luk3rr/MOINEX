-- ##############################################################################
-- ## Migration: Refatora investment_performance_snapshot para usar reference_month
-- ## Nota: Esta migração converte as colunas month e year em um único campo
-- ##       reference_month no formato YYYY-MM. SQLite não permite remover
-- ##       colunas que fazem parte de constraints UNIQUE, então a tabela é
-- ##       recriada com a nova estrutura.
-- ##############################################################################

-- Deletar dados existentes de investment_performance_snapshot (podem ser recalculados)
DELETE FROM investment_performance_snapshot;

-- Remover o índice antigo
DROP INDEX IF EXISTS idx_investment_performance_snapshot_month_year;

-- Criar coluna temporária com reference_month
ALTER TABLE investment_performance_snapshot ADD COLUMN reference_month_new VARCHAR(7);

-- Popular reference_month_new a partir de month e year (formato: YYYY-MM)
UPDATE investment_performance_snapshot
SET reference_month_new = printf('%04d-%02d', year, month)
WHERE month IS NOT NULL AND year IS NOT NULL;

-- Recriar a tabela sem as colunas antigas (workaround SQLite para DROP COLUMN com constraint UNIQUE)
CREATE TABLE investment_performance_snapshot_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invested_value NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    portfolio_value NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    accumulated_capital_gains NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    monthly_capital_gains NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    calculated_at VARCHAR(255) NOT NULL,
    reference_month VARCHAR(7) NOT NULL,
    UNIQUE(reference_month)
);

INSERT INTO investment_performance_snapshot_new (id, invested_value, portfolio_value, accumulated_capital_gains, monthly_capital_gains, calculated_at, reference_month)
SELECT id, invested_value, portfolio_value, accumulated_capital_gains, monthly_capital_gains, calculated_at, reference_month_new
FROM investment_performance_snapshot;

DROP TABLE investment_performance_snapshot;

ALTER TABLE investment_performance_snapshot_new RENAME TO investment_performance_snapshot;

CREATE UNIQUE INDEX idx_investment_performance_snapshot_reference_month ON investment_performance_snapshot(reference_month);
