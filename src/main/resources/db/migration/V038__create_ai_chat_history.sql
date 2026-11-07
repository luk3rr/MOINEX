-- Migration: V038__create_ai_chat_history.sql
-- Created on: November 6, 2026

CREATE TABLE IF NOT EXISTS ai_chat (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    title      VARCHAR(200) NOT NULL,
    created_at VARCHAR(255) NOT NULL,
    updated_at VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_updated_at ON ai_chat (updated_at);

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id              INTEGER      NOT NULL REFERENCES ai_chat (id),
    position             INTEGER      NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    content              TEXT         NOT NULL,
    display_text         TEXT,
    attachment_names     TEXT,
    tool_calls_json      TEXT,
    tool_call_id         VARCHAR(100),
    tool_name            VARCHAR(100),
    response_time_millis BIGINT,
    created_at           VARCHAR(255) NOT NULL,
    UNIQUE (chat_id, position)
);

CREATE TABLE IF NOT EXISTS ai_chat_change_set (
    id           VARCHAR(64) PRIMARY KEY,
    message_id   INTEGER NOT NULL REFERENCES ai_chat_message (id),
    ignored_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_change_set_message ON ai_chat_change_set (message_id);

CREATE TABLE IF NOT EXISTS ai_chat_change (
    id                    VARCHAR(64) PRIMARY KEY,
    change_set_id         VARCHAR(64)  NOT NULL REFERENCES ai_chat_change_set (id),
    position              INTEGER      NOT NULL,
    operation_name        VARCHAR(100) NOT NULL,
    payload_json          TEXT         NOT NULL,
    original_payload_json TEXT         NOT NULL,
    summary               VARCHAR(500),
    badge                 VARCHAR(30)  NOT NULL,
    reasons_json          TEXT,
    confidence            NUMERIC      NOT NULL,
    depends_on_json       TEXT,
    match_candidates_json TEXT,
    target_id             INTEGER,
    state                 VARCHAR(20)  NOT NULL,
    message               TEXT,
    decided_at            VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_change_change_set ON ai_chat_change (change_set_id);
