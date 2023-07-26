create table test_domain(
  id int primary key,
  name varchar(255) not null,
  street varchar(50) not null,
  postcode varchar(10) not null,
  items int not null
);

insert into test_domain(id, name, street, postcode, items)
values
  (1, 'Foo', 'Montague Road', 'SW12 2PB', 5),
  (2, 'Bar', 'Goodenough Road', 'SW19 2BP', 0),
  (3, 'Baz', 'Small Lane', 'SY21 3QL', 1),
  (4, 'Blah', 'Myrtle Avenue', 'CR0 2DT', 4),
  (5, 'Blam', 'The Parade', 'NW2 5SN', 3)
;
