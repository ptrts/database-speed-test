alter table campaign_users
    set (autovacuum_enabled = false, toast.autovacuum_enabled = false);
