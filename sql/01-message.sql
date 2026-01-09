create sequence message_id_bigint_seq;

create table message
(
    id_bigint   bigint default nextval('message_id_bigint_seq'::regclass) not null
        primary key,
    id_uuid     uuid                                                      not null,
    campaign_id bigint                                                    not null,
    user_id     bigint                                                    not null,
    topic       varchar(128)                                              not null,
    text        varchar(1024)                                             not null,
    created     timestamp(6) with time zone,
    sent        timestamp(6) with time zone,
    deleted     timestamp(6) with time zone
);

alter table message
    set (autovacuum_enabled = false, toast.autovacuum_enabled = false);
