-- ##############################################################################
-- ## Migration: Converte maturity_date de TIMESTAMP para DATE
-- ## Nota: Esta migração converte o campo de data da tabela bond de
-- ##       VARCHAR(255) contendo TIMESTAMP (LocalDateTime) para TEXT (LocalDate).
-- ##       Os dados existentes são convertidos extraindo apenas a parte da data.
-- ##       SQLite armazena datas como TEXT no formato ISO 8601 (YYYY-MM-DD).
-- ##############################################################################

-- Criar coluna temporária com o tipo TEXT
ALTER TABLE bond ADD COLUMN maturity_date_new TEXT;

-- Converter os dados existentes de TIMESTAMP para DATE
-- Extrai apenas a parte da data (YYYY-MM-DD) do formato TIMESTAMP (YYYY-MM-DDTHH:MM:SS)
UPDATE bond 
SET maturity_date_new = SUBSTR(maturity_date, 1, 10)
WHERE maturity_date IS NOT NULL;

-- Remover a coluna antiga
ALTER TABLE bond DROP COLUMN maturity_date;

-- Renomear a nova coluna para o nome original
ALTER TABLE bond RENAME COLUMN maturity_date_new TO maturity_date;
