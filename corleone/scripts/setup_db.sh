#!/usr/bin/env bash
read -p "Insert database: " db
read -p "Insert port: " pt
psql --host=$db --port=$pt --username=postgres << EOF
    BEGIN;
    
        DROP DATABASE IF EXISTS translation_service_db;
        DROP ROLE IF EXISTS translation_service;
        
        CREATE ROLE translation_service WITH LOGIN PASSWORD 'translation_service';
        
        CREATE DATABASE translation_service_db;
        GRANT ALL PRIVILEGES ON DATABASE "translation_service_db" to translation_service;
        
        \connect translation_service_db;
        CREATE SCHEMA ts_data;
        GRANT ALL PRIVILEGES ON SCHEMA "ts_data" to translation_service;
    COMMIT;
    END;
--EOF--