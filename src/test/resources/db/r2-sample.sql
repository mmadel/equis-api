-- A release 2 (2.0.2) database, for MigrationFromRelease2IT: the V1-V22 migrations with their Flyway history,
-- the seeds of V2, V4, V5 (the test admin of config/application.properties), V8 and V12, and a row or more in every
-- school table (trainers, invites, templates, sessions, members, bookings, a placed rider with an account, a seat,
-- an evaluation, a level change, a payment, a used session, a notice and refresh tokens).
-- Made with pg_dump --inserts --no-owner --no-privileges --no-comments from PostgreSQL 17; psql's \restrict lines
-- removed so JDBC can run it. Restore it as the database owner.
--
-- PostgreSQL database dump
--


-- Dumped from database version 17.11 (Debian 17.11-1.pgdg13+2)
-- Dumped by pg_dump version 17.11 (Debian 17.11-1.pgdg13+2)

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin_account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_account (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(100) NOT NULL,
    failed_attempts integer DEFAULT 0 NOT NULL,
    locked_until timestamp with time zone,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_admin_account_email CHECK ((((email)::text = lower((email)::text)) AND (POSITION(('@'::text) IN (email)) > 1))),
    CONSTRAINT ck_admin_account_failed_attempts CHECK ((failed_attempts >= 0))
);


--
-- Name: assessment_booking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_booking (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    member_id uuid NOT NULL,
    session_id uuid NOT NULL,
    assessor_id uuid NOT NULL,
    status character varying(16) DEFAULT 'upcoming'::character varying NOT NULL,
    attended boolean,
    level_id smallint,
    notes character varying(500),
    recorded_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    cancel_reason character varying(32),
    cancel_reason_text character varying(200),
    CONSTRAINT ck_assessment_booking_cancel_reason CHECK (((cancel_reason IS NULL) OR ((cancel_reason)::text = ANY ((ARRAY['person_cancelled'::character varying, 'person_unwell'::character varying, 'trainer_unavailable'::character varying, 'weather'::character varying, 'horse_unavailable'::character varying, 'facility_closed'::character varying, 'other'::character varying])::text[])))),
    CONSTRAINT ck_assessment_booking_cancel_status CHECK (((cancel_reason IS NULL) OR ((status)::text = 'cancelled'::text))),
    CONSTRAINT ck_assessment_booking_cancel_text CHECK (((((cancel_reason)::text = 'other'::text) AND (cancel_reason_text IS NOT NULL) AND (btrim((cancel_reason_text)::text) <> ''::text)) OR (((cancel_reason)::text IS DISTINCT FROM 'other'::text) AND (cancel_reason_text IS NULL)))),
    CONSTRAINT ck_assessment_booking_result CHECK (((((status)::text = 'upcoming'::text) AND (attended IS NULL) AND (level_id IS NULL) AND (recorded_at IS NULL) AND (cancelled_at IS NULL)) OR (((status)::text = 'placed'::text) AND (attended = true) AND (level_id IS NOT NULL) AND (recorded_at IS NOT NULL) AND (cancelled_at IS NULL)) OR (((status)::text = 'noshow'::text) AND (attended = false) AND (level_id IS NULL) AND (recorded_at IS NOT NULL) AND (cancelled_at IS NULL)) OR (((status)::text = 'cancelled'::text) AND (attended IS NULL) AND (level_id IS NULL) AND (recorded_at IS NULL) AND (cancelled_at IS NOT NULL)))),
    CONSTRAINT ck_assessment_booking_status CHECK (((status)::text = ANY ((ARRAY['upcoming'::character varying, 'placed'::character varying, 'noshow'::character varying, 'cancelled'::character varying])::text[])))
);


