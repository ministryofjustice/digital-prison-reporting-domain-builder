alter table domain
    add constraint unique_name_and_status_constraint unique (name, status),
    drop constraint unique_key_name;