-- Migration: Add source fields to recipes table
-- This script adds author, source, and page columns only if they don't exist

DO $$
BEGIN
    -- Add author column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'recipes' AND column_name = 'author'
    ) THEN
        ALTER TABLE recipes ADD COLUMN author VARCHAR(255);
    END IF;

    -- Add source column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'recipes' AND column_name = 'source'
    ) THEN
        ALTER TABLE recipes ADD COLUMN source VARCHAR(255);
    END IF;

    -- Add page column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'recipes' AND column_name = 'page'
    ) THEN
        ALTER TABLE recipes ADD COLUMN page VARCHAR(50);
    END IF;
END $$;
