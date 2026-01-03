package com.anas.aiassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ai_assistant.db";
    private static final int DATABASE_VERSION = 4;

    // Table and column names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_CONVERSATIONS = "conversations";
    public static final String TABLE_MESSAGES = "messages";

    // Users table columns
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";

    // Conversations table columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CONVERSATION_USER_ID = "user_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CREATED_AT = "created_at";

    // Messages table columns
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_CONVERSATION_ID = "conversation_id";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_IS_USER = "is_user";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // SQL statements
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL" +
                    ")";

    private static final String CREATE_TABLE_CONVERSATIONS =
            "CREATE TABLE " + TABLE_CONVERSATIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CONVERSATION_USER_ID + " INTEGER NOT NULL, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                    "FOREIGN KEY(" + COLUMN_CONVERSATION_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TABLE_MESSAGES =
            "CREATE TABLE " + TABLE_MESSAGES + " (" +
                    COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CONVERSATION_ID + " INTEGER NOT NULL, " +
                    COLUMN_MESSAGE + " TEXT NOT NULL, " +
                    COLUMN_IS_USER + " INTEGER NOT NULL, " +
                    COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                    "FOREIGN KEY(" + COLUMN_CONVERSATION_ID + ") REFERENCES " +
                    TABLE_CONVERSATIONS + "(" + COLUMN_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String DROP_TABLE_USERS =
            "DROP TABLE IF EXISTS " + TABLE_USERS;

    private static final String DROP_TABLE_CONVERSATIONS =
            "DROP TABLE IF EXISTS " + TABLE_CONVERSATIONS;

    private static final String DROP_TABLE_MESSAGES =
            "DROP TABLE IF EXISTS " + TABLE_MESSAGES;

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_CONVERSATIONS);
        db.execSQL(CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migrate from version 1 to 2
            // Create conversations table
            db.execSQL(CREATE_TABLE_CONVERSATIONS);

            // Create a default conversation for existing messages
            db.execSQL("INSERT INTO " + TABLE_CONVERSATIONS + " (" +
                    COLUMN_TITLE + ", " + COLUMN_CREATED_AT + ") VALUES ('Chat', " +
                    System.currentTimeMillis() + ")");

            // Add conversation_id column to messages table
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " +
                    COLUMN_CONVERSATION_ID + " INTEGER DEFAULT 1");

            // Update all existing messages to use conversation_id = 1
            db.execSQL("UPDATE " + TABLE_MESSAGES + " SET " + COLUMN_CONVERSATION_ID + " = 1");

            // Rename id to message_id for consistency
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " RENAME COLUMN " + COLUMN_ID +
                    " TO " + COLUMN_MESSAGE_ID);
        }

        if (oldVersion < 3) {
            // Create users table for version 3
            db.execSQL(CREATE_TABLE_USERS);
        }

        if (oldVersion < 4) {
            // Add user_id column to conversations table for version 4
            db.execSQL("ALTER TABLE " + TABLE_CONVERSATIONS + " ADD COLUMN " +
                    COLUMN_USER_ID + " INTEGER DEFAULT 1");

            // Add foreign key constraint (SQLite doesn't support adding FK constraints via ALTER TABLE)
            // We'll rely on application logic to maintain referential integrity
        }
    }

    // Conversation CRUD operations
    public long createConversation(long userId, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());

        long id = db.insert(TABLE_CONVERSATIONS, null, values);
        db.close();
        return id;
    }

    public List<Conversation> getConversationsForUser(long userId) {
        List<Conversation> conversations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CONVERSATIONS,
                null,
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));

                conversations.add(new Conversation(id, title, createdAt));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return conversations;
    }

    // Keep the old method for backward compatibility, but it should not be used
    @Deprecated
    public List<Conversation> getAllConversations() {
        return new ArrayList<>();
    }

    // Message CRUD operations
    public long insertMessage(long conversationId, ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONVERSATION_ID, conversationId);
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_IS_USER, message.isUser() ? 1 : 0);
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());

        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close();
        return id;
    }

    public List<ChatMessage> getMessagesForConversation(long conversationId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MESSAGES,
                null,
                COLUMN_CONVERSATION_ID + " = ?",
                new String[]{String.valueOf(conversationId)},
                null, null,
                COLUMN_TIMESTAMP + " ASC");

        if (cursor.moveToFirst()) {
            do {
                String message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE));
                boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_USER)) == 1;
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));

                messages.add(new ChatMessage(message, isUser, timestamp));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messages;
    }

    public void deleteConversation(long conversationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete messages first (due to foreign key constraint)
        db.delete(TABLE_MESSAGES, COLUMN_CONVERSATION_ID + " = ?",
                new String[]{String.valueOf(conversationId)});
        // Then delete conversation
        db.delete(TABLE_CONVERSATIONS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(conversationId)});
        db.close();
    }

    public void updateConversationTitle(long conversationId, String newTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, newTitle);

        db.update(TABLE_CONVERSATIONS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(conversationId)});
        db.close();
    }

    public int getMessageCountForConversation(long conversationId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MESSAGES +
                " WHERE " + COLUMN_CONVERSATION_ID + " = ?",
                new String[]{String.valueOf(conversationId)});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    // User CRUD operations
    public long registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password); // Note: In production, hash the password

        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_EMAIL + " = ?",
                new String[]{email},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null);

        boolean success = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return success;
    }

    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID, COLUMN_USERNAME, COLUMN_EMAIL},
                COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            String userUsername = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
            user = new User(id, userUsername, email);
        }

        cursor.close();
        db.close();
        return user;
    }
}