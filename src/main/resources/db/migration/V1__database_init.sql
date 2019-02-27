create table "channels"
(
  "channel_id"  uuid      not null primary key,
  "name"        varchar   not null,
  "created"     timestamp not null,
  "last_update" timestamp
);
create table "groups"
(
  "group_id"    uuid      not null primary key,
  "name"        varchar   not null,
  "channel_id"  uuid      not null,
  "status"      integer   not null,
  "created"     timestamp not null,
  "last_update" timestamp,
  "parent_id"   uuid
);
create table "users"
(
  "user_id"     uuid      not null primary key,
  "level"       integer   not null,
  "login"       varchar   not null unique,
  "first_name"  varchar   not null,
  "last_name"   varchar   not null,
  "email"       varchar   not null unique,
  "password"    varchar   not null,
  "channel_id"  uuid,
  "group_id"    uuid,
  "group_role"  integer,
  "status"      integer   not null,
  "created"     timestamp not null,
  "last_update" timestamp
);
create index "idx_users_email" on "users" ("email");
create index "idx_users_group_id" on "users" ("group_id");
create index "idx_users_login" on "users" ("login");
create table "tokens"
(
  "token_id" uuid      not null primary key,
  "user_id"  uuid      not null,
  "role"     integer   not null,
  "created"  timestamp not null,
  "expires"  timestamp
);
alter table "groups"
  add constraint "group_channel_fk" foreign key ("channel_id") references "channels" ("channel_id") on update restrict on delete cascade;
alter table "users"
  add constraint "user_channel_fk" foreign key ("channel_id") references "channels" ("channel_id") on update restrict on delete cascade;
alter table "users"
  add constraint "user_group_fk" foreign key ("group_id") references "groups" ("group_id") on update restrict on delete cascade;
alter table "tokens"
  add constraint "user_fk" foreign key ("user_id") references "users" ("user_id") on update restrict on delete cascade;
