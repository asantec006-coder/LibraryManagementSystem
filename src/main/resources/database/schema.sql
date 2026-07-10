-- ==========================================================================
-- Reference schema for the Library Management System (SQLite).
--
-- NOTE: This file is documentation only. The actual table creation is
-- performed at runtime by each repository's createTableIfNotExists()
-- method. Keep this file in sync if the repository DDL changes.
-- ==========================================================================

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS books (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    title            TEXT    NOT NULL,
    author           TEXT    NOT NULL,
    isbn             TEXT    NOT NULL UNIQUE,
    genre            TEXT,
    type             TEXT    NOT NULL CHECK(type IN ('PHYSICAL','EBOOK')),
    total_copies     INTEGER NOT NULL DEFAULT 0,
    available_copies INTEGER NOT NULL DEFAULT 0,
    download_url     TEXT
);

CREATE TABLE IF NOT EXISTS members (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    email           TEXT    NOT NULL UNIQUE,
    phone           TEXT,
    join_date       TEXT    NOT NULL,
    books_borrowed  INTEGER NOT NULL DEFAULT 0,
    membership_type TEXT    NOT NULL CHECK(membership_type IN ('Standard','Premium')),
    status          TEXT    NOT NULL CHECK(status IN ('Active','Inactive'))
);

CREATE TABLE IF NOT EXISTS loans (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id     INTEGER NOT NULL,
    member_id   INTEGER NOT NULL,
    loan_date   TEXT    NOT NULL,
    due_date    TEXT,
    return_date TEXT,
    returned    INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS downloaded_books (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    title         TEXT    NOT NULL,
    author        TEXT,
    source        TEXT    NOT NULL,
    format        TEXT    NOT NULL,
    local_path    TEXT    NOT NULL,
    download_url  TEXT,
    downloaded_at TEXT    NOT NULL
);
