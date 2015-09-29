CREATE TABLE ts_data.translation_key (
  "tk_id" BIGSERIAL NOT NULL PRIMARY KEY,
  "tk_name" TEXT NOT NULL,
  "tk_is_active" BOOLEAN NOT NULL,
  "tk_created" TIMESTAMP WITHOUT TIME ZONE DEFAULT CLOCK_TIMESTAMP()
);

CREATE UNIQUE INDEX ON ts_data.translation_key (tk_name, tk_is_active)
  WHERE tk_is_active;

CREATE TYPE ts_data.language_code as ENUM('en','de','it','fr','pl','tr','es');

CREATE TABLE ts_data.translation_message (
  "tm_id" BIGSERIAL NOT NULL PRIMARY KEY,
  "tm_translation_key_id" BIGINT NOT NULL REFERENCES ts_data.translation_key(tk_id),
  "tm_language_code" ts_data.language_code NOT NULL,
  "tm_value" TEXT NOT NULL,
  "tm_is_active" BOOLEAN NOT NULL,
  "tm_last_modified" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP(),
  "tm_created" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP()
);

CREATE UNIQUE INDEX ON ts_data.translation_message(tm_translation_key_id, tm_language_code, tm_is_active)
  WHERE tm_is_active;

CREATE TYPE ts_data.operation as ENUM('CREATED','MODIFIED','DELETED');

CREATE TABLE ts_data.version (
  "v_id" BIGSERIAL NOT NULL PRIMARY KEY,
  "v_name" TEXT NOT NULL,
  --"v_tagging_id" BIGINT NOT NULL,
  "v_translation_key_id" BIGINT NOT NULL REFERENCES ts_data.translation_key(tk_id),
  --"v_translation_message_id"  BIGINT NOT NULL REFERENCES ts_data.translation_message(tm_id),
  "v_performed_operation" ts_data.operation NOT NULL,
  "v_last_apply" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP(),
  "v_created" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP()
);

CREATE TABLE ts_data.tag(
  "t_id" BIGSERIAL NOT NULL PRIMARY KEY,
  "t_name" TEXT NOT NULL,
  "t_created" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP()
);

CREATE TABLE ts_data.translation_tagging(
  "tt_id" BIGSERIAL NOT NULL PRIMARY KEY,
  "tt_tag_id" BIGINT NOT NULL REFERENCES ts_data.tag(t_id),
  "tt_translation_key_id" BIGINT NOT NULL REFERENCES ts_data.translation_key(tk_id),
  "tt_is_active" BOOLEAN NOT NULL,
  "tt_last_modified" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP(),
  "tt_created" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CLOCK_TIMESTAMP()
);

CREATE UNIQUE INDEX ON ts_data.translation_tagging(tt_tag_id, tt_translation_key_id, tt_is_active)
  WHERE tt_is_active;


