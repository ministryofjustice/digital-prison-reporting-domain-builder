create table domain(
  id uuid primary key,
  name varchar(255) not null,
  data json not null,
  created timestamp not null,
  lastUpdated timestamp not null,
  constraint unique_key_name unique (name)
)