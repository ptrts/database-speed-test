truncate campaign_users;
truncate message;

select setval('message_id_bigint_seq', 1, false);

alter table message drop constraint message_pkey;
drop index message__campaign_id__user_id__uidx;
drop index message__user_id__created__idx;

vacuum (analyze) message;
vacuum (analyze) campaign_users;
