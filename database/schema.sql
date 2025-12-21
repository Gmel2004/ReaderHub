-- ReaderHub Database Schema
-- Version: 1.0
-- Database: PostgreSQL/MySQL compatible

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS reading_history;
DROP TABLE IF EXISTS bookmark;
DROP TABLE IF EXISTS book;

-- Table: book
-- Stores information about books in the library
CREATE TABLE book (
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

-- Table: bookmark
-- Stores bookmarks for books
CREATE TABLE bookmark (
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

-- Table: reading_history
-- Stores reading history and statistics
CREATE TABLE reading_history (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    bookId INTEGER NOT NULL,
    pageNumber INTEGER NOT NULL,
    timestamp BIGINT NOT NULL,
    readingDuration INTEGER DEFAULT 0,
    
    FOREIGN KEY (bookId) REFERENCES book(id) ON DELETE CASCADE,
    INDEX idx_bookId (bookId),
    INDEX idx_timestamp (timestamp)
);

-- PostgreSQL specific (uncomment if using PostgreSQL)
-- ALTER TABLE book ALTER COLUMN id TYPE SERIAL;
-- ALTER TABLE bookmark ALTER COLUMN id TYPE SERIAL;
-- ALTER TABLE reading_history ALTER COLUMN id TYPE SERIAL;

-- Sample data (optional)
-- INSERT INTO book (title, author, filePath, fileType, dateAdded) 
-- VALUES ('Sample Book', 'Sample Author', '/path/to/book.epub', 'EPUB', UNIX_TIMESTAMP() * 1000);

