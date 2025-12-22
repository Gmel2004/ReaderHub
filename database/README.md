# ReaderHub Database Setup

This directory contains SQL schema and migration scripts for the ReaderHub database.

## Files

- `schema.sql` - Complete database schema (CREATE TABLE statements)
- `migrations.js` - Node.js migration runner script
- `setup.js` - Initial database setup script
- `package.json` - Node.js dependencies

## Quick Start

### 1. Install Dependencies

```bash
cd database
npm install
```

### 2. Configure Database

Set environment variables or edit the configuration in `migrations.js` and `setup.js`:

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=your_password
export DB_NAME=readerhub
export DB_DIALECT=mysql  # or 'postgres'
```

### 3. Setup Database

Run the setup script to create the database and tables:

```bash
npm run setup
# or
node setup.js
```

### 4. Run Migrations

```bash
npm run migrate:up
# or
node migrations.js up
```

## Database Schema

### Tables

1. **book** - Stores book information
   - id (PRIMARY KEY)
   - title, author, filePath, fileType
   - coverUrl, fileSize
   - dateAdded, lastOpened
   - currentPage, totalPages
   - isFavorite

2. **bookmark** - Stores bookmarks
   - id (PRIMARY KEY)
   - bookId (FOREIGN KEY -> book.id)
   - pageNumber, chapterName, note
   - createdAt

3. **reading_history** - Stores reading statistics
   - id (PRIMARY KEY)
   - bookId (FOREIGN KEY -> book.id)
   - pageNumber, timestamp
   - readingDuration

## Migration Commands

### Run Migrations

```bash
node migrations.js up
```

### Rollback Last Migration

```bash
node migrations.js down
```

### Create New Migration

```bash
node migrations.js create migration_name
```

## Supported Databases

- MySQL/MariaDB
- PostgreSQL

## Notes

- Foreign keys use CASCADE delete (deleting a book deletes its bookmarks and history)
- Indexes are created on frequently queried columns
- Timestamps are stored as BIGINT (milliseconds since epoch)

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | Database host |
| DB_PORT | 3306 | Database port |
| DB_USER | root | Database user |
| DB_PASSWORD | (empty) | Database password |
| DB_NAME | readerhub | Database name |
| DB_DIALECT | mysql | Database type (mysql/postgres) |

