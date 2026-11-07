# Ops scripts

Scripts an operator runs by hand. Flyway and the app never run them.

## Create the super admin

Run once per environment, after the app has started once (so Flyway has created the `super_admin` table):

```sh
psql "$DATABASE_URL" -v email='ops@equis.app' -v pwd='<password>' -f ops/create-super-admin.sql
```

- Type the password on the command line only. Never write it into a file in this repository.
- The email is stored lowercase. Running the script again with the same email changes nothing; it does not
  reset the password.
- The super admin then signs in with `POST /api/platform/auth/login` (`{ "email", "password", "rememberMe" }`).
