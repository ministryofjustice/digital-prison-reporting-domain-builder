create type status as enum ('DRAFT', 'PUBLISHED');

alter table domain
add column status status not null default 'DRAFT'


