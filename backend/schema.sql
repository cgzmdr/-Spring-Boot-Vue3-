-- ============================================================
-- 56_app 数据库结构快照（pg_dump --schema-only 自动导出）
-- 用途：新环境快速建表参考；若与代码迁移不一致，以 src/main/resources/db/migration 下的迁移脚本为准。
-- 重新导出：pg_dump -h <host> -U postgres -d 56_app --schema-only --no-owner --no-privileges -f schema.sql
-- ============================================================

--
-- PostgreSQL database dump
--

\restrict 1cL1SfnhI5c8Ap220BbI0PGHCVOqMdDZwyQBJjLUI7kynh4B6n4juPpFayWgbZ0

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: art; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.art (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug character varying(64) NOT NULL,
    ethnic_group_id uuid,
    name character varying(128) NOT NULL,
    name_en character varying(128),
    category character varying(20) NOT NULL,
    intangible_heritage character varying(20),
    description text,
    inheritors jsonb DEFAULT '[]'::jsonb NOT NULL,
    cover_image text,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    order_num integer DEFAULT 0 NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    origin text,
    CONSTRAINT art_category_check CHECK (((category)::text = ANY (ARRAY[('music'::character varying)::text, ('dance'::character varying)::text, ('drama'::character varying)::text, ('costume'::character varying)::text, ('craft'::character varying)::text, ('architecture'::character varying)::text]))),
    CONSTRAINT art_intangible_heritage_check CHECK (((intangible_heritage)::text = ANY (ARRAY[('world'::character varying)::text, ('national'::character varying)::text, ('provincial'::character varying)::text]))),
    CONSTRAINT art_status_check CHECK (((status)::text = ANY ((ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying])::text[])))
);


--
-- Name: TABLE art; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.art IS '传统艺术';


--
-- Name: content_review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_review (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entry_type character varying(20) NOT NULL,
    entry_id uuid NOT NULL,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    submitter_id uuid,
    reviewer_id uuid,
    reject_reason text,
    submitted_at timestamp with time zone DEFAULT now() NOT NULL,
    reviewed_at timestamp with time zone,
    CONSTRAINT content_review_status_check CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::text[])))
);


--
-- Name: TABLE content_review; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.content_review IS '内容审核流水';


--
-- Name: ethnic_custom; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ethnic_custom (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    ethnic_group_id uuid NOT NULL,
    category character varying(32),
    title character varying(128) NOT NULL,
    content text,
    image text,
    order_num integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE ethnic_custom; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ethnic_custom IS '民族风俗习惯';


--
-- Name: ethnic_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ethnic_group (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug character varying(64) NOT NULL,
    name character varying(64) NOT NULL,
    name_en character varying(64),
    self_name character varying(64),
    pinyin character varying(128),
    population bigint,
    language_family character varying(64),
    region jsonb DEFAULT '[]'::jsonb NOT NULL,
    languages jsonb DEFAULT '[]'::jsonb NOT NULL,
    scripts jsonb DEFAULT '[]'::jsonb NOT NULL,
    religion jsonb DEFAULT '[]'::jsonb NOT NULL,
    summary text,
    summary_en text,
    description text,
    description_en text,
    cover_image text,
    theme_color character varying(7),
    tags jsonb DEFAULT '[]'::jsonb NOT NULL,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    order_num integer DEFAULT 0 NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ethnic_group_status_check CHECK (((status)::text = ANY ((ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying])::text[])))
);


--
-- Name: TABLE ethnic_group; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ethnic_group IS '民族主表（56 个民族）';


--
-- Name: ethnic_location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ethnic_location (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    ethnic_group_id uuid NOT NULL,
    province character varying(64),
    city character varying(64),
    longitude numeric(10,7),
    latitude numeric(10,7),
    description text,
    order_num integer DEFAULT 0 NOT NULL
);


--
-- Name: TABLE ethnic_location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ethnic_location IS '民族聚居地地理坐标';


