-- ##############################################################################
-- ## Migration: Converte start_date, end_date e next_due_date de TIMESTAMP para DATE
-- ## Nota: Esta migração converte os campos de data da tabela recurring_transaction de
-- ##       VARCHAR(255) contendo TIMESTAMP (LocalDateTime) para TEXT (LocalDate).
-- ##       Os dados existentes são convertidos extraindo apenas a parte da data.
-- ##       SQLite armazena datas como TEXT no formato ISO 8601 (YYYY-MM-DD).
-- ##############################################################################

-- Criar colunas temporárias com o tipo TEXT
ALTER TABLE recurring_transaction ADD COLUMN start_date_new TEXT;
ALTER TABLE recurring_transaction ADD COLUMN end_date_new TEXT;
ALTER TABLE recurring_transaction ADD COLUMN next_due_date_new TEXT;

-- Converter os dados existentes de TIMESTAMP para DATE
-- Extrai apenas a parte da data (YYYY-MM-DD) do formato TIMESTAMP (YYYY-MM-DDTHH:MM:SS)
UPDATE recurring_transaction 
SET start_date_new = SUBSTR(start_date, 1, 10)
WHERE start_date IS NOT NULL;

UPDATE recurring_transaction 
SET end_date_new = SUBSTR(end_date, 1, 10)
WHERE end_date IS NOT NULL;

UPDATE recurring_transaction 
SET next_due_date_new = SUBSTR(next_due_date, 1, 10)
WHERE next_due_date IS NOT NULL;

-- Remover as colunas antigas
ALTER TABLE recurring_transaction DROP COLUMN start_date;
ALTER TABLE recurring_transaction DROP COLUMN end_date;
ALTER TABLE recurring_transaction DROP COLUMN next_due_date;

-- Renomear as novas colunas para os nomes originais
ALTER TABLE recurring_transaction RENAME COLUMN start_date_new TO start_date;
ALTER TABLE recurring_transaction RENAME COLUMN end_date_new TO end_date;
ALTER TABLE recurring_transaction RENAME COLUMN next_due_date_new TO next_due_date;
