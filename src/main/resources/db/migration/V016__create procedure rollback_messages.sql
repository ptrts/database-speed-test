create or replace procedure _rollback_messages(
    p_campaigns_number   int,
    p_users_per_campaign int
)
    language plpgsql
as
$$
declare
    end_campaign_id bigint;
    end_id_bigint bigint;
begin
    raise notice 'truncate campaign_users...';
    truncate campaign_users;

    end_campaign_id := 1 + p_campaigns_number;
    end_id_bigint := 1 + p_campaigns_number * p_users_per_campaign;

    raise notice 'delete from message...';
    delete from message where campaign_id >= end_campaign_id;

    perform setval('message_id_bigint_seq', end_id_bigint, false);
    insert into campaign_users(campaign_id, user_id) values (p_campaigns_number, 0);
end;
$$;
