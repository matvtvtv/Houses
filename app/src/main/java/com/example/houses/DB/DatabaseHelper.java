package com.example.houses.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "houses.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseHelper instance;

    // Таблица пользователей
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_LOGIN = "login";
    private static final String COLUMN_AVATAR = "avatar"; // BLOB

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    public void deleteUser(String login) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("users", "login=?", new String[]{login});
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создаем таблицу пользователей (если еще нет)
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                COLUMN_LOGIN + " TEXT PRIMARY KEY, " +
                COLUMN_AVATAR + " BLOB" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Для простоты, просто удалим и пересоздадим
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- Обновление аватарки пользователя ---
    public void updateUserAvatar(String login, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();

        // Проверяем, есть ли такой пользователь
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_LOGIN},
                COLUMN_LOGIN + "=?", new String[]{login}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Пользователь есть, обновляем
            db.update(TABLE_USERS, values, COLUMN_LOGIN + "=?", new String[]{login});
        } else {
            // Пользователя нет, создаем
            values.put(COLUMN_LOGIN, login);
            db.insert(TABLE_USERS, null, values);
        }

        if (cursor != null) cursor.close();
    }

    // --- Получение аватарки пользователя ---
    public byte[] getUserAvatar(String login) {
        SQLiteDatabase db = getReadableDatabase();
        byte[] avatar = null;

        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_AVATAR},
                COLUMN_LOGIN + "=?", new String[]{login}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            avatar = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_AVATAR));
        }

        if (cursor != null) cursor.close();
        return avatar;
    }
}
