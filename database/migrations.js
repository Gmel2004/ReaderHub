/**
 * ReaderHub Database Migrations
 * Node.js migration script for database setup and migrations
 * 
 * Usage: node migrations.js [command]
 * Commands:
 *   - up: Run all pending migrations
 *   - down: Rollback last migration
 *   - create [name]: Create a new migration file
 */

const fs = require('fs');
const path = require('path');

// Database configuration
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'readerhub',
    dialect: process.env.DB_DIALECT || 'mysql' // 'mysql' or 'postgres'
};

// SQL queries for migrations
const migrations = [
    {
        version: 1,
        name: 'initial_schema',
        up: `
            CREATE TABLE IF NOT EXISTS book (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                title VARCHAR(500) NOT NULL,
                author VARCHAR(255) NOT NULL,
                filePath VARCHAR(1000) NOT NULL,
                fileType VARCHAR(10) NOT NULL CHECK (fileType IN ('EPUB', 'PDF', 'FB2', 'RANOBE')),
                coverUrl VARCHAR(1000),
                fileSize BIGINT DEFAULT 0,
                dateAdded BIGINT NOT NULL,
                lastOpened BIGINT DEFAULT 0,
                currentPage INTEGER DEFAULT 0,
                totalPages INTEGER DEFAULT 0,
                isFavorite BOOLEAN DEFAULT FALSE,
                INDEX idx_title (title),
                INDEX idx_author (author),
                INDEX idx_lastOpened (lastOpened),
                INDEX idx_isFavorite (isFavorite)
            );
            
            CREATE TABLE IF NOT EXISTS bookmark (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                bookId INTEGER NOT NULL,
                pageNumber INTEGER NOT NULL,
                chapterName VARCHAR(500),
                note TEXT,
                createdAt BIGINT NOT NULL,
                FOREIGN KEY (bookId) REFERENCES book(id) ON DELETE CASCADE,
                INDEX idx_bookId (bookId),
                INDEX idx_createdAt (createdAt)
            );
            
            CREATE TABLE IF NOT EXISTS reading_history (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                bookId INTEGER NOT NULL,
                pageNumber INTEGER NOT NULL,
                timestamp BIGINT NOT NULL,
                readingDuration INTEGER DEFAULT 0,
                FOREIGN KEY (bookId) REFERENCES book(id) ON DELETE CASCADE,
                INDEX idx_bookId (bookId),
                INDEX idx_timestamp (timestamp)
            );
        `,
        down: `
            DROP TABLE IF EXISTS reading_history;
            DROP TABLE IF EXISTS bookmark;
            DROP TABLE IF EXISTS book;
        `
    }
];

// Migration tracking table
const createMigrationTable = `
    CREATE TABLE IF NOT EXISTS schema_migrations (
        version INTEGER PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
`;

/**
 * Execute SQL query
 */
async function executeQuery(query, connection) {
    return new Promise((resolve, reject) => {
        connection.query(query, (error, results) => {
            if (error) {
                reject(error);
            } else {
                resolve(results);
            }
        });
    });
}

/**
 * Get database connection
 */
function getConnection() {
    if (dbConfig.dialect === 'mysql') {
        const mysql = require('mysql2/promise');
        return mysql.createConnection({
            host: dbConfig.host,
            port: dbConfig.port,
            user: dbConfig.user,
            password: dbConfig.password,
            database: dbConfig.database
        });
    } else if (dbConfig.dialect === 'postgres') {
        const { Pool } = require('pg');
        return new Pool({
            host: dbConfig.host,
            port: dbConfig.port,
            user: dbConfig.user,
            password: dbConfig.password,
            database: dbConfig.database
        });
    }
    throw new Error(`Unsupported database dialect: ${dbConfig.dialect}`);
}

/**
 * Get applied migrations
 */
async function getAppliedMigrations(connection) {
    try {
        await executeQuery(createMigrationTable, connection);
        const results = await executeQuery('SELECT version FROM schema_migrations ORDER BY version', connection);
        return results.map(row => row.version || row.rows?.[0]?.version);
    } catch (error) {
        // Table doesn't exist yet, return empty array
        return [];
    }
}

