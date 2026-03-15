-- ##############################################################################
-- ## Migration: Refatora net_worth_snapshot para usar reference_month
-- ## Nota: Esta migração converte as colunas month e year em um único campo
-- ##       reference_month no formato YYYY-MM. SQLite não permite remover
-- ##       colunas que fazem parte de constraints UNIQUE, então a tabela é
-- ##       recriada com a nova estrutura.
-- ##############################################################################

-- Deletar dados existentes de net_worth_snapshot (podem ser recalculados)
DELETE FROM net_worth_snapshot;

-- Remover o índice antigo
DROP INDEX IF EXISTS idx_net_worth_snapshot_month_year;

-- Criar coluna temporária com reference_month
ALTER TABLE net_worth_snapshot ADD COLUMN reference_month_new VARCHAR(7);

-- Popular reference_month_new a partir de month e year (formato: YYYY-MM)
UPDATE net_worth_snapshot
SET reference_month_new = printf('%04d-%02d', year, month)
WHERE month IS NOT NULL AND year IS NOT NULL;

-- Recriar a tabela sem as colunas antigas (workaround SQLite para DROP COLUMN com constraint UNIQUE)
CREATE TABLE net_worth_snapshot_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    assets NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    liabilities NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    net_worth NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    wallet_balances NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    investments NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    credit_card_debt NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    negative_wallet_balances NUMERIC(38, 2) NOT NULL DEFAULT 0.0,
    calculated_at VARCHAR(255) NOT NULL,
    reference_month VARCHAR(7) NOT NULL,
    UNIQUE(reference_month)
);

INSERT INTO net_worth_snapshot_new (id, assets, liabilities, net_worth, wallet_balances, investments, credit_card_debt, negative_wallet_balances, calculated_at, reference_month)
SELECT id, assets, liabilities, net_worth, wallet_balances, investments, credit_card_debt, negative_wallet_balances, calculated_at, reference_month_new
FROM net_worth_snapshot;

DROP TABLE net_worth_snapshot;

ALTER TABLE net_worth_snapshot_new RENAME TO net_worth_snapshot;

CREATE UNIQUE INDEX idx_net_worth_snapshot_reference_month ON net_worth_snapshot(reference_month);
