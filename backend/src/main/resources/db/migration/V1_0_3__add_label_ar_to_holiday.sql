-- Migration to add Arabic label support for Holiday
ALTER TABLE holiday ADD COLUMN IF NOT EXISTS label_ar VARCHAR(255);
