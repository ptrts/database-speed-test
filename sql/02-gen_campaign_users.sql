create or replace procedure gen_campaign_users(
    p_start_campaign_id  bigint default 0,
    p_campaigns_number   int default 100,
    p_users_per_campaign int default 10000,
    p_users_number       int default 100000
)
    language plpgsql
as
$$
declare
    c_batch_max_size     constant int    := 20000;
    i                             int;
    current_campaign_id           bigint;
    campaign_users_count          int;
    campaign_users_to_add         int;
    batch_size                    int;
    inserted_rows_count           int;
begin
    for i in 0..(p_campaigns_number - 1)
        loop
            current_campaign_id := p_start_campaign_id + i;
            --raise notice 'current_campaign_id=%', current_campaign_id;
            select
                count(*)
            into campaign_users_count
            from
                campaign_users
            where
                campaign_id = current_campaign_id;

            campaign_users_to_add := p_users_per_campaign - campaign_users_count;
            if campaign_users_to_add <= 0 then
                continue;
            end if;

            while campaign_users_to_add > 0
                loop
                    batch_size := least(campaign_users_to_add, c_batch_max_size);

                    insert into campaign_users (campaign_id, user_id)
                    select
                        current_campaign_id::bigint,
                        floor(random() * p_users_number)::bigint
                    from
                        generate_series(1, batch_size)
                    on conflict on constraint campaign_users_uidx do nothing;

                    get diagnostics inserted_rows_count = row_count;
                    --raise notice 'inserted=%', inserted_rows_count;

                    campaign_users_to_add := campaign_users_to_add - inserted_rows_count;
                end loop;
        end loop;
end;
$$;

call gen_campaign_users();

select campaign_id, count(*) c from campaign_users group by campaign_id;
select user_id from campaign_users where campaign_id = 0;