/**
 * Record migration
 */
async function recordMigration(connection, version, name) {
    const query = dbConfig.dialect === 'mysql'
        ? `INSERT INTO schema_migrations (version, name) VALUES (${version}, '${name}')`
        : `INSERT INTO schema_migrations (version, name) VALUES (${version}, '${name}')`;
    await executeQuery(query, connection);
}

/**
 * Run migrations up
 */
async function migrateUp() {
    const connection = await getConnection();
    try {
        const applied = await getAppliedMigrations(connection);
        const pending = migrations.filter(m => !applied.includes(m.version));
        
        if (pending.length === 0) {
            console.log('No pending migrations.');
            return;
        }
        
        console.log(`Running ${pending.length} migration(s)...`);
        
        for (const migration of pending) {
            console.log(`Running migration ${migration.version}: ${migration.name}`);
            await executeQuery(migration.up, connection);
            await recordMigration(connection, migration.version, migration.name);
            console.log(`✓ Migration ${migration.version} completed`);
        }
        
        console.log('All migrations completed successfully!');
    } catch (error) {
        console.error('Migration failed:', error);
        throw error;
    } finally {
        await connection.end();
    }
}

/**
 * Rollback last migration
 */
async function migrateDown() {
    const connection = await getConnection();
    try {
        const applied = await getAppliedMigrations(connection);
        
        if (applied.length === 0) {
            console.log('No migrations to rollback.');
            return;
        }
        
        const lastMigration = migrations.find(m => m.version === Math.max(...applied));
        
        if (!lastMigration) {
            console.log('Migration not found.');
            return;
        }
        
        console.log(`Rolling back migration ${lastMigration.version}: ${lastMigration.name}`);
        await executeQuery(lastMigration.down, connection);
        
        const deleteQuery = dbConfig.dialect === 'mysql'
            ? `DELETE FROM schema_migrations WHERE version = ${lastMigration.version}`
            : `DELETE FROM schema_migrations WHERE version = ${lastMigration.version}`;
        await executeQuery(deleteQuery, connection);
        
        console.log(`✓ Migration ${lastMigration.version} rolled back`);
    } catch (error) {
        console.error('Rollback failed:', error);
        throw error;
    } finally {
        await connection.end();
    }
}

/**
 * Create new migration file
 */
function createMigration(name) {
    const timestamp = Date.now();
    const filename = `${timestamp}_${name}.js`;
    const filepath = path.join(__dirname, 'migrations', filename);
    
    const template = `/**
 * Migration: ${name}
 * Created: ${new Date().toISOString()}
 */

module.exports = {
    version: ${migrations.length + 1},
    name: '${name}',
    up: \`
        -- Add your UP migration SQL here
    \`,
    down: \`
        -- Add your DOWN migration SQL here
    \`
};
`;
    
    const migrationsDir = path.join(__dirname, 'migrations');
    if (!fs.existsSync(migrationsDir)) {
        fs.mkdirSync(migrationsDir, { recursive: true });
    }
    
    fs.writeFileSync(filepath, template);
    console.log(`Created migration file: ${filename}`);
}

// CLI interface
const command = process.argv[2];
const arg = process.argv[3];

(async () => {
    try {
        switch (command) {
            case 'up':
                await migrateUp();
                break;
            case 'down':
                await migrateDown();
                break;
            case 'create':
                if (!arg) {
                    console.error('Please provide a migration name: node migrations.js create [name]');
                    process.exit(1);
                }
                createMigration(arg);
                break;
            default:
                console.log('Usage: node migrations.js [command]');
                console.log('Commands:');
                console.log('  up              Run all pending migrations');
                console.log('  down            Rollback last migration');
                console.log('  create [name]   Create a new migration file');
                process.exit(1);
        }
    } catch (error) {
        console.error('Error:', error.message);
        process.exit(1);
    }
})();

