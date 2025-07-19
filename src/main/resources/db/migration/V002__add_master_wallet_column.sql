-- #############################################################################
-- ## Migration: Adiciona suporte para Carteiras Mestre (Saldos Compartilhados)
-- ## Nota: Este script segue o procedimento recomendado pelo SQLite para adicionar
-- ##       uma chave estrangeira a uma tabela existente, pois o comando
-- ##       'ALTER TABLE ... ADD CONSTRAINT' não é suportado.
-- #############################################################################

PRAGMA
foreign_keys=off;

ALTER TABLE wallet RENAME TO _wallet_old;

CREATE TABLE wallet
(
    id               INTEGER               NOT NULL PRIMARY KEY AUTOINCREMENT,
    archived         BOOLEAN DEFAULT FALSE NOT NULL,
    balance          NUMERIC(38, 2)        NOT NULL,
    name             VARCHAR(50)           NOT NULL UNIQUE,
    type_id          INTEGER,
    master_wallet_id INTEGER,
    CONSTRAINT fk_wallet_type FOREIGN KEY (type_id) REFERENCES wallet_type (id),
    CONSTRAINT fk_master_wallet FOREIGN KEY (master_wallet_id) REFERENCES wallet (id)
);

INSERT INTO wallet (id, archived, balance, name, type_id, master_wallet_id)
SELECT id, archived, balance, name, type_id, NULL
FROM _wallet_old;

DROP TABLE _wallet_old;

PRAGMA
foreign_keys=on;
