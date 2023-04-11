create table domain(
  id uuid primary key,
  name varchar(255) not null,
  data json not null,
  constraint unique_key_name unique (name)
)