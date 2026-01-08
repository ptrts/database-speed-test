select
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    wait_event_type,
    wait_event,
    now() - query_start as query_age,
    left(query, 2000) as query
from pg_stat_activity
where datname = current_database()
and state = 'active'
and pid <> pg_backend_pid()
order by query_start nulls last;

select * from campaign_users limit 1;
select campaign_id, count(*) c from campaign_users group by campaign_id;
select user_id from campaign_users where campaign_id = 0;


select campaign_id, count(*) c from message group by campaign_id;
select campaign_id, user_id c from message order by campaign_id, user_id;

select count(distinct campaign_id) from message;

select
    pg_size_pretty(pg_total_relation_size('campaign_users')) as campaign_users,
    pg_size_pretty(pg_total_relation_size('message')) as message
;
