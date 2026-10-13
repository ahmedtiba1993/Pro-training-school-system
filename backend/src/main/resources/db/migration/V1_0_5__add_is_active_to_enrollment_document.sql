-- Migration to add is_active column to enrollment_document table
ALTER TABLE enrollment_document 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_enrollment_document_is_active 
ON enrollment_document(is_active);
