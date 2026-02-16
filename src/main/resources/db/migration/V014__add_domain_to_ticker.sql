-- Add domain column to ticker table for storing company website domain
ALTER TABLE ticker ADD COLUMN domain VARCHAR(255);
