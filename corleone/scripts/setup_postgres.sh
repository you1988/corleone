#!/usr/bin/env bash
read -p "Insert database: " db
read -p "Insert port: " pt
psql --host=$db --port=$pt --username=postgres << EOF
    BEGIN;
        ALTER ROLE postgres
        CREATE LANGUAGE plpythonu;
        SET search_path TO public;
        CREATE AGGREGATE array_agg_cat(anyarray) ( SFUNC=array_cat, STYPE=anyarray, INITCOND='{}' );

        CREATE EXTENSION HSTORE;
        CREATE AGGREGATE hstore_agg(hstore) ( sfunc = hs_concat, stype = hstore, initcond = '' );
        CREATE EXTENSION pg_trgm;
        CREATE EXTENSION intarray;
        CREATE EXTENSION btree_gist;
        CREATE EXTENSION btree_gin;
        CREATE EXTENSION fuzzystrmatch;
    
    COMMIT;
    END;
--EOF--