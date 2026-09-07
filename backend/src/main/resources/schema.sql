/*
 Navicat Premium Dump SQL

 Source Server         : cz
 Source Server Type    : PostgreSQL
 Source Server Version : 180004 (180004)
 Source Host           : localhost:5432
 Source Catalog        : 56_app
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 180004 (180004)
 File Encoding         : 65001

 Date: 07/09/2026 14:58:05
*/


-- ----------------------------
-- Type structure for gtrgm
-- ----------------------------
DROP TYPE IF EXISTS "public"."gtrgm";
CREATE TYPE "public"."gtrgm" (
  INPUT = "public"."gtrgm_in",
  OUTPUT = "public"."gtrgm_out",
  INTERNALLENGTH = VARIABLE,
  CATEGORY = U,
  DELIMITER = ','
);

-- ----------------------------
-- Table structure for art
-- ----------------------------
DROP TABLE IF EXISTS "public"."art";
CREATE TABLE "public"."art" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "slug" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "ethnic_group_id" uuid,
  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(128) COLLATE "pg_catalog"."default",
  "category" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "intangible_heritage" varchar(20) COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "inheritors" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "cover_image" text COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'draft'::character varying,
  "order_num" int4 NOT NULL DEFAULT 0,
  "created_by" uuid,
  "updated_by" uuid,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "origin" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON TABLE "public"."art" IS '传统艺术';

-- ----------------------------
-- Table structure for content_review
-- ----------------------------
DROP TABLE IF EXISTS "public"."content_review";
CREATE TABLE "public"."content_review" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "entry_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" uuid NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'draft'::character varying,
  "submitter_id" uuid,
  "reviewer_id" uuid,
  "reject_reason" text COLLATE "pg_catalog"."default",
  "submitted_at" timestamptz(6) NOT NULL DEFAULT now(),
  "reviewed_at" timestamptz(6)
)
;
COMMENT ON TABLE "public"."content_review" IS '内容审核流水';

-- ----------------------------
-- Table structure for ethnic_custom
-- ----------------------------
DROP TABLE IF EXISTS "public"."ethnic_custom";
CREATE TABLE "public"."ethnic_custom" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "ethnic_group_id" uuid NOT NULL,
  "category" varchar(32) COLLATE "pg_catalog"."default",
  "title" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default",
  "image" text COLLATE "pg_catalog"."default",
  "order_num" int4 NOT NULL DEFAULT 0,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."ethnic_custom" IS '民族风俗习惯';

-- ----------------------------
-- Table structure for ethnic_group
-- ----------------------------
DROP TABLE IF EXISTS "public"."ethnic_group";
CREATE TABLE "public"."ethnic_group" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "slug" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(64) COLLATE "pg_catalog"."default",
  "self_name" varchar(64) COLLATE "pg_catalog"."default",
  "pinyin" varchar(128) COLLATE "pg_catalog"."default",
  "population" int8,
  "language_family" varchar(64) COLLATE "pg_catalog"."default",
  "region" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "languages" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "scripts" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "religion" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "summary" text COLLATE "pg_catalog"."default",
  "summary_en" text COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "description_en" text COLLATE "pg_catalog"."default",
  "cover_image" text COLLATE "pg_catalog"."default",
  "theme_color" varchar(7) COLLATE "pg_catalog"."default",
  "tags" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'draft'::character varying,
  "order_num" int4 NOT NULL DEFAULT 0,
  "created_by" uuid,
  "updated_by" uuid,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."ethnic_group" IS '民族主表（56 个民族）';

