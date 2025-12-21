/**
 * ReaderHub Database Setup Script
 * Initializes the database and runs migrations
 */

const fs = require('fs');
const path = require('path');

// Read SQL schema file
const schemaPath = path.join(__dirname, 'schema.sql');
const schema = fs.readFileSync(schemaPath, 'utf8');

// Database configuration from environment variables
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'readerhub',
    dialect: process.env.DB_DIALECT || 'mysql'
};

/**
 * Execute SQL file
 */
async function setupDatabase() {
    console.log('Setting up ReaderHub database...');
    console.log(`Database: ${dbConfig.database}`);
    console.log(`Host: ${dbConfig.host}:${dbConfig.port}`);
    console.log(`Dialect: ${dbConfig.dialect}`);
    
    try {
        let connection;
        
        if (dbConfig.dialect === 'mysql') {
            const mysql = require('mysql2/promise');
            
            // First, connect without database to create it if needed
            const adminConnection = await mysql.createConnection({
                host: dbConfig.host,
                port: dbConfig.port,
                user: dbConfig.user,
                password: dbConfig.password
            });
            
            await adminConnection.query(`CREATE DATABASE IF NOT EXISTS \`${dbConfig.database}\``);
            await adminConnection.end();
            
            // Now connect to the database
            connection = await mysql.createConnection({
                host: dbConfig.host,
                port: dbConfig.port,
                user: dbConfig.user,
                password: dbConfig.password,
                database: dbConfig.database,
                multipleStatements: true
            });
            
            // Split SQL by semicolons and execute
            const statements = schema
                .split(';')
                .map(s => s.trim())
                .filter(s => s.length > 0 && !s.startsWith('--'));
            
            for (const statement of statements) {
                if (statement.length > 0) {
                    await connection.query(statement);
                }
            }
            
            await connection.end();
            
        } else if (dbConfig.dialect === 'postgres') {
            const { Pool } = require('pg');
            
            // First, connect to default database to create target database
            const adminPool = new Pool({
                host: dbConfig.host,
                port: dbConfig.port,
                user: dbConfig.user,
                password: dbConfig.password,
                database: 'postgres'
            });
            
            await adminPool.query(`CREATE DATABASE ${dbConfig.database}`);
            await adminPool.end();
            
            // Now connect to the target database
            const pool = new Pool({
                host: dbConfig.host,
                port: dbConfig.port,
                user: dbConfig.user,
                password: dbConfig.password,
                database: dbConfig.database
            });
            
            // Execute schema
            await pool.query(schema);
            await pool.end();
        }
        
        console.log('✓ Database setup completed successfully!');
        console.log('\nNext steps:');
        console.log('  1. Run migrations: npm run migrate:up');
        console.log('  2. Configure your application to use this database');
        
    } catch (error) {
        if (error.code === 'ER_DB_CREATE_EXISTS' || error.code === '42P04') {
            console.log('Database already exists. Continuing with schema setup...');
        } else {
            console.error('Setup failed:', error.message);
            process.exit(1);
        }
    }
}

// Run setup
setupDatabase().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});