--
-- Name: assignment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assignment (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id uuid NOT NULL,
    rider_id uuid NOT NULL,
    assigned_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: invite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invite (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    trainer_id uuid,
    token_hash character varying(64) NOT NULL,
    purpose character varying(16) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    accepted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    rider_id uuid,
    CONSTRAINT ck_invite_accepted CHECK (((accepted_at IS NULL) OR (accepted_at >= created_at))),
    CONSTRAINT ck_invite_expiry CHECK ((expires_at > created_at)),
    CONSTRAINT ck_invite_owner CHECK (((trainer_id IS NULL) <> (rider_id IS NULL))),
    CONSTRAINT ck_invite_purpose CHECK (((purpose)::text = ANY ((ARRAY['invite'::character varying, 'reset'::character varying])::text[]))),
    CONSTRAINT ck_invite_token_hash CHECK (((token_hash)::text ~ '^[0-9a-f]{64}$'::text))
);


--
-- Name: level; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.level (
    id smallint NOT NULL,
    name character varying(50) NOT NULL,
    colour character varying(7) NOT NULL,
    CONSTRAINT ck_level_colour CHECK (((colour)::text ~ '^#[0-9A-Fa-f]{6}$'::text)),
    CONSTRAINT ck_level_id CHECK (((id >= 1) AND (id <= 6)))
);


--
-- Name: level_change; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.level_change (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rider_id uuid NOT NULL,
    from_level_id smallint NOT NULL,
    to_level_id smallint NOT NULL,
    reason character varying(100) NOT NULL,
    by_trainer_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_level_change_one_step CHECK ((abs((to_level_id - from_level_id)) = 1)),
    CONSTRAINT ck_level_change_reason CHECK ((btrim((reason)::text) <> ''::text))
);


--
-- Name: member; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.member (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(11) NOT NULL,
    joined_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    email character varying(254) NOT NULL,
    closed_at timestamp with time zone,
    CONSTRAINT ck_member_email_lower CHECK (((email)::text = lower((email)::text))),
    CONSTRAINT ck_member_email_shape CHECK (((email)::text ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'::text)),
    CONSTRAINT ck_member_name CHECK ((btrim((name)::text) <> ''::text)),
    CONSTRAINT ck_member_phone CHECK (((phone IS NULL) OR ((phone)::text ~ '^0[0-9]{10}$'::text)))
);


--
-- Name: notice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notice (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    trainer_id uuid NOT NULL,
    type character varying(32) NOT NULL,
    ref_id uuid,
    message character varying(300) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    read_at timestamp with time zone,
    CONSTRAINT ck_notice_message CHECK ((btrim((message)::text) <> ''::text)),
    CONSTRAINT ck_notice_type CHECK (((type)::text = ANY ((ARRAY['result_overdue'::character varying, 'session_cancelled'::character varying])::text[])))
);


--
-- Name: payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rider_id uuid NOT NULL,
    amount_egp numeric(10,2) NOT NULL,
    method character varying(20) NOT NULL,
    start_month date NOT NULL,
    months smallint NOT NULL,
    sessions smallint NOT NULL,
    valid_until date NOT NULL,
    recorded_at timestamp with time zone DEFAULT now() NOT NULL,
    recorded_by uuid NOT NULL,
    voided_at timestamp with time zone,
    voided_by uuid,
    void_reason character varying(300),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_payment_amount CHECK ((amount_egp > (0)::numeric)),
    CONSTRAINT ck_payment_method CHECK (((method)::text = ANY ((ARRAY['cash'::character varying, 'card'::character varying, 'bank_transfer'::character varying, 'mobile_wallet'::character varying, 'other'::character varying])::text[]))),
    CONSTRAINT ck_payment_months CHECK (((months >= 1) AND (months <= 12))),
    CONSTRAINT ck_payment_sessions CHECK (((sessions >= 1) AND (sessions <= 200))),
    CONSTRAINT ck_payment_start_month CHECK ((EXTRACT(day FROM start_month) = (1)::numeric)),
    CONSTRAINT ck_payment_valid_until CHECK ((valid_until = (((start_month + make_interval(months => (months + 1))))::date - 1))),
    CONSTRAINT ck_payment_void CHECK ((((voided_at IS NULL) AND (voided_by IS NULL) AND (void_reason IS NULL)) OR ((voided_at IS NOT NULL) AND (voided_by IS NOT NULL) AND (length(TRIM(BOTH FROM void_reason)) > 0))))
);


--
-- Name: refresh_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_token (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    subject_type character varying(16) NOT NULL,
    subject_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    remember_me boolean DEFAULT false NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    revoked_at timestamp with time zone,
    replaced_by uuid,
    CONSTRAINT ck_refresh_token_expiry CHECK ((expires_at > created_at)),
    CONSTRAINT ck_refresh_token_revoked CHECK (((revoked_at IS NULL) OR (revoked_at >= created_at))),
    CONSTRAINT ck_refresh_token_subject_type CHECK (((subject_type)::text = ANY ((ARRAY['trainer'::character varying, 'admin'::character varying, 'rider'::character varying])::text[]))),
    CONSTRAINT ck_refresh_token_token_hash CHECK (((token_hash)::text ~ '^[0-9a-f]{64}$'::text))
);


--
-- Name: rider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rider (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(11),
    level_id smallint NOT NULL,
    trainer_id uuid,
    member_id uuid,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    email character varying(254),
    password_hash character varying(100),
    account_status character varying(16) DEFAULT 'none'::character varying NOT NULL,
    last_active_at timestamp with time zone,
    CONSTRAINT ck_rider_account_email CHECK ((((account_status)::text = 'none'::text) OR (email IS NOT NULL))),
    CONSTRAINT ck_rider_account_status CHECK (((account_status)::text = ANY ((ARRAY['none'::character varying, 'invited'::character varying, 'active'::character varying])::text[]))),
    CONSTRAINT ck_rider_active_password CHECK ((((account_status)::text <> 'active'::text) OR (password_hash IS NOT NULL))),
    CONSTRAINT ck_rider_email CHECK (((email IS NULL) OR (((email)::text = lower((email)::text)) AND (POSITION(('@'::text) IN (email)) > 1)))),
    CONSTRAINT ck_rider_phone CHECK (((phone IS NULL) OR ((phone)::text ~ '^0[0-9]{10}$'::text)))
);


--
-- Name: session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_id uuid NOT NULL,
    session_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    capacity smallint NOT NULL,
    status character varying(16) DEFAULT 'scheduled'::character varying NOT NULL,
    cancelled_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    finished_at timestamp with time zone,
    finished_by uuid,
    CONSTRAINT ck_session_cancelled CHECK ((((status)::text = 'cancelled'::text) = (cancelled_at IS NOT NULL))),
    CONSTRAINT ck_session_capacity CHECK (((capacity >= 1) AND (capacity <= 12))),
    CONSTRAINT ck_session_finished CHECK (((((status)::text = ANY ((ARRAY['finished'::character varying, 'done'::character varying])::text[])) AND (finished_at IS NOT NULL) AND (finished_by IS NOT NULL)) OR (((status)::text <> ALL ((ARRAY['finished'::character varying, 'done'::character varying])::text[])) AND (finished_at IS NULL) AND (finished_by IS NULL)))),
    CONSTRAINT ck_session_status CHECK (((status)::text = ANY ((ARRAY['scheduled'::character varying, 'cancelled'::character varying, 'finished'::character varying, 'done'::character varying])::text[]))),
    CONSTRAINT ck_session_times CHECK ((end_time > start_time))
);


--
-- Name: session_evaluation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session_evaluation (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id uuid NOT NULL,
    rider_id uuid NOT NULL,
    trainer_id uuid NOT NULL,
    stars smallint NOT NULL,
    comment character varying(500),
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT ck_session_evaluation_comment CHECK (((comment IS NULL) OR (length(btrim((comment)::text)) > 0))),
    CONSTRAINT ck_session_evaluation_stars CHECK (((stars >= 1) AND (stars <= 5)))
);


--
-- Name: session_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session_template (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    level_id smallint,
    day_of_week smallint NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    capacity smallint NOT NULL,
    valid_from date NOT NULL,
    valid_to date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT ck_session_template_capacity CHECK (((capacity >= 1) AND (capacity <= 12))),
    CONSTRAINT ck_session_template_day CHECK (((day_of_week >= 1) AND (day_of_week <= 7))),
    CONSTRAINT ck_session_template_times CHECK ((end_time > start_time)),
    CONSTRAINT ck_session_template_validity CHECK (((valid_to IS NULL) OR (valid_to >= valid_from)))
);


--
-- Name: session_usage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session_usage (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rider_id uuid NOT NULL,
    payment_id uuid NOT NULL,
    session_id uuid NOT NULL,
    used_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: trainer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trainer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(11) NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(100),
    status character varying(16) DEFAULT 'invited'::character varying NOT NULL,
    last_active_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_trainer_active_password CHECK ((((status)::text <> 'active'::text) OR (password_hash IS NOT NULL))),
    CONSTRAINT ck_trainer_email CHECK ((((email)::text = lower((email)::text)) AND (POSITION(('@'::text) IN (email)) > 1))),
    CONSTRAINT ck_trainer_name CHECK ((length(TRIM(BOTH FROM name)) > 0)),
    CONSTRAINT ck_trainer_phone CHECK (((phone)::text ~ '^0[0-9]{10}$'::text)),
    CONSTRAINT ck_trainer_status CHECK (((status)::text = ANY ((ARRAY['invited'::character varying, 'active'::character varying, 'disabled'::character varying])::text[])))
);


--
-- Data for Name: admin_account; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.admin_account VALUES ('b30cb8e5-8e49-44f9-8df2-f228c40b44e9', 'trainer-accounts-admin@example.com', '$2a$10$2QypuqWXbYQIqoQbaNJV.Oj6KhEoRYT8Wgv1Ii/iyiV5CkeALH9.G', 0, NULL, NULL, '2026-11-08 05:52:51.998478+00', '2026-11-08 05:52:51.998478+00');


--
-- Data for Name: assessment_booking; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.assessment_booking VALUES ('60000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'placed', true, 1, NULL, '2026-09-04 09:10:00+00', NULL, 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', NULL, NULL);
INSERT INTO public.assessment_booking VALUES ('60000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'upcoming', NULL, NULL, NULL, NULL, NULL, 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', NULL, NULL);
INSERT INTO public.assessment_booking VALUES ('60000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'cancelled', NULL, NULL, NULL, NULL, '2026-09-03 07:00:00+00', 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', 'person_cancelled', NULL);


--
-- Data for Name: assignment; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.assignment VALUES ('75bf27fd-b04b-4e1e-9ab8-8757e37d4ef7', '40000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', '2026-11-08 05:52:52.492568+00');


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.flyway_schema_history VALUES (1, '1', 'create level', 'SQL', 'V1__create_level.sql', -1615516701, 'test', '2026-11-08 07:52:51.831014', 13, true);
INSERT INTO public.flyway_schema_history VALUES (2, '2', 'seed levels', 'SQL', 'V2__seed_levels.sql', -1692383344, 'test', '2026-11-08 07:52:51.885962', 6, true);
INSERT INTO public.flyway_schema_history VALUES (3, '3', 'create accounts', 'SQL', 'V3__create_accounts.sql', -1701859786, 'test', '2026-11-08 07:52:51.91547', 18, true);
INSERT INTO public.flyway_schema_history VALUES (4, '4', 'seed test trainers', 'SQL', 'V4__seed_test_trainers.sql', 625667190, 'test', '2026-11-08 07:52:51.964135', 8, true);
INSERT INTO public.flyway_schema_history VALUES (5, '5', 'seed admin', 'SQL', 'V5__seed_admin.sql', -1351229897, 'test', '2026-11-08 07:52:51.989409', 8, true);
INSERT INTO public.flyway_schema_history VALUES (6, '6', 'create refresh token', 'SQL', 'V6__create_refresh_token.sql', 1241921630, 'test', '2026-11-08 07:52:52.014779', 10, true);
INSERT INTO public.flyway_schema_history VALUES (7, '7', 'create session template', 'SQL', 'V7__create_session_template.sql', 1952103733, 'test', '2026-11-08 07:52:52.039886', 8, true);
INSERT INTO public.flyway_schema_history VALUES (8, '8', 'seed sample templates', 'SQL', 'V8__seed_sample_templates.sql', -803946233, 'test', '2026-11-08 07:52:52.062867', 6, true);
INSERT INTO public.flyway_schema_history VALUES (9, '9', 'create session', 'SQL', 'V9__create_session.sql', 964883117, 'test', '2026-11-08 07:52:52.083191', 9, true);
INSERT INTO public.flyway_schema_history VALUES (10, '10', 'create rider', 'SQL', 'V10__create_rider.sql', -1743449009, 'test', '2026-11-08 07:52:52.106125', 9, true);
INSERT INTO public.flyway_schema_history VALUES (11, '11', 'create assignment', 'SQL', 'V11__create_assignment.sql', 340811727, 'test', '2026-11-08 07:52:52.131048', 8, true);
INSERT INTO public.flyway_schema_history VALUES (12, '12', 'seed sample riders', 'SQL', 'V12__seed_sample_riders.sql', -1311989071, 'test', '2026-11-08 07:52:52.152757', 5, true);
INSERT INTO public.flyway_schema_history VALUES (13, '13', 'create member and assessment booking', 'SQL', 'V13__create_member_and_assessment_booking.sql', 1779782217, 'test', '2026-11-08 07:52:52.169576', 15, true);
INSERT INTO public.flyway_schema_history VALUES (14, '14', 'create level change', 'SQL', 'V14__create_level_change.sql', 139958457, 'test', '2026-11-08 07:52:52.201103', 9, true);
INSERT INTO public.flyway_schema_history VALUES (15, '15', 'create notice', 'SQL', 'V15__create_notice.sql', -21361822, 'test', '2026-11-08 07:52:52.223514', 9, true);
INSERT INTO public.flyway_schema_history VALUES (16, '16', 'applicant intake', 'SQL', 'V16__applicant_intake.sql', -1812832471, 'test', '2026-11-08 07:52:52.245645', 10, true);
INSERT INTO public.flyway_schema_history VALUES (17, '17', 'member closed', 'SQL', 'V17__member_closed.sql', -1603861572, 'test', '2026-11-08 07:52:52.2713', 6, true);
INSERT INTO public.flyway_schema_history VALUES (18, '18', 'assessment cancel reason', 'SQL', 'V18__assessment_cancel_reason.sql', 854052586, 'test', '2026-11-08 07:52:52.292044', 6, true);
INSERT INTO public.flyway_schema_history VALUES (19, '19', 'payments', 'SQL', 'V19__payments.sql', -400021933, 'test', '2026-11-08 07:52:52.311884', 12, true);
INSERT INTO public.flyway_schema_history VALUES (20, '20', 'session finished', 'SQL', 'V20__session_finished.sql', 1998002356, 'test', '2026-11-08 07:52:52.33885', 13, true);
INSERT INTO public.flyway_schema_history VALUES (21, '21', 'session evaluation', 'SQL', 'V21__session_evaluation.sql', 1235508020, 'test', '2026-11-08 07:52:52.366276', 15, true);
INSERT INTO public.flyway_schema_history VALUES (22, '22', 'rider accounts', 'SQL', 'V22__rider_accounts.sql', 262097043, 'test', '2026-11-08 07:52:52.398082', 26, true);


--
-- Data for Name: invite; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.invite VALUES ('92947d83-ced5-43ef-88f2-df9a6b71b943', '00000000-0000-0000-0000-000000000002', '7b86a4fcceeda9da0942d6bd2c458b4d14bca92f1de0794db65b4b240d4fb1fd', 'invite', '2099-12-31 23:59:59+00', NULL, '2026-11-08 05:52:51.973473+00', NULL);
INSERT INTO public.invite VALUES ('a6690261-c3a9-4bda-af1e-e225a80cd5f6', '20000000-0000-0000-0000-000000000003', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'invite', '2026-09-04 08:00:00+00', '2026-09-01 10:00:00+00', '2026-09-01 08:00:00+00', NULL);
INSERT INTO public.invite VALUES ('0c9b44c5-2c12-4b33-82f8-10bd59a7ab8d', NULL, 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'invite', '2026-09-07 07:00:00+00', '2026-09-05 09:00:00+00', '2026-09-04 11:00:00+00', '70000000-0000-0000-0000-000000000001');


--
-- Data for Name: level; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.level VALUES (1, 'Level 1', '#17695B');
INSERT INTO public.level VALUES (2, 'Level 2', '#2A559A');
INSERT INTO public.level VALUES (3, 'Level 3', '#5E4196');
INSERT INTO public.level VALUES (4, 'Level 4', '#9E4A17');
INSERT INTO public.level VALUES (5, 'Level 5', '#96283F');
INSERT INTO public.level VALUES (6, 'Level 6', '#4F5B1C');


--
-- Data for Name: level_change; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.level_change VALUES ('f7c49e6e-8d32-4a43-ad54-3f3ed2679d66', '70000000-0000-0000-0000-000000000001', 1, 2, 'Ready for Level 2', '20000000-0000-0000-0000-000000000003', '2026-09-04 10:15:00+00');


--
-- Data for Name: member; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.member VALUES ('50000000-0000-0000-0000-000000000001', 'Sara Ali', '01112345671', '2026-09-01 06:00:00+00', '2026-11-08 05:52:52.492568+00', 'sara.ali@mail.eg', NULL);
INSERT INTO public.member VALUES ('50000000-0000-0000-0000-000000000002', 'Omar Hassan', '01112345672', '2026-09-02 06:00:00+00', '2026-11-08 05:52:52.492568+00', 'omar.hassan@mail.eg', NULL);
INSERT INTO public.member VALUES ('50000000-0000-0000-0000-000000000003', 'Laila Samy', '01112345673', '2026-09-02 06:30:00+00', '2026-11-08 05:52:52.492568+00', 'laila.samy@mail.eg', '2026-09-05 07:00:00+00');


--
-- Data for Name: notice; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notice VALUES ('2ca49356-9714-4be8-ac3d-73e0b9d8ba9a', '20000000-0000-0000-0000-000000000003', 'result_overdue', '60000000-0000-0000-0000-000000000002', 'Record the assessment result for Omar Hassan', '2026-11-08 05:52:52.492568+00', NULL);


--
-- Data for Name: payment; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.payment VALUES ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 1200.00, 'cash', '2026-09-01', 1, 8, '2026-10-31', '2026-11-08 05:52:52.492568+00', 'b30cb8e5-8e49-44f9-8df2-f228c40b44e9', NULL, NULL, NULL, 0);


