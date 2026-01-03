create or replace procedure fill_messages()
    language plpgsql
as
$$
begin
    with
        ordered as (select
                        cu.campaign_id,
                        cu.user_id,
                        row_number() over (order by cu.campaign_id, cu.user_id) as rn
                    from
                        campaign_users cu
                    order by
                        cu.campaign_id,
                        cu.user_id
        )
    insert
    into
        message
    (id_bigint, id_uuid, campaign_id, user_id, topic, text, created, deleted, sent)
    select
        nextval('message_id_bigint_seq')              as id_bigint,
        gen_random_uuid()                             as id_uuid,
        o.campaign_id,
        o.user_id,
        'Кампания ' || o.campaign_id                  as topic,
        array_to_string(
        array_fill(
            'текст кампании ' || o.campaign_id, array [20]
            ),
        ', '
            )                                             as text,
        ('2025-01-01 00:00:00+00'::timestamptz
            + (o.rn - 1) * interval '4 milliseconds') as created,
        null::timestamptz                             as deleted,
        null::timestamptz                             as sent
    from
        ordered o
    order by
        o.campaign_id, o.user_id;
end;
$$;

call fill_messages();

select campaign_id, count(*) c from message group by campaign_id;
select campaign_id, user_id c from message order by campaign_id, user_id;

select pg_size_pretty(pg_total_relation_size('message')) as total_size;