--
-- Name: favorite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.favorite (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    entry_type character varying(20) NOT NULL,
    entry_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE favorite; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.favorite IS '用户收藏';


--
-- Name: feedback; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feedback (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(64),
    contact character varying(128),
    topic character varying(32) NOT NULL,
    rating character varying(16),
    content text NOT NULL,
    visit_date character varying(32),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: festival; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.festival (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug character varying(64) NOT NULL,
    ethnic_group_id uuid,
    name character varying(128) NOT NULL,
    name_en character varying(128),
    type character varying(20) DEFAULT 'traditional'::character varying NOT NULL,
    solar_date date,
    lunar_date character varying(64),
    origin text,
    description text,
    customs jsonb DEFAULT '[]'::jsonb NOT NULL,
    images jsonb DEFAULT '[]'::jsonb NOT NULL,
    cover_image text,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    order_num integer DEFAULT 0 NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT festival_status_check CHECK (((status)::text = ANY ((ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying])::text[]))),
    CONSTRAINT festival_type_check CHECK (((type)::text = ANY (ARRAY[('traditional'::character varying)::text, ('religious'::character varying)::text, ('agricultural'::character varying)::text])))
);


--
-- Name: TABLE festival; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.festival IS '节日庆典';


--
-- Name: food; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.food (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    ethnic_group_id uuid NOT NULL,
    name character varying(128) NOT NULL,
    name_en character varying(128),
    description text,
    image text,
    order_num integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    origin text
);


--
-- Name: TABLE food; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.food IS '民族特色美食';


--
-- Name: form_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.form_config (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    description text,
    schema jsonb DEFAULT '{}'::jsonb NOT NULL,
    status character varying(20) DEFAULT 'active'::character varying NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT form_config_status_check CHECK (((status)::text = ANY (ARRAY[('active'::character varying)::text, ('disabled'::character varying)::text])))
);


--
-- Name: like_counter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.like_counter (
    entry_type character varying(20) NOT NULL,
    entry_id uuid NOT NULL,
    count bigint DEFAULT 0 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE like_counter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.like_counter IS '点赞计数（Redis 为热读主存储，此表持久化）';


--
-- Name: like_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.like_record (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    entry_type character varying(20) NOT NULL,
    entry_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE like_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.like_record IS '用户点赞';


--
-- Name: media_asset; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.media_asset (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_type character varying(20) NOT NULL,
    owner_id uuid NOT NULL,
    media_type character varying(20) DEFAULT 'image'::character varying NOT NULL,
    url text NOT NULL,
    title character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE media_asset; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.media_asset IS '图集与影像资源（多态关联）';


--
-- Name: permission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permission (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(64) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE permission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.permission IS 'RBAC 权限点';


--
-- Name: role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(64) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.role IS 'RBAC 角色';


--
-- Name: role_permission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permission (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


--
-- Name: topic; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.topic (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug character varying(64) NOT NULL,
    title character varying(128) NOT NULL,
    subtitle character varying(255),
    description text,
    cover_image text,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    order_num integer DEFAULT 0 NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT topic_status_check CHECK (((status)::text = ANY ((ARRAY['draft'::character varying, 'pending'::character varying, 'published'::character varying, 'offline'::character varying, 'rejected'::character varying])::text[])))
);


--
-- Name: TABLE topic; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.topic IS '专题';


--
-- Name: topic_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.topic_entry (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic_id uuid NOT NULL,
    entry_type character varying(20) NOT NULL,
    entry_id uuid NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);


--
-- Name: TABLE topic_entry; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.topic_entry IS '专题与条目关联';


--
-- Name: user_account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_account (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    account character varying(64) NOT NULL,
    password_hash character varying(128) NOT NULL,
    nickname character varying(64),
    avatar text,
    mobile character varying(20),
    email character varying(128),
    status character varying(20) DEFAULT 'active'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    lang character varying(8) DEFAULT 'zh'::character varying NOT NULL,
    security_question character varying(255),
    security_answer character varying(255),
    CONSTRAINT user_account_status_check CHECK (((status)::text = ANY (ARRAY[('active'::character varying)::text, ('disabled'::character varying)::text])))
);


--
-- Name: TABLE user_account; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_account IS '用户账号（C 端用户 + 后台管理员）';


--
-- Name: COLUMN user_account.security_question; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_account.security_question IS '密保问题（明文问题文本，为空表示未设置）';


--
-- Name: COLUMN user_account.security_answer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_account.security_answer IS '密保答案（AES 加密存储）';


--
-- Name: user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_role (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL
);


--
-- Name: view_counter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.view_counter (
    entry_type character varying(20) NOT NULL,
    entry_id uuid NOT NULL,
    count bigint DEFAULT 0 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: art art_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.art
    ADD CONSTRAINT art_pkey PRIMARY KEY (id);


--
-- Name: art art_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.art
    ADD CONSTRAINT art_slug_key UNIQUE (slug);


--
-- Name: content_review content_review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_review
    ADD CONSTRAINT content_review_pkey PRIMARY KEY (id);


--
-- Name: ethnic_custom ethnic_custom_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ethnic_custom
    ADD CONSTRAINT ethnic_custom_pkey PRIMARY KEY (id);


--
-- Name: ethnic_group ethnic_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ethnic_group
    ADD CONSTRAINT ethnic_group_pkey PRIMARY KEY (id);


--
-- Name: ethnic_group ethnic_group_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ethnic_group
    ADD CONSTRAINT ethnic_group_slug_key UNIQUE (slug);


--
-- Name: ethnic_location ethnic_location_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ethnic_location
    ADD CONSTRAINT ethnic_location_pkey PRIMARY KEY (id);


--
-- Name: favorite favorite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite
    ADD CONSTRAINT favorite_pkey PRIMARY KEY (id);


--
-- Name: favorite favorite_user_id_entry_type_entry_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite
    ADD CONSTRAINT favorite_user_id_entry_type_entry_id_key UNIQUE (user_id, entry_type, entry_id);


--
-- Name: feedback feedback_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback
    ADD CONSTRAINT feedback_pkey PRIMARY KEY (id);


--
-- Name: festival festival_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.festival
    ADD CONSTRAINT festival_pkey PRIMARY KEY (id);


--
-- Name: festival festival_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.festival
    ADD CONSTRAINT festival_slug_key UNIQUE (slug);


--
-- Name: food food_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.food
    ADD CONSTRAINT food_pkey PRIMARY KEY (id);


--
-- Name: form_config form_config_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.form_config
    ADD CONSTRAINT form_config_code_key UNIQUE (code);


--
-- Name: like_counter like_counter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.like_counter
    ADD CONSTRAINT like_counter_pkey PRIMARY KEY (entry_type, entry_id);


--
-- Name: like_record like_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.like_record
    ADD CONSTRAINT like_record_pkey PRIMARY KEY (id);


--
-- Name: like_record like_record_user_id_entry_type_entry_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.like_record
    ADD CONSTRAINT like_record_user_id_entry_type_entry_id_key UNIQUE (user_id, entry_type, entry_id);


--
-- Name: media_asset media_asset_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media_asset
    ADD CONSTRAINT media_asset_pkey PRIMARY KEY (id);


--
-- Name: permission permission_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_code_key UNIQUE (code);


--
-- Name: permission permission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (id);


--
-- Name: role role_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_code_key UNIQUE (code);


--
-- Name: role_permission role_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission
    ADD CONSTRAINT role_permission_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- Name: topic_entry topic_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic_entry
    ADD CONSTRAINT topic_entry_pkey PRIMARY KEY (id);


--
-- Name: topic_entry topic_entry_topic_id_entry_type_entry_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic_entry
    ADD CONSTRAINT topic_entry_topic_id_entry_type_entry_id_key UNIQUE (topic_id, entry_type, entry_id);


--
-- Name: topic topic_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic
    ADD CONSTRAINT topic_pkey PRIMARY KEY (id);


--
-- Name: topic topic_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic
    ADD CONSTRAINT topic_slug_key UNIQUE (slug);


--
-- Name: user_account user_account_account_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_account
    ADD CONSTRAINT user_account_account_key UNIQUE (account);


--
-- Name: user_account user_account_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_account
    ADD CONSTRAINT user_account_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: view_counter view_counter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.view_counter
    ADD CONSTRAINT view_counter_pkey PRIMARY KEY (entry_type, entry_id);


--
-- Name: idx_art_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_art_category ON public.art USING btree (category);


--
-- Name: idx_art_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_art_group ON public.art USING btree (ethnic_group_id);


--
-- Name: idx_art_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_art_status ON public.art USING btree (status);


--
-- Name: idx_ethnic_custom_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_custom_group ON public.ethnic_custom USING btree (ethnic_group_id);


--
-- Name: idx_ethnic_group_language_family; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_group_language_family ON public.ethnic_group USING btree (language_family);


--
-- Name: idx_ethnic_group_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_group_name_trgm ON public.ethnic_group USING gin (name public.gin_trgm_ops);


--
-- Name: idx_ethnic_group_pinyin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_group_pinyin ON public.ethnic_group USING btree (pinyin);


--
-- Name: idx_ethnic_group_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_group_status ON public.ethnic_group USING btree (status);


--
-- Name: idx_ethnic_group_tags; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_group_tags ON public.ethnic_group USING gin (tags);


--
-- Name: idx_ethnic_location_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ethnic_location_group ON public.ethnic_location USING btree (ethnic_group_id);


--
-- Name: idx_favorite_entry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favorite_entry ON public.favorite USING btree (entry_type, entry_id);


--
-- Name: idx_feedback_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_created_at ON public.feedback USING btree (created_at DESC);


--
-- Name: idx_festival_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_festival_group ON public.festival USING btree (ethnic_group_id);


--
-- Name: idx_festival_solar; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_festival_solar ON public.festival USING btree (solar_date);


--
-- Name: idx_festival_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_festival_status ON public.festival USING btree (status);


--
-- Name: idx_festival_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_festival_type ON public.festival USING btree (type);


--
-- Name: idx_food_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_food_group ON public.food USING btree (ethnic_group_id);


--
-- Name: idx_like_entry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_like_entry ON public.like_record USING btree (entry_type, entry_id);


--
-- Name: idx_media_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_owner ON public.media_asset USING btree (owner_type, owner_id);


--
-- Name: idx_review_entry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_entry ON public.content_review USING btree (entry_type, entry_id);


--
-- Name: idx_review_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_status ON public.content_review USING btree (status);


--
-- Name: idx_topic_entry_topic; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_topic_entry_topic ON public.topic_entry USING btree (topic_id);


--
-- Name: idx_topic_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_topic_status ON public.topic USING btree (status);


--
-- Name: art trg_art_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_art_updated BEFORE UPDATE ON public.art FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: ethnic_custom trg_ethnic_custom_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ethnic_custom_updated BEFORE UPDATE ON public.ethnic_custom FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: ethnic_group trg_ethnic_group_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ethnic_group_updated BEFORE UPDATE ON public.ethnic_group FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: festival trg_festival_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_festival_updated BEFORE UPDATE ON public.festival FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: food trg_food_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_food_updated BEFORE UPDATE ON public.food FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: like_counter trg_like_counter_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_like_counter_updated BEFORE UPDATE ON public.like_counter FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: permission trg_permission_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_permission_updated BEFORE UPDATE ON public.permission FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: role trg_role_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_role_updated BEFORE UPDATE ON public.role FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: topic trg_topic_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_topic_updated BEFORE UPDATE ON public.topic FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: user_account trg_user_account_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_user_account_updated BEFORE UPDATE ON public.user_account FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: view_counter trg_view_counter_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_view_counter_updated BEFORE UPDATE ON public.view_counter FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- PostgreSQL database dump complete
--

\unrestrict 1cL1SfnhI5c8Ap220BbI0PGHCVOqMdDZwyQBJjLUI7kynh4B6n4juPpFayWgbZ0

