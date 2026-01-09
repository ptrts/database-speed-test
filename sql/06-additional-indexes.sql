create unique index message__campaign_id__user_id__uidx on message(campaign_id, user_id);

create index message__user_id__created__idx on message(user_id, created);
