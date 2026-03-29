CREATE TABLE wishlist_item
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    title                 VARCHAR(255)   NOT NULL,
    estimated_price       NUMERIC(38, 2) NOT NULL,
    target_date           VARCHAR(255),
    status                VARCHAR(255)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PURCHASED')),
    priority              VARCHAR(255)   NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    notes                 VARCHAR(1000),
    category_id           INTEGER        NOT NULL,
    created_at            VARCHAR(255)   NOT NULL,
    purchased_at          VARCHAR(255),
    wallet_transaction_id INTEGER,
    credit_card_debt_id   INTEGER,
    CONSTRAINT fk_wishlist_item_to_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_wishlist_item_to_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_wishlist_item_to_credit_card_debt
        FOREIGN KEY (credit_card_debt_id) REFERENCES credit_card_debt (id)
            ON DELETE SET NULL
);

CREATE TABLE wishlist_item_link
(
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    url              VARCHAR(1000) NOT NULL,
    label            VARCHAR(100),
    wishlist_item_id INTEGER      NOT NULL,
    CONSTRAINT fk_wishlist_item_link_to_wishlist_item
        FOREIGN KEY (wishlist_item_id) REFERENCES wishlist_item (id)
            ON DELETE CASCADE
);
