-- ##############################################################################
-- ## Migration: Cria uma nova coluna 'category_id' na tabela transfers
-- ## Nota: Esta migração adiciona uma coluna 'category_id' à tabela de
-- ##       transferências, permitindo associar cada transferência a uma
-- ##       categoria específica. Esta coluna é opcional e pode ser nula,
-- ##       permitindo que as transferências existentes não sejam afetadas.
-- ##############################################################################


ALTER TABLE transfer
    ADD COLUMN category_id INTEGER DEFAULT NULL;