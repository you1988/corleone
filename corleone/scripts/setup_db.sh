#!/usr/bin/env bash
read -p "Insert database: " db
read -p "Insert port: " pt
read -p "Insert username: " us
psql --host=$db --port=$pt --username=$us << EOF
     DROP DATABASE IF EXISTS translation_service_db;
        DROP ROLE IF EXISTS translation_service;
    
        
        CREATE ROLE translation_service WITH LOGIN PASSWORD 'translation_service';
        
        CREATE DATABASE translation_service_db;
        GRANT ALL PRIVILEGES ON DATABASE "translation_service_db" to translation_service;
        
        \connect translation_service_db;
        CREATE SCHEMA ts_data;
        GRANT ALL PRIVILEGES ON SCHEMA "ts_data" to translation_service;
--EOF--
