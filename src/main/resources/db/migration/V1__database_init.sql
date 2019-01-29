create table "CHANNELS"
(
  "CHANNEL_ID"  UUID      NOT NULL PRIMARY KEY,
  "NAME"        VARCHAR   NOT NULL,
  "CREATED"     TIMESTAMP NOT NULL,
  "LAST_UPDATE" TIMESTAMP
);
create table "GROUPS"
(
  "GROUP_ID"    UUID      NOT NULL PRIMARY KEY,
  "NAME"        VARCHAR   NOT NULL,
  "CHANNEL_ID"  UUID      NOT NULL,
  "STATUS"      INTEGER   NOT NULL,
  "CREATED"     TIMESTAMP NOT NULL,
  "LAST_UPDATE" TIMESTAMP,
  "PARENT_ID"   UUID
);
create table "USERS"
(
  "USER_ID"     UUID      NOT NULL PRIMARY KEY,
  "LEVEL"       INTEGER   NOT NULL,
  "LOGIN"       VARCHAR   NOT NULL UNIQUE,
  "FIRST_NAME"  VARCHAR   NOT NULL,
  "LAST_NAME"   VARCHAR   NOT NULL,
  "EMAIL"       VARCHAR   NOT NULL UNIQUE,
  "PASSWORD"    VARCHAR   NOT NULL,
  "CHANNEL_ID"  UUID,
  "GROUP_ID"    UUID,
  "GROUP_ROLE"  INTEGER,
  "STATUS"      INTEGER   NOT NULL,
  "CREATED"     TIMESTAMP NOT NULL,
  "LAST_UPDATE" TIMESTAMP
);
create index "IDX_USERS_EMAIL" on "USERS" ("EMAIL");
create index "IDX_USERS_GROUP_ID" on "USERS" ("GROUP_ID");
create index "IDX_USERS_LOGIN" on "USERS" ("LOGIN");
create table "TOKENS"
(
  "TOKEN_ID" UUID      NOT NULL PRIMARY KEY,
  "USER_ID"  UUID      NOT NULL,
  "ROLE"     INTEGER   NOT NULL,
  "CREATED"  TIMESTAMP NOT NULL,
  "EXPIRES"  TIMESTAMP
);
alter table "GROUPS"
  add constraint "GROUP_CHANNEL_FK" foreign key ("CHANNEL_ID") references "CHANNELS" ("CHANNEL_ID") on update RESTRICT on delete CASCADE;
alter table "USERS"
  add constraint "USER_CHANNEL_FK" foreign key ("CHANNEL_ID") references "CHANNELS" ("CHANNEL_ID") on update RESTRICT on delete CASCADE;
alter table "USERS"
  add constraint "USER_GROUP_FK" foreign key ("GROUP_ID") references "GROUPS" ("GROUP_ID") on update RESTRICT on delete CASCADE;
alter table "TOKENS"
  add constraint "USER_FK" foreign key ("USER_ID") references "USERS" ("USER_ID") on update RESTRICT on delete CASCADE;