-- ----------------------------
-- Table structure for ethnic_location
-- ----------------------------
DROP TABLE IF EXISTS "public"."ethnic_location";
CREATE TABLE "public"."ethnic_location" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "ethnic_group_id" uuid NOT NULL,
  "province" varchar(64) COLLATE "pg_catalog"."default",
  "city" varchar(64) COLLATE "pg_catalog"."default",
  "longitude" numeric(10,7),
  "latitude" numeric(10,7),
  "description" text COLLATE "pg_catalog"."default",
  "order_num" int4 NOT NULL DEFAULT 0
)
;
COMMENT ON TABLE "public"."ethnic_location" IS '民族聚居地地理坐标';

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS "public"."favorite";
CREATE TABLE "public"."favorite" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "entry_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" uuid NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."favorite" IS '用户收藏';

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS "public"."feedback";
CREATE TABLE "public"."feedback" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "name" varchar(64) COLLATE "pg_catalog"."default",
  "contact" varchar(128) COLLATE "pg_catalog"."default",
  "topic" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "rating" varchar(16) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "visit_date" varchar(32) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for festival
-- ----------------------------
DROP TABLE IF EXISTS "public"."festival";
CREATE TABLE "public"."festival" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "slug" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "ethnic_group_id" uuid,
  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(128) COLLATE "pg_catalog"."default",
  "type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'traditional'::character varying,
  "solar_date" date,
  "lunar_date" varchar(64) COLLATE "pg_catalog"."default",
  "origin" text COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "customs" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "images" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "cover_image" text COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'draft'::character varying,
  "order_num" int4 NOT NULL DEFAULT 0,
  "created_by" uuid,
  "updated_by" uuid,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."festival" IS '节日庆典';

-- ----------------------------
-- Table structure for food
-- ----------------------------
DROP TABLE IF EXISTS "public"."food";
CREATE TABLE "public"."food" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "ethnic_group_id" uuid NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(128) COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "image" text COLLATE "pg_catalog"."default",
  "order_num" int4 NOT NULL DEFAULT 0,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "origin" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON TABLE "public"."food" IS '民族特色美食';

-- ----------------------------
-- Table structure for form_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."form_config";
CREATE TABLE "public"."form_config" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "schema" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'active'::character varying,
  "created_by" uuid,
  "updated_by" uuid,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for like_counter
-- ----------------------------
DROP TABLE IF EXISTS "public"."like_counter";
CREATE TABLE "public"."like_counter" (
  "entry_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" uuid NOT NULL,
  "count" int8 NOT NULL DEFAULT 0,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."like_counter" IS '点赞计数（Redis 为热读主存储，此表持久化）';

-- ----------------------------
-- Table structure for like_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."like_record";
CREATE TABLE "public"."like_record" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "entry_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" uuid NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."like_record" IS '用户点赞';

-- ----------------------------
-- Table structure for media_asset
-- ----------------------------
DROP TABLE IF EXISTS "public"."media_asset";
CREATE TABLE "public"."media_asset" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "owner_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "owner_id" uuid NOT NULL,
  "media_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'image'::character varying,
  "url" text COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "sort_order" int4 NOT NULL DEFAULT 0,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."media_asset" IS '图集与影像资源（多态关联）';

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."permission";
CREATE TABLE "public"."permission" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."permission" IS 'RBAC 权限点';

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS "public"."role";
CREATE TABLE "public"."role" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."role" IS 'RBAC 角色';

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."role_permission";
CREATE TABLE "public"."role_permission" (
  "role_id" uuid NOT NULL,
  "permission_id" uuid NOT NULL
)
;

-- ----------------------------
-- Table structure for topic
-- ----------------------------
DROP TABLE IF EXISTS "public"."topic";
CREATE TABLE "public"."topic" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "slug" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "subtitle" varchar(255) COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "cover_image" text COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'draft'::character varying,
  "order_num" int4 NOT NULL DEFAULT 0,
  "created_by" uuid,
  "updated_by" uuid,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "public"."topic" IS '专题';

-- ----------------------------
-- Table structure for topic_entry
-- ----------------------------
DROP TABLE IF EXISTS "public"."topic_entry";
CREATE TABLE "public"."topic_entry" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "topic_id" uuid NOT NULL,
  "entry_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" uuid NOT NULL,
  "sort_order" int4 NOT NULL DEFAULT 0
)
;
COMMENT ON TABLE "public"."topic_entry" IS '专题与条目关联';

