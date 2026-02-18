-- Migration to add created_at column to ticker table
-- This column stores when the ticker was first registered in the system

ALTER TABLE ticker ADD COLUMN created_at VARCHAR(255);

UPDATE ticker SET created_at = strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime') WHERE created_at IS NULL;
