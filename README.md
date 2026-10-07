# Zone Finder

Arabic (RTL) single-file web app. It imports courier Excel files, extracts customer / address / product / phone / amount data, and detects the delivery zone from Arabic keywords.

## Structure
- `index.html` — the whole app (HTML + CSS + JS, SheetJS and Cairo font embedded). No build step.
- `config.js` — public Supabase URL and publishable key only (never the secret / service_role key).
- `supabase/setup.sql` — tables, security rules (RLS) and realtime setup, run once in the Supabase SQL Editor.
- `.github/workflows/pages.yml` — publishes the site to GitHub Pages on every push to `main`.
- `.gitignore` — blocks backups (`zone_backup*.json`), Excel files and `.env`.

## How to change the app
1. Create a side branch (`git checkout -b my-change`).
2. Edit `index.html` and test it in a browser.
3. Open a pull request and merge it into `main`.
4. The site updates automatically after the merge.

## Data
Data is shared through Supabase (login required, sign-up disabled; users are added from the Supabase dashboard). The browser keeps a local copy (IndexedDB `zs2`) that syncs about every 5 seconds and works through short offline periods. Never commit real customer data or backup files to this repo.
