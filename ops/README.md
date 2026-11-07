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

## Create or change a plan

The `plan` table starts empty. Run the script once per plan, after the app has started once (so Flyway has created
the table). Running it again with the same code updates that plan.

The numbers below are placeholders until prices and limits are decided:

```sh
psql "$DATABASE_URL" -v code=TRIAL -v name=Trial -v trainers=NULL -v riders=NULL \
     -v trial_days=30 -v on_expiry=BLOCK -f ops/upsert-plan.sql
psql "$DATABASE_URL" -v code=BASIC -v name=Basic -v trainers=2 -v riders=50 \
     -v trial_days=NULL -v on_expiry=READ_ONLY -f ops/upsert-plan.sql
psql "$DATABASE_URL" -v code=PRO -v name=Pro -v trainers=10 -v riders=300 \
     -v trial_days=NULL -v on_expiry=READ_ONLY -f ops/upsert-plan.sql
psql "$DATABASE_URL" -v code=MAX -v name=Max -v trainers=NULL -v riders=NULL \
     -v trial_days=NULL -v on_expiry=READ_ONLY -f ops/upsert-plan.sql
```

- `trainers` / `riders`: the most the plan allows; `NULL` means unlimited.
- `trial_days`: set only on the trial plan (at most one plan may have it); `NULL` for paid plans.
- `on_expiry`: `BLOCK` (the club can't be used) or `READ_ONLY` (the club can look but not change anything).
- The code is stored uppercase (letters and `_`). There is no delete script: a plan used by a subscription
  can't be removed.