--
-- Data for Name: refresh_token; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.refresh_token VALUES ('2dbeca98-adc1-44ad-9213-261bf651257e', 'admin', 'b30cb8e5-8e49-44f9-8df2-f228c40b44e9', 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', false, '2026-10-04 07:00:00+00', '2026-09-04 07:00:00+00', NULL, NULL);
INSERT INTO public.refresh_token VALUES ('0fdbb32f-1aea-444f-a1c0-07e068c67c0a', 'rider', '70000000-0000-0000-0000-000000000001', 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', false, '2026-10-04 07:00:00+00', '2026-09-04 07:00:00+00', NULL, NULL);


--
-- Data for Name: rider; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.rider VALUES ('ffdfa363-a040-4739-b20c-19aba466ae0b', 'Mona Ali', '01001234567', 1, '00000000-0000-0000-0000-000000000001', NULL, 0, '2026-11-08 05:52:52.158201+00', '2026-11-08 05:52:52.158201+00', NULL, NULL, 'none', NULL);
INSERT INTO public.rider VALUES ('4f2397c2-c77b-49df-8cdd-1173f88d039e', 'Omar Hassan', NULL, 1, '00000000-0000-0000-0000-000000000001', NULL, 0, '2026-11-08 05:52:52.158201+00', '2026-11-08 05:52:52.158201+00', NULL, NULL, 'none', NULL);
INSERT INTO public.rider VALUES ('7565b2af-b0cd-4103-a441-2b31670a865f', 'Laila Samir', '01112345678', 1, '00000000-0000-0000-0000-000000000001', NULL, 0, '2026-11-08 05:52:52.158201+00', '2026-11-08 05:52:52.158201+00', NULL, NULL, 'none', NULL);
INSERT INTO public.rider VALUES ('70000000-0000-0000-0000-000000000001', 'Sara Ali', '01112345671', 2, '20000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001', 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', 'sara.ali@mail.eg', '$2a$10$2QypuqWXbYQIqoQbaNJV.Oj6KhEoRYT8Wgv1Ii/iyiV5CkeALH9.G', 'active', NULL);


--
-- Data for Name: session; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.session VALUES ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '2026-09-04', '11:00:00', '12:00:00', 4, 'scheduled', NULL, 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', NULL, NULL);
INSERT INTO public.session VALUES ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '2026-09-04', '12:00:00', '13:00:00', 6, 'done', NULL, 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', '2026-09-04 10:05:00+00', '20000000-0000-0000-0000-000000000003');
INSERT INTO public.session VALUES ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002', '2026-09-11', '12:00:00', '13:00:00', 6, 'scheduled', NULL, 0, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', NULL, NULL);


--
-- Data for Name: session_evaluation; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.session_evaluation VALUES ('932e491b-ba63-48a8-bf71-5751610e1160', '40000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 4, 'Steady seat', '2026-09-04 10:10:00+00');


--
-- Data for Name: session_template; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.session_template VALUES ('38b34cdc-a1c9-4636-826c-15d13c656d2a', 1, 6, '07:00:00', '08:00:00', 6, '2026-10-01', NULL, '2026-11-08 05:52:52.069369+00', '2026-11-08 05:52:52.069369+00', NULL);
INSERT INTO public.session_template VALUES ('1c728fc9-d201-4f33-97a9-3959625ce79e', 1, 1, '16:00:00', '17:00:00', 6, '2026-10-01', NULL, '2026-11-08 05:52:52.069369+00', '2026-11-08 05:52:52.069369+00', NULL);
INSERT INTO public.session_template VALUES ('335b6679-eb03-4ef7-b5f3-caa11515a041', 2, 6, '08:30:00', '09:30:00', 6, '2026-10-01', NULL, '2026-11-08 05:52:52.069369+00', '2026-11-08 05:52:52.069369+00', NULL);
INSERT INTO public.session_template VALUES ('04f20a5d-5e1e-4b15-befa-9bc87c5df450', NULL, 6, '12:00:00', '12:30:00', 3, '2026-10-01', NULL, '2026-11-08 05:52:52.069369+00', '2026-11-08 05:52:52.069369+00', NULL);
INSERT INTO public.session_template VALUES ('41096ff9-b76a-4849-9204-4e58260eaefa', NULL, 2, '12:00:00', '12:30:00', 3, '2026-10-01', NULL, '2026-11-08 05:52:52.069369+00', '2026-11-08 05:52:52.069369+00', NULL);
INSERT INTO public.session_template VALUES ('30000000-0000-0000-0000-000000000001', NULL, 5, '11:00:00', '12:00:00', 4, '2026-09-01', NULL, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', NULL);
INSERT INTO public.session_template VALUES ('30000000-0000-0000-0000-000000000002', 1, 5, '12:00:00', '13:00:00', 6, '2026-09-01', NULL, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00', NULL);


--
-- Data for Name: session_usage; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.session_usage VALUES ('085ecf43-53ca-404f-9fa4-6304ae091787', '70000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', '2026-09-04 10:05:00+00');


--
-- Data for Name: trainer; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.trainer VALUES ('00000000-0000-0000-0000-000000000001', 'Test Trainer One', '01000000001', 'trainer1@example.com', '$2a$10$Ik5cCu7XNL8SFoUPFT21hO5GvUhnVSPNyuKdsac0MJHveGb8ZWcC.', 'active', NULL, '2026-11-08 05:52:51.973473+00', '2026-11-08 05:52:51.973473+00');
INSERT INTO public.trainer VALUES ('00000000-0000-0000-0000-000000000002', 'Test Trainer Two', '01000000002', 'trainer2@example.com', NULL, 'invited', NULL, '2026-11-08 05:52:51.973473+00', '2026-11-08 05:52:51.973473+00');
INSERT INTO public.trainer VALUES ('20000000-0000-0000-0000-000000000003', 'Mona Adel', '01022222223', 'mona@school.eg', '$2a$10$2QypuqWXbYQIqoQbaNJV.Oj6KhEoRYT8Wgv1Ii/iyiV5CkeALH9.G', 'active', NULL, '2026-11-08 05:52:52.492568+00', '2026-11-08 05:52:52.492568+00');


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: admin_account pk_admin_account; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_account
    ADD CONSTRAINT pk_admin_account PRIMARY KEY (id);


--
-- Name: assessment_booking pk_assessment_booking; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_booking
    ADD CONSTRAINT pk_assessment_booking PRIMARY KEY (id);


--
-- Name: assignment pk_assignment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT pk_assignment PRIMARY KEY (id);


--
-- Name: invite pk_invite; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invite
    ADD CONSTRAINT pk_invite PRIMARY KEY (id);


--
-- Name: level pk_level; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level
    ADD CONSTRAINT pk_level PRIMARY KEY (id);


--
-- Name: level_change pk_level_change; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level_change
    ADD CONSTRAINT pk_level_change PRIMARY KEY (id);


--
-- Name: member pk_member; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT pk_member PRIMARY KEY (id);


--
-- Name: notice pk_notice; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notice
    ADD CONSTRAINT pk_notice PRIMARY KEY (id);


--
-- Name: payment pk_payment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT pk_payment PRIMARY KEY (id);


--
-- Name: refresh_token pk_refresh_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT pk_refresh_token PRIMARY KEY (id);


--
-- Name: rider pk_rider; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rider
    ADD CONSTRAINT pk_rider PRIMARY KEY (id);


--
-- Name: session pk_session; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session
    ADD CONSTRAINT pk_session PRIMARY KEY (id);


--
-- Name: session_evaluation pk_session_evaluation; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_evaluation
    ADD CONSTRAINT pk_session_evaluation PRIMARY KEY (id);


--
-- Name: session_template pk_session_template; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_template
    ADD CONSTRAINT pk_session_template PRIMARY KEY (id);


--
-- Name: session_usage pk_session_usage; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_usage
    ADD CONSTRAINT pk_session_usage PRIMARY KEY (id);


--
-- Name: trainer pk_trainer; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trainer
    ADD CONSTRAINT pk_trainer PRIMARY KEY (id);


--
-- Name: admin_account uq_admin_account_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_account
    ADD CONSTRAINT uq_admin_account_email UNIQUE (email);


--
-- Name: assignment uq_assignment_session_rider; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT uq_assignment_session_rider UNIQUE (session_id, rider_id);


--
-- Name: invite uq_invite_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invite
    ADD CONSTRAINT uq_invite_token_hash UNIQUE (token_hash);


--
-- Name: level uq_level_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level
    ADD CONSTRAINT uq_level_name UNIQUE (name);


--
-- Name: member uq_member_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT uq_member_email UNIQUE (email);


--
-- Name: member uq_member_phone; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT uq_member_phone UNIQUE (phone);


--
-- Name: payment uq_payment_id_rider; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT uq_payment_id_rider UNIQUE (id, rider_id);


--
-- Name: refresh_token uq_refresh_token_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT uq_refresh_token_token_hash UNIQUE (token_hash);


--
-- Name: rider uq_rider_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rider
    ADD CONSTRAINT uq_rider_email UNIQUE (email);


--
-- Name: rider uq_rider_member; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rider
    ADD CONSTRAINT uq_rider_member UNIQUE (member_id);


--
-- Name: session_evaluation uq_session_evaluation; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_evaluation
    ADD CONSTRAINT uq_session_evaluation UNIQUE (session_id, rider_id);


--
-- Name: session uq_session_template_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session
    ADD CONSTRAINT uq_session_template_date UNIQUE (template_id, session_date);


--
-- Name: session_usage uq_session_usage_rider_session; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_usage
    ADD CONSTRAINT uq_session_usage_rider_session UNIQUE (rider_id, session_id);


--
-- Name: trainer uq_trainer_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trainer
    ADD CONSTRAINT uq_trainer_email UNIQUE (email);


--
-- Name: trainer uq_trainer_phone; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trainer
    ADD CONSTRAINT uq_trainer_phone UNIQUE (phone);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: ix_assessment_booking_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_assessment_booking_session ON public.assessment_booking USING btree (session_id, status);


--
-- Name: ix_assignment_rider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_assignment_rider ON public.assignment USING btree (rider_id);


--
-- Name: ix_invite_rider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_invite_rider ON public.invite USING btree (rider_id);


--
-- Name: ix_invite_trainer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_invite_trainer ON public.invite USING btree (trainer_id);


--
-- Name: ix_level_change_rider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_level_change_rider ON public.level_change USING btree (rider_id, created_at DESC);


--
-- Name: ix_notice_ref; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_notice_ref ON public.notice USING btree (ref_id, type);


--
-- Name: ix_notice_trainer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_notice_trainer ON public.notice USING btree (trainer_id, created_at DESC);


--
-- Name: ix_payment_rider_live; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_payment_rider_live ON public.payment USING btree (rider_id, valid_until) WHERE (voided_at IS NULL);


--
-- Name: ix_refresh_token_active_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_refresh_token_active_subject ON public.refresh_token USING btree (subject_type, subject_id) WHERE (revoked_at IS NULL);


--
-- Name: ix_rider_trainer_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_rider_trainer_level ON public.rider USING btree (trainer_id, level_id);


--
-- Name: ix_session_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_session_date ON public.session USING btree (session_date);


--
-- Name: ix_session_evaluation_rider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_session_evaluation_rider ON public.session_evaluation USING btree (rider_id, created_at DESC);


--
-- Name: ix_session_evaluation_trainer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_session_evaluation_trainer ON public.session_evaluation USING btree (trainer_id);


--
-- Name: ix_session_template_level_day; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_session_template_level_day ON public.session_template USING btree (level_id, day_of_week) WHERE (deleted_at IS NULL);


--
-- Name: ix_session_usage_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_session_usage_payment ON public.session_usage USING btree (payment_id);


--
-- Name: uq_assessment_booking_active_member; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_assessment_booking_active_member ON public.assessment_booking USING btree (member_id) WHERE ((status)::text = 'upcoming'::text);


--
-- Name: uq_invite_open_per_rider; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_invite_open_per_rider ON public.invite USING btree (rider_id) WHERE ((accepted_at IS NULL) AND (rider_id IS NOT NULL));


--
-- Name: uq_invite_open_per_trainer; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_invite_open_per_trainer ON public.invite USING btree (trainer_id) WHERE (accepted_at IS NULL);


--
-- Name: uq_notice_unread_reminder; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_notice_unread_reminder ON public.notice USING btree (trainer_id, ref_id) WHERE (((type)::text = 'result_overdue'::text) AND (read_at IS NULL));


--
-- Name: assessment_booking fk_assessment_booking_assessor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_booking
    ADD CONSTRAINT fk_assessment_booking_assessor FOREIGN KEY (assessor_id) REFERENCES public.trainer(id);


--
-- Name: assessment_booking fk_assessment_booking_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_booking
    ADD CONSTRAINT fk_assessment_booking_level FOREIGN KEY (level_id) REFERENCES public.level(id);


--
-- Name: assessment_booking fk_assessment_booking_member; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_booking
    ADD CONSTRAINT fk_assessment_booking_member FOREIGN KEY (member_id) REFERENCES public.member(id);


--
-- Name: assessment_booking fk_assessment_booking_session; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_booking
    ADD CONSTRAINT fk_assessment_booking_session FOREIGN KEY (session_id) REFERENCES public.session(id);


--
-- Name: assignment fk_assignment_assigned_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT fk_assignment_assigned_by FOREIGN KEY (assigned_by) REFERENCES public.trainer(id);


--
-- Name: assignment fk_assignment_rider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT fk_assignment_rider FOREIGN KEY (rider_id) REFERENCES public.rider(id);


--
-- Name: assignment fk_assignment_session; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment
    ADD CONSTRAINT fk_assignment_session FOREIGN KEY (session_id) REFERENCES public.session(id);


--
-- Name: invite fk_invite_rider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invite
    ADD CONSTRAINT fk_invite_rider FOREIGN KEY (rider_id) REFERENCES public.rider(id) ON DELETE CASCADE;


--
-- Name: invite fk_invite_trainer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invite
    ADD CONSTRAINT fk_invite_trainer FOREIGN KEY (trainer_id) REFERENCES public.trainer(id) ON DELETE CASCADE;


--
-- Name: level_change fk_level_change_by_trainer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level_change
    ADD CONSTRAINT fk_level_change_by_trainer FOREIGN KEY (by_trainer_id) REFERENCES public.trainer(id);


--
-- Name: level_change fk_level_change_from_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level_change
    ADD CONSTRAINT fk_level_change_from_level FOREIGN KEY (from_level_id) REFERENCES public.level(id);


--
-- Name: level_change fk_level_change_rider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level_change
    ADD CONSTRAINT fk_level_change_rider FOREIGN KEY (rider_id) REFERENCES public.rider(id);


--
-- Name: level_change fk_level_change_to_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.level_change
    ADD CONSTRAINT fk_level_change_to_level FOREIGN KEY (to_level_id) REFERENCES public.level(id);


--
-- Name: notice fk_notice_trainer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notice
    ADD CONSTRAINT fk_notice_trainer FOREIGN KEY (trainer_id) REFERENCES public.trainer(id);


--
-- Name: payment fk_payment_recorded_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk_payment_recorded_by FOREIGN KEY (recorded_by) REFERENCES public.admin_account(id);


--
-- Name: payment fk_payment_rider; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk_payment_rider FOREIGN KEY (rider_id) REFERENCES public.rider(id);


--
-- Name: payment fk_payment_voided_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk_payment_voided_by FOREIGN KEY (voided_by) REFERENCES public.admin_account(id);


--
-- Name: refresh_token fk_refresh_token_replaced_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT fk_refresh_token_replaced_by FOREIGN KEY (replaced_by) REFERENCES public.refresh_token(id) ON DELETE SET NULL;


--
-- Name: rider fk_rider_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rider
    ADD CONSTRAINT fk_rider_level FOREIGN KEY (level_id) REFERENCES public.level(id);


--
-- Name: rider fk_rider_member; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rider
    ADD CONSTRAINT fk_rider_member FOREIGN KEY (member_id) REFERENCES public.member(id);


--
-- Name: rider fk_rider_trainer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rider
    ADD CONSTRAINT fk_rider_trainer FOREIGN KEY (trainer_id) REFERENCES public.trainer(id);


--
-- Name: session_evaluation fk_session_evaluation_seat; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_evaluation
    ADD CONSTRAINT fk_session_evaluation_seat FOREIGN KEY (session_id, rider_id) REFERENCES public.assignment(session_id, rider_id);


--
-- Name: session_evaluation fk_session_evaluation_trainer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_evaluation
    ADD CONSTRAINT fk_session_evaluation_trainer FOREIGN KEY (trainer_id) REFERENCES public.trainer(id);


--
-- Name: session fk_session_finished_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session
    ADD CONSTRAINT fk_session_finished_by FOREIGN KEY (finished_by) REFERENCES public.trainer(id);


--
-- Name: session fk_session_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session
    ADD CONSTRAINT fk_session_template FOREIGN KEY (template_id) REFERENCES public.session_template(id);


--
-- Name: session_template fk_session_template_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_template
    ADD CONSTRAINT fk_session_template_level FOREIGN KEY (level_id) REFERENCES public.level(id);


--
-- Name: session_usage fk_session_usage_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_usage
    ADD CONSTRAINT fk_session_usage_payment FOREIGN KEY (payment_id, rider_id) REFERENCES public.payment(id, rider_id);


--
-- Name: session_usage fk_session_usage_session; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_usage
    ADD CONSTRAINT fk_session_usage_session FOREIGN KEY (session_id) REFERENCES public.session(id);


--
-- PostgreSQL database dump complete
--


