-- Add tracking_mode column to goal table
ALTER TABLE goal ADD COLUMN tracking_mode VARCHAR(20) DEFAULT 'WALLET' NOT NULL CHECK (tracking_mode IN ('WALLET', 'ASSET_ALLOCATION'));

-- Create index for performance
CREATE INDEX idx_goal_tracking_mode ON goal(tracking_mode);

-- Create goal_asset_allocation table
CREATE TABLE goal_asset_allocation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    goal_id INTEGER NOT NULL,
    asset_type VARCHAR(255) NOT NULL CHECK (asset_type IN ('BOND', 'TICKER')),
    asset_id INTEGER NOT NULL,
    allocation_type VARCHAR(255) NOT NULL CHECK (allocation_type IN ('PERCENTAGE', 'QUANTITY', 'VALUE')),
    allocation_value NUMERIC(255, 8) NOT NULL CHECK (allocation_value > 0),
    created_at VARCHAR(255),
    updated_at VARCHAR(255),
    FOREIGN KEY (goal_id) REFERENCES goal(wallet_id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES bond(id) ON DELETE CASCADE,
    
    CONSTRAINT uk_goal_asset
        UNIQUE (goal_id, asset_type, asset_id)
);

-- Create indexes for performance
CREATE INDEX idx_asset_lookup ON goal_asset_allocation(asset_type, asset_id);
CREATE INDEX idx_goal_lookup ON goal_asset_allocation(goal_id);

-- Add current_unit_value column to bond table
ALTER TABLE bond ADD COLUMN current_unit_value NUMERIC(38, 2);