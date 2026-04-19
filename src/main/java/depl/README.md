# Archived Sources

This folder contains archived project sources that were identified as safe to move during cleanup.

Purpose:

- keep a trace of duplicated, obsolete or unused classes
- avoid deleting files immediately
- keep active source packages cleaner

Archiving rules used here:

- files moved from active source packages are stored with the suffix `.archived`
- legacy sources formerly stored under the old `main/java` tree are also archived here
- archived files are intentionally neutralized as compile inputs so they do not create duplicate classes or accidental Spring component scanning

Important note:

- this folder is documentation/archive material, not active application code
- classes still used by Spring, schedulers, listeners, WebSocket publishers, frontend flows or tests must not be moved here without a new audit
