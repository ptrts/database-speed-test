truncate campaign_users;
truncate message;

select setval('message_id_bigint_seq', 1, false);

alter table message
    drop constraint message_pkey;

call gen_campaign_users(
    p_start_campaign_id => 0,
    p_campaigns_number => 700,
    p_users_per_campaign => 100000,
    p_users_number => 1000000
);

call fill_messages();

alter table message
    add primary key (id_bigint);
