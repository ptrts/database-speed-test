do
$$
    declare
        v_campaigns_number   int;
        v_users_per_campaign int;
        v_users_number       int;
    begin
        select
            campaigns_number,
            users_per_campaign,
            users_number
        into
            v_campaigns_number, v_users_per_campaign, v_users_number
        from
            data_parameters
        where
            id = 1;

        call _gen_campaign_users(
            p_start_campaign_id => 1,
            p_campaigns_number => v_campaigns_number,
            p_users_per_campaign => v_users_per_campaign,
            p_users_number => v_users_number
        );
    end
$$;
