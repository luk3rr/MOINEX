CREATE TABLE IF NOT EXISTS notification (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    type              TEXT    NOT NULL,
    title             TEXT    NOT NULL,
    message           TEXT    NOT NULL,
    status            TEXT    NOT NULL DEFAULT 'UNREAD',
    created_at        TEXT    NOT NULL,
    related_entity_id INTEGER
);

CREATE INDEX IF NOT EXISTS idx_notification_status     ON notification(status);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notification(created_at DESC);