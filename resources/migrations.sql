/* thanks to pesterhazy for the `idempotent` function:
 * https://clojureverse.org/t/how-do-you-do-database-migration-evolution/2005/2
 * https://gist.github.com/pesterhazy/9f7c0a7a9edd002759779c1732e0ac43
 */

create table if not exists migrations (
  key text CONSTRAINT pkey PRIMARY KEY
);

create or replace function idempotent(migration_name text,code text) returns void as $$
begin
if exists (select key from migrations where key=migration_name) then
  raise notice 'Migration already applied: %', migration_name;
else
  raise notice 'Running migration: %', migration_name;
  execute code;
  insert into migrations (key) VALUES (migration_name);
end if;
end;
$$ language plpgsql strict;

do $do$ begin perform idempotent('V0004__auth_code_table', $$
CREATE TABLE  patient (
  id              uuid PRIMARY KEY,
  first_name      text,
  last_name     text,
  ssn             text,
  sex             text,
  contact_phone   text,
  email           text
)
$$); end $do$;