-- ----------------------------
-- Table structure for user_account
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_account";
CREATE TABLE "public"."user_account" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "account" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "password_hash" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(64) COLLATE "pg_catalog"."default",
  "avatar" text COLLATE "pg_catalog"."default",
  "mobile" varchar(20) COLLATE "pg_catalog"."default",
  "email" varchar(128) COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'active'::character varying,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "lang" varchar(8) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'zh'::character varying,
  "security_question" varchar(255) COLLATE "pg_catalog"."default",
  "security_answer" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."user_account"."security_question" IS '密保问题（明文问题文本，为空表示未设置）';
COMMENT ON COLUMN "public"."user_account"."security_answer" IS '密保答案（AES 加密存储）';
COMMENT ON TABLE "public"."user_account" IS '用户账号（C 端用户 + 后台管理员）';

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_role";
CREATE TABLE "public"."user_role" (
  "user_id" uuid NOT NULL,
  "role_id" uuid NOT NULL
)
;

-- ----------------------------
-- Table structure for view_counter
-- ----------------------------
DROP TABLE IF EXISTS "public"."view_counter";
CREATE TABLE "public"."view_counter" (
  "entry_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" uuid NOT NULL,
  "count" int8 NOT NULL DEFAULT 0,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Function structure for armor
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."armor"(bytea);
CREATE FUNCTION "public"."armor"(bytea)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pg_armor'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for armor
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."armor"(bytea, _text, _text);
CREATE FUNCTION "public"."armor"(bytea, _text, _text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pg_armor'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for crypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."crypt"(text, text);
CREATE FUNCTION "public"."crypt"(text, text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pg_crypt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for dearmor
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."dearmor"(text);
CREATE FUNCTION "public"."dearmor"(text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_dearmor'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for decrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."decrypt"(bytea, bytea, text);
CREATE FUNCTION "public"."decrypt"(bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_decrypt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for decrypt_iv
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."decrypt_iv"(bytea, bytea, bytea, text);
CREATE FUNCTION "public"."decrypt_iv"(bytea, bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_decrypt_iv'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for digest
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."digest"(bytea, text);
CREATE FUNCTION "public"."digest"(bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_digest'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for digest
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."digest"(text, text);
CREATE FUNCTION "public"."digest"(text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_digest'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for encrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."encrypt"(bytea, bytea, text);
CREATE FUNCTION "public"."encrypt"(bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_encrypt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for encrypt_iv
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."encrypt_iv"(bytea, bytea, bytea, text);
CREATE FUNCTION "public"."encrypt_iv"(bytea, bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_encrypt_iv'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for fips_mode
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."fips_mode"();
CREATE FUNCTION "public"."fips_mode"()
  RETURNS "pg_catalog"."bool" AS '$libdir/pgcrypto', 'pg_check_fipsmode'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gen_random_bytes
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gen_random_bytes"(int4);
CREATE FUNCTION "public"."gen_random_bytes"(int4)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_random_bytes'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gen_random_uuid
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gen_random_uuid"();
CREATE FUNCTION "public"."gen_random_uuid"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/pgcrypto', 'pg_random_uuid'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for gen_salt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gen_salt"(text);
CREATE FUNCTION "public"."gen_salt"(text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pg_gen_salt'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gen_salt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gen_salt"(text, int4);
CREATE FUNCTION "public"."gen_salt"(text, int4)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pg_gen_salt_rounds'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_extract_query_trgm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_extract_query_trgm"(text, internal, int2, internal, internal, internal, internal);
CREATE FUNCTION "public"."gin_extract_query_trgm"(text, internal, int2, internal, internal, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gin_extract_query_trgm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_extract_value_trgm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_extract_value_trgm"(text, internal);
CREATE FUNCTION "public"."gin_extract_value_trgm"(text, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gin_extract_value_trgm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_trgm_consistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_trgm_consistent"(internal, int2, text, int4, internal, internal, internal, internal);
CREATE FUNCTION "public"."gin_trgm_consistent"(internal, int2, text, int4, internal, internal, internal, internal)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'gin_trgm_consistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_trgm_triconsistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_trgm_triconsistent"(internal, int2, text, int4, internal, internal, internal);
CREATE FUNCTION "public"."gin_trgm_triconsistent"(internal, int2, text, int4, internal, internal, internal)
  RETURNS "pg_catalog"."char" AS '$libdir/pg_trgm', 'gin_trgm_triconsistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_compress
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_compress"(internal);
CREATE FUNCTION "public"."gtrgm_compress"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_compress'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_consistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_consistent"(internal, text, int2, oid, internal);
CREATE FUNCTION "public"."gtrgm_consistent"(internal, text, int2, oid, internal)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'gtrgm_consistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_decompress
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_decompress"(internal);
CREATE FUNCTION "public"."gtrgm_decompress"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_decompress'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_distance"(internal, text, int2, oid, internal);
CREATE FUNCTION "public"."gtrgm_distance"(internal, text, int2, oid, internal)
  RETURNS "pg_catalog"."float8" AS '$libdir/pg_trgm', 'gtrgm_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_in"(cstring);
CREATE FUNCTION "public"."gtrgm_in"(cstring)
  RETURNS "public"."gtrgm" AS '$libdir/pg_trgm', 'gtrgm_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_options
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_options"(internal);
CREATE FUNCTION "public"."gtrgm_options"(internal)
  RETURNS "pg_catalog"."void" AS '$libdir/pg_trgm', 'gtrgm_options'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_out"("public"."gtrgm");
CREATE FUNCTION "public"."gtrgm_out"("public"."gtrgm")
  RETURNS "pg_catalog"."cstring" AS '$libdir/pg_trgm', 'gtrgm_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_penalty
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_penalty"(internal, internal, internal);
CREATE FUNCTION "public"."gtrgm_penalty"(internal, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_penalty'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_picksplit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_picksplit"(internal, internal);
CREATE FUNCTION "public"."gtrgm_picksplit"(internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_picksplit'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_same
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_same"("public"."gtrgm", "public"."gtrgm", internal);
CREATE FUNCTION "public"."gtrgm_same"("public"."gtrgm", "public"."gtrgm", internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_same'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_union
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_union"(internal, internal);
CREATE FUNCTION "public"."gtrgm_union"(internal, internal)
  RETURNS "public"."gtrgm" AS '$libdir/pg_trgm', 'gtrgm_union'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hmac
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hmac"(text, text, text);
CREATE FUNCTION "public"."hmac"(text, text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_hmac'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hmac
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hmac"(bytea, bytea, text);
CREATE FUNCTION "public"."hmac"(bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pg_hmac'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_armor_headers
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_armor_headers"(text, OUT "key" text, OUT "value" text);
CREATE FUNCTION "public"."pgp_armor_headers"(IN text, OUT "key" text, OUT "value" text)
  RETURNS SETOF "pg_catalog"."record" AS '$libdir/pgcrypto', 'pgp_armor_headers'
  LANGUAGE c IMMUTABLE STRICT
  COST 1
  ROWS 1000;

-- ----------------------------
-- Function structure for pgp_key_id
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_key_id"(bytea);
CREATE FUNCTION "public"."pgp_key_id"(bytea)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pgp_key_id_w'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_decrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_decrypt"(bytea, bytea, text);
CREATE FUNCTION "public"."pgp_pub_decrypt"(bytea, bytea, text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pgp_pub_decrypt_text'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_decrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_decrypt"(bytea, bytea);
CREATE FUNCTION "public"."pgp_pub_decrypt"(bytea, bytea)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pgp_pub_decrypt_text'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_decrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_decrypt"(bytea, bytea, text, text);
CREATE FUNCTION "public"."pgp_pub_decrypt"(bytea, bytea, text, text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pgp_pub_decrypt_text'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_decrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_decrypt_bytea"(bytea, bytea, text, text);
CREATE FUNCTION "public"."pgp_pub_decrypt_bytea"(bytea, bytea, text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_decrypt_bytea'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_decrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_decrypt_bytea"(bytea, bytea, text);
CREATE FUNCTION "public"."pgp_pub_decrypt_bytea"(bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_decrypt_bytea'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_decrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_decrypt_bytea"(bytea, bytea);
CREATE FUNCTION "public"."pgp_pub_decrypt_bytea"(bytea, bytea)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_decrypt_bytea'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_encrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_encrypt"(text, bytea);
CREATE FUNCTION "public"."pgp_pub_encrypt"(text, bytea)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_encrypt_text'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_encrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_encrypt"(text, bytea, text);
CREATE FUNCTION "public"."pgp_pub_encrypt"(text, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_encrypt_text'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_encrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_encrypt_bytea"(bytea, bytea);
CREATE FUNCTION "public"."pgp_pub_encrypt_bytea"(bytea, bytea)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_encrypt_bytea'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_pub_encrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_pub_encrypt_bytea"(bytea, bytea, text);
CREATE FUNCTION "public"."pgp_pub_encrypt_bytea"(bytea, bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_pub_encrypt_bytea'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_decrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_decrypt"(bytea, text);
CREATE FUNCTION "public"."pgp_sym_decrypt"(bytea, text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pgp_sym_decrypt_text'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_decrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_decrypt"(bytea, text, text);
CREATE FUNCTION "public"."pgp_sym_decrypt"(bytea, text, text)
  RETURNS "pg_catalog"."text" AS '$libdir/pgcrypto', 'pgp_sym_decrypt_text'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_decrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_decrypt_bytea"(bytea, text);
CREATE FUNCTION "public"."pgp_sym_decrypt_bytea"(bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_sym_decrypt_bytea'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_decrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_decrypt_bytea"(bytea, text, text);
CREATE FUNCTION "public"."pgp_sym_decrypt_bytea"(bytea, text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_sym_decrypt_bytea'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_encrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_encrypt"(text, text, text);
CREATE FUNCTION "public"."pgp_sym_encrypt"(text, text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_sym_encrypt_text'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_encrypt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_encrypt"(text, text);
CREATE FUNCTION "public"."pgp_sym_encrypt"(text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_sym_encrypt_text'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_encrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_encrypt_bytea"(bytea, text);
CREATE FUNCTION "public"."pgp_sym_encrypt_bytea"(bytea, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_sym_encrypt_bytea'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for pgp_sym_encrypt_bytea
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."pgp_sym_encrypt_bytea"(bytea, text, text);
CREATE FUNCTION "public"."pgp_sym_encrypt_bytea"(bytea, text, text)
  RETURNS "pg_catalog"."bytea" AS '$libdir/pgcrypto', 'pgp_sym_encrypt_bytea'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for set_limit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."set_limit"(float4);
CREATE FUNCTION "public"."set_limit"(float4)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'set_limit'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for set_updated_at
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."set_updated_at"();
CREATE FUNCTION "public"."set_updated_at"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for show_limit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."show_limit"();
CREATE FUNCTION "public"."show_limit"()
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'show_limit'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for show_trgm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."show_trgm"(text);
CREATE FUNCTION "public"."show_trgm"(text)
  RETURNS "pg_catalog"."_text" AS '$libdir/pg_trgm', 'show_trgm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for similarity
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."similarity"(text, text);
CREATE FUNCTION "public"."similarity"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'similarity'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for similarity_dist
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."similarity_dist"(text, text);
CREATE FUNCTION "public"."similarity_dist"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'similarity_dist'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for similarity_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."similarity_op"(text, text);
CREATE FUNCTION "public"."similarity_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'similarity_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity"(text, text);
CREATE FUNCTION "public"."strict_word_similarity"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'strict_word_similarity'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_commutator_op"(text, text);
CREATE FUNCTION "public"."strict_word_similarity_commutator_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'strict_word_similarity_commutator_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_dist_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_dist_commutator_op"(text, text);
CREATE FUNCTION "public"."strict_word_similarity_dist_commutator_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'strict_word_similarity_dist_commutator_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_dist_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_dist_op"(text, text);
CREATE FUNCTION "public"."strict_word_similarity_dist_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'strict_word_similarity_dist_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_op"(text, text);
CREATE FUNCTION "public"."strict_word_similarity_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'strict_word_similarity_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity"(text, text);
CREATE FUNCTION "public"."word_similarity"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'word_similarity'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_commutator_op"(text, text);
CREATE FUNCTION "public"."word_similarity_commutator_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'word_similarity_commutator_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_dist_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_dist_commutator_op"(text, text);
CREATE FUNCTION "public"."word_similarity_dist_commutator_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'word_similarity_dist_commutator_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_dist_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_dist_op"(text, text);
CREATE FUNCTION "public"."word_similarity_dist_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'word_similarity_dist_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_op"(text, text);
CREATE FUNCTION "public"."word_similarity_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'word_similarity_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Indexes structure for table art
-- ----------------------------
CREATE INDEX "idx_art_category" ON "public"."art" USING btree (
  "category" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_art_group" ON "public"."art" USING btree (
  "ethnic_group_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_art_status" ON "public"."art" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table art
-- ----------------------------
CREATE TRIGGER "trg_art_updated" BEFORE UPDATE ON "public"."art"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table art
-- ----------------------------
ALTER TABLE "public"."art" ADD CONSTRAINT "art_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Checks structure for table art
-- ----------------------------
ALTER TABLE "public"."art" ADD CONSTRAINT "art_intangible_heritage_check" CHECK (intangible_heritage::text = ANY (ARRAY['world'::character varying::text, 'national'::character varying::text, 'provincial'::character varying::text]));
ALTER TABLE "public"."art" ADD CONSTRAINT "art_status_check" CHECK (status::text = ANY (ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying]::text[]));
ALTER TABLE "public"."art" ADD CONSTRAINT "art_category_check" CHECK (category::text = ANY (ARRAY['music'::character varying::text, 'dance'::character varying::text, 'drama'::character varying::text, 'costume'::character varying::text, 'craft'::character varying::text, 'architecture'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table art
-- ----------------------------
ALTER TABLE "public"."art" ADD CONSTRAINT "art_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table content_review
-- ----------------------------
CREATE INDEX "idx_review_entry" ON "public"."content_review" USING btree (
  "entry_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "entry_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_review_status" ON "public"."content_review" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table content_review
-- ----------------------------
ALTER TABLE "public"."content_review" ADD CONSTRAINT "content_review_status_check" CHECK (status::text = ANY (ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table content_review
-- ----------------------------
ALTER TABLE "public"."content_review" ADD CONSTRAINT "content_review_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table ethnic_custom
-- ----------------------------
CREATE INDEX "idx_ethnic_custom_group" ON "public"."ethnic_custom" USING btree (
  "ethnic_group_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table ethnic_custom
-- ----------------------------
CREATE TRIGGER "trg_ethnic_custom_updated" BEFORE UPDATE ON "public"."ethnic_custom"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Primary Key structure for table ethnic_custom
-- ----------------------------
ALTER TABLE "public"."ethnic_custom" ADD CONSTRAINT "ethnic_custom_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table ethnic_group
-- ----------------------------
CREATE INDEX "idx_ethnic_group_language_family" ON "public"."ethnic_group" USING btree (
  "language_family" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_ethnic_group_name_trgm" ON "public"."ethnic_group" USING gin (
  "name" COLLATE "pg_catalog"."default" "public"."gin_trgm_ops"
);
CREATE INDEX "idx_ethnic_group_pinyin" ON "public"."ethnic_group" USING btree (
  "pinyin" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_ethnic_group_status" ON "public"."ethnic_group" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_ethnic_group_tags" ON "public"."ethnic_group" USING gin (
  "tags" "pg_catalog"."jsonb_ops"
);

-- ----------------------------
-- Triggers structure for table ethnic_group
-- ----------------------------
CREATE TRIGGER "trg_ethnic_group_updated" BEFORE UPDATE ON "public"."ethnic_group"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table ethnic_group
-- ----------------------------
ALTER TABLE "public"."ethnic_group" ADD CONSTRAINT "ethnic_group_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Checks structure for table ethnic_group
-- ----------------------------
ALTER TABLE "public"."ethnic_group" ADD CONSTRAINT "ethnic_group_status_check" CHECK (status::text = ANY (ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table ethnic_group
-- ----------------------------
ALTER TABLE "public"."ethnic_group" ADD CONSTRAINT "ethnic_group_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table ethnic_location
-- ----------------------------
CREATE INDEX "idx_ethnic_location_group" ON "public"."ethnic_location" USING btree (
  "ethnic_group_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table ethnic_location
-- ----------------------------
ALTER TABLE "public"."ethnic_location" ADD CONSTRAINT "ethnic_location_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table favorite
-- ----------------------------
CREATE INDEX "idx_favorite_entry" ON "public"."favorite" USING btree (
  "entry_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "entry_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table favorite
-- ----------------------------
ALTER TABLE "public"."favorite" ADD CONSTRAINT "favorite_user_id_entry_type_entry_id_key" UNIQUE ("user_id", "entry_type", "entry_id");

-- ----------------------------
-- Primary Key structure for table favorite
-- ----------------------------
ALTER TABLE "public"."favorite" ADD CONSTRAINT "favorite_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table feedback
-- ----------------------------
CREATE INDEX "idx_feedback_created_at" ON "public"."feedback" USING btree (
  "created_at" "pg_catalog"."timestamptz_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table feedback
-- ----------------------------
ALTER TABLE "public"."feedback" ADD CONSTRAINT "feedback_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table festival
-- ----------------------------
CREATE INDEX "idx_festival_group" ON "public"."festival" USING btree (
  "ethnic_group_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_festival_solar" ON "public"."festival" USING btree (
  "solar_date" "pg_catalog"."date_ops" ASC NULLS LAST
);
CREATE INDEX "idx_festival_status" ON "public"."festival" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_festival_type" ON "public"."festival" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table festival
-- ----------------------------
CREATE TRIGGER "trg_festival_updated" BEFORE UPDATE ON "public"."festival"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table festival
-- ----------------------------
ALTER TABLE "public"."festival" ADD CONSTRAINT "festival_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Checks structure for table festival
-- ----------------------------
ALTER TABLE "public"."festival" ADD CONSTRAINT "festival_status_check" CHECK (status::text = ANY (ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying]::text[]));
ALTER TABLE "public"."festival" ADD CONSTRAINT "festival_type_check" CHECK (type::text = ANY (ARRAY['traditional'::character varying::text, 'religious'::character varying::text, 'agricultural'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table festival
-- ----------------------------
ALTER TABLE "public"."festival" ADD CONSTRAINT "festival_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table food
-- ----------------------------
CREATE INDEX "idx_food_group" ON "public"."food" USING btree (
  "ethnic_group_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table food
-- ----------------------------
CREATE TRIGGER "trg_food_updated" BEFORE UPDATE ON "public"."food"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Primary Key structure for table food
-- ----------------------------
ALTER TABLE "public"."food" ADD CONSTRAINT "food_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table form_config
-- ----------------------------
ALTER TABLE "public"."form_config" ADD CONSTRAINT "form_config_code_key" UNIQUE ("code");

-- ----------------------------
-- Checks structure for table form_config
-- ----------------------------
ALTER TABLE "public"."form_config" ADD CONSTRAINT "form_config_status_check" CHECK (status::text = ANY (ARRAY['active'::character varying::text, 'disabled'::character varying::text]));

-- ----------------------------
-- Triggers structure for table like_counter
-- ----------------------------
CREATE TRIGGER "trg_like_counter_updated" BEFORE UPDATE ON "public"."like_counter"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Primary Key structure for table like_counter
-- ----------------------------
ALTER TABLE "public"."like_counter" ADD CONSTRAINT "like_counter_pkey" PRIMARY KEY ("entry_type", "entry_id");

-- ----------------------------
-- Indexes structure for table like_record
-- ----------------------------
CREATE INDEX "idx_like_entry" ON "public"."like_record" USING btree (
  "entry_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "entry_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table like_record
-- ----------------------------
ALTER TABLE "public"."like_record" ADD CONSTRAINT "like_record_user_id_entry_type_entry_id_key" UNIQUE ("user_id", "entry_type", "entry_id");

-- ----------------------------
-- Primary Key structure for table like_record
-- ----------------------------
ALTER TABLE "public"."like_record" ADD CONSTRAINT "like_record_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table media_asset
-- ----------------------------
CREATE INDEX "idx_media_owner" ON "public"."media_asset" USING btree (
  "owner_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "owner_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table media_asset
-- ----------------------------
ALTER TABLE "public"."media_asset" ADD CONSTRAINT "media_asset_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Triggers structure for table permission
-- ----------------------------
CREATE TRIGGER "trg_permission_updated" BEFORE UPDATE ON "public"."permission"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table permission
-- ----------------------------
ALTER TABLE "public"."permission" ADD CONSTRAINT "permission_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table permission
-- ----------------------------
ALTER TABLE "public"."permission" ADD CONSTRAINT "permission_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Triggers structure for table role
-- ----------------------------
CREATE TRIGGER "trg_role_updated" BEFORE UPDATE ON "public"."role"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table role
-- ----------------------------
ALTER TABLE "public"."role" ADD CONSTRAINT "role_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table role
-- ----------------------------
ALTER TABLE "public"."role" ADD CONSTRAINT "role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table role_permission
-- ----------------------------
ALTER TABLE "public"."role_permission" ADD CONSTRAINT "role_permission_pkey" PRIMARY KEY ("role_id", "permission_id");

-- ----------------------------
-- Indexes structure for table topic
-- ----------------------------
CREATE INDEX "idx_topic_status" ON "public"."topic" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table topic
-- ----------------------------
CREATE TRIGGER "trg_topic_updated" BEFORE UPDATE ON "public"."topic"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table topic
-- ----------------------------
ALTER TABLE "public"."topic" ADD CONSTRAINT "topic_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Checks structure for table topic
-- ----------------------------
ALTER TABLE "public"."topic" ADD CONSTRAINT "topic_status_check" CHECK (status::text = ANY (ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table topic
-- ----------------------------
ALTER TABLE "public"."topic" ADD CONSTRAINT "topic_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table topic_entry
-- ----------------------------
CREATE INDEX "idx_topic_entry_topic" ON "public"."topic_entry" USING btree (
  "topic_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table topic_entry
-- ----------------------------
ALTER TABLE "public"."topic_entry" ADD CONSTRAINT "topic_entry_topic_id_entry_type_entry_id_key" UNIQUE ("topic_id", "entry_type", "entry_id");

-- ----------------------------
-- Primary Key structure for table topic_entry
-- ----------------------------
ALTER TABLE "public"."topic_entry" ADD CONSTRAINT "topic_entry_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Triggers structure for table user_account
-- ----------------------------
CREATE TRIGGER "trg_user_account_updated" BEFORE UPDATE ON "public"."user_account"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Uniques structure for table user_account
-- ----------------------------
ALTER TABLE "public"."user_account" ADD CONSTRAINT "user_account_account_key" UNIQUE ("account");

-- ----------------------------
-- Checks structure for table user_account
-- ----------------------------
ALTER TABLE "public"."user_account" ADD CONSTRAINT "user_account_status_check" CHECK (status::text = ANY (ARRAY['active'::character varying::text, 'disabled'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table user_account
-- ----------------------------
ALTER TABLE "public"."user_account" ADD CONSTRAINT "user_account_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table user_role
-- ----------------------------
ALTER TABLE "public"."user_role" ADD CONSTRAINT "user_role_pkey" PRIMARY KEY ("user_id", "role_id");

-- ----------------------------
-- Triggers structure for table view_counter
-- ----------------------------
CREATE TRIGGER "trg_view_counter_updated" BEFORE UPDATE ON "public"."view_counter"
FOR EACH ROW
EXECUTE PROCEDURE "public"."set_updated_at"();

-- ----------------------------
-- Primary Key structure for table view_counter
-- ----------------------------
ALTER TABLE "public"."view_counter" ADD CONSTRAINT "view_counter_pkey" PRIMARY KEY ("entry_type", "entry_id");
