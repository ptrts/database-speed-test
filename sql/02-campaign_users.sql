create table campaign_users
(
    campaign_id bigint not null,
    user_id     bigint not null,
    constraint campaign_users_uidx unique (campaign_id, user_id)
);

alter table campaign_users
    set (autovacuum_enabled = false, toast.autovacuum_enabled = false);
