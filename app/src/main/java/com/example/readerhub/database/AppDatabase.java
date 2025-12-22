package com.example.readerhub.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.readerhub.database.dao.BookDao;
import com.example.readerhub.database.dao.BookmarkDao;
import com.example.readerhub.database.dao.ReadingHistoryDao;
import com.example.readerhub.models.Book;
import com.example.readerhub.models.Bookmark;
import com.example.readerhub.models.ReadingHistory;

/**
 * Контекст БД
 */
@Database(entities = {Book.class, Bookmark.class, ReadingHistory.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract BookDao bookDao();
    public abstract BookmarkDao bookmarkDao();
    public abstract ReadingHistoryDao readingHistoryDao();

    // TODO: Подумать про потокобезопасность
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "readerHub_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}