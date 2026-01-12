create table data_parameters(
    id int primary key not null default 1,
    campaigns_number   int not null default 0,
    users_per_campaign int not null default 0,
    users_number       int not null default 0
);

truncate data_parameters;

insert into data_parameters(id) values(1);

update data_parameters
set
    campaigns_number = 700,
    users_per_campaign = 100000,
    users_number = 1000000
where
    id = 1;
