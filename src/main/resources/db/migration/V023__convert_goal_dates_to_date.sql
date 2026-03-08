-- ##############################################################################
-- ## Migration: Converte target_date e completion_date de TIMESTAMP para DATE
-- ## Nota: Esta migração converte os campos de data da tabela goal de
-- ##       VARCHAR(255) contendo TIMESTAMP (LocalDateTime) para TEXT (LocalDate).
-- ##       Os dados existentes são convertidos extraindo apenas a parte da data.
-- ##       SQLite armazena datas como TEXT no formato ISO 8601 (YYYY-MM-DD).
-- ##############################################################################

-- Criar colunas temporárias com o tipo TEXT
ALTER TABLE goal ADD COLUMN target_date_new TEXT;
ALTER TABLE goal ADD COLUMN completion_date_new TEXT;

-- Converter os dados existentes de TIMESTAMP para DATE
-- Extrai apenas a parte da data (YYYY-MM-DD) do formato TIMESTAMP (YYYY-MM-DDTHH:MM:SS)
UPDATE goal 
SET target_date_new = SUBSTR(target_date, 1, 10)
WHERE target_date IS NOT NULL;

UPDATE goal 
SET completion_date_new = SUBSTR(completion_date, 1, 10)
WHERE completion_date IS NOT NULL;

-- Remover as colunas antigas
ALTER TABLE goal DROP COLUMN target_date;
ALTER TABLE goal DROP COLUMN completion_date;

-- Renomear as novas colunas para os nomes originais
ALTER TABLE goal RENAME COLUMN target_date_new TO target_date;
ALTER TABLE goal RENAME COLUMN completion_date_new TO completion_date;
