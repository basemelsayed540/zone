# Zone Finder

Arabic (RTL) single-file web app. It imports courier Excel files, extracts customer / address / product / phone / amount data, and detects the delivery zone from Arabic keywords.

## Structure
- `index.html` — the whole app (HTML + CSS + JS, SheetJS and Cairo font embedded). No build step.
- `.github/workflows/pages.yml` — publishes the site to GitHub Pages on every push to `main`.
- `.gitignore` — blocks backups (`zone_backup*.json`), Excel files and `.env`.

## How to change the app
1. Create a side branch (`git checkout -b my-change`).
2. Edit `index.html` and test it in a browser.
3. Open a pull request and merge it into `main`.
4. The site updates automatically after the merge.

## Data
Data currently lives in the browser (IndexedDB `zs2`). Never commit real customer data or backup files to this repo.
