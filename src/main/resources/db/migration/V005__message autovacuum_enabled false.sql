alter table message
    set (autovacuum_enabled = false, toast.autovacuum_enabled = false);
