-- Migration to add isActive column to specialty table
ALTER TABLE specialty ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
