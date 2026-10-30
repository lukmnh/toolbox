# Toolbox Guide

Toolbox is a desktop toolkit for PDF, data, developer and everyday tasks. Everything runs **100% offline**: your files never leave this computer, and there is no server and no telemetry.

This guide is editable. Change it on the left, the preview on the right updates as you type, and your version is saved automatically. Use **Reset to default** to bring back the original text, or **Download .md / .pdf** to keep a copy.

## Getting around

- **Home** lists every tool, grouped by category. Type in the search box to filter by name or description, then click a card to open the tool.
- **Sidebar** on the left lists the same tools. Only one tool is open at a time; switching tools closes the previous one and frees its memory.
- If a tool has results you have not downloaded yet, Toolbox asks before closing it.
- **Open with**: in your file manager, open a supported file (for example `.pdf` or `.json`) with Toolbox to jump straight into the matching tool.

### Keyboard shortcuts

- `Ctrl+H`: go to Home.
- `Ctrl+B`: show or hide the sidebar.

## Downloads

- **Download** saves the result to your default download folder (`Downloads`). A notification offers **Open file** and **Open folder**.
- **Save as…** lets you pick the folder and file name.
- Existing files are never overwritten: a number is added to the name instead.
- Results stay available, so you can download the same result again.

## Settings and data

- **Theme**: the button at the top right switches between light and dark mode. The choice is remembered after a restart.
- **Version**: the footer shows the installed version of Toolbox.
- App data lives in the `.toolbox` folder in your home directory: settings, this guide, budgets and rename history. Temporary files are deleted when the app closes.

## PDF

### Fill & Sign

Add text, fill PDF forms and place a signature. Draw the signature by hand or upload a photo of a signature on white paper; the background is removed automatically. Download the signed PDF when done.

### Merge PDF

Combine several PDFs into one file, in the order you choose.

### Split PDF

Split a PDF into single pages or by page ranges such as `1-3, 5, 8-10`.

### Compress PDF

Shrink a PDF by recompressing its images with one of three presets. Text and vector graphics are left untouched.

## Data

### JSON Viewer

Open large JSON or JSONL files (hundreds of MB) as a table or a tree. Filter with a query, sort columns, and export the result to JSON, JSONL or CSV.

### JSON Beautify

Paste JSON or upload a `.json` file to format, minify or validate it.

### CSV Columns: Split & Merge

Split, merge and rearrange columns of large CSV files in a streaming pipeline. The delimiter and encoding are detected automatically, and a formula guard protects the output from spreadsheet formula injection.

## Dev Utils

### JWT Debugger

Decode a JWT header, payload and claims, check its time-based status, and verify HS, RS, PS, ES or EdDSA signatures offline.

### Encoder / Decoder

Base64 for text and files (URL-safe, MIME, data URI), URL encode/decode, and a query string parser.

### Hash Generator

- **Hash / HMAC**: hash text with MD5, SHA-1, SHA-2 or SHA-3, or switch to HMAC mode with a secret key.
- **Generate**: create a random secret (hex, Base64 or Base64URL), a key pair as PEM (public and private key), or UUIDs (v4 random or v7 time-ordered) for configuration, tests and fixtures.

### QR Code Generator

Turn text, a URL or a contact card (vCard) into a QR code, choose the error correction level, size and quiet zone, and download it as a PNG image.

### Checksum

Generate and verify file checksums in the familiar `sha256sum` format.

### Text Diff

Compare two texts or files side by side or as a unified diff, with word-level highlights. Export a `.diff` file that can be applied with `git apply`.

### Regex Tester

Test and explain regular expressions for Java, JavaScript, Go and PHP: per-group highlights, a match table, replace preview, and a timeout that stops runaway backtracking.

### SQL Formatter

Format or minify SQL in 10 dialects, including MySQL, PostgreSQL, Oracle and SQL Server. Strings and quoted identifiers are never changed.

### Cron Builder & Explainer

Build a cron expression or paste one to get a plain-language explanation and the next 10 run times. Supports Unix crontab, Spring, Quarkus/Quartz, Go (robfig/cron) and Laravel, with a code snippet for each.

## Extra Tools

### Text Tools

Count characters, words, sentences and reading time, change letter case, and clean up lines. Up to 20 steps of undo.

### Batch Rename

Rename many files at once with stacked rules. Preview the new names and conflicts first; the rename is applied safely in two phases and can be undone.

### Installment Simulator

- **Installment**: compare annuity, flat and effective interest, view the full amortization table, and download the schedule as CSV or PDF.
- **Discount**: stack several discounts plus a fixed deduction and see the effective discount.
- **Tax & VAT**: add tax to a price or extract it from a tax-inclusive amount.
- **Invoice**: fill in items, quantities and prices, then download a ready-to-send invoice or receipt as PDF.

Amounts are in Rupiah (`Rp`). Numbers can be typed in Indonesian format (`1.250.000,50`) or plain (`1250000.5`).

### Income & Expense Budget

Track monthly income and recurring expenses, see the average cost per month and what is left over, with a summary and chart. Budgets are saved automatically.

- **Download** a budget as Excel (`.xlsx` with live formulas) or as PDF.
- **Import** only accepts Excel or PDF files exported by Toolbox, so you can restore a budget or move it to another computer.

## Tips

- Large files are processed in the background; you can keep working while a progress bar runs.
- Nothing is uploaded anywhere, so Toolbox is safe for confidential documents.
- Found a problem or have an idea? Visit the author on GitHub: [@lukmnh](https://github.com/lukmnh).
