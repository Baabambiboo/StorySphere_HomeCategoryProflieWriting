package com.example.storysphere_appbar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

  
    public static final String DATABASE_NAME = "StorysphereDatabase.db";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_WRITINGS = "writings";
    public static final String TABLE_CURRENT_SESSION = "current_session";
    public static final String TABLE_EPISODES = "episodes";

    private static final int DATABASE_VERSION = 7;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "email TEXT UNIQUE, " +
                    "username TEXT, " +
                    "password TEXT, " +
                    "image_uri TEXT, " +
                    "role TEXT DEFAULT 'user')");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WRITINGS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT, " +
                    "tagline TEXT, " +
                    "tag TEXT, " +
                    "category TEXT, " +
                    "image_path TEXT, " +
                    "content TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CURRENT_SESSION + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_email TEXT)");


            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EPISODES + " (" +
                    "episode_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "writing_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "content_html TEXT NOT NULL, " +
                    "privacy_settings TEXT NOT NULL CHECK(privacy_settings IN ('public','private')) DEFAULT 'public', " +
                    "episode_no INTEGER, " +
                    "created_at_text TEXT NOT NULL, " +
                    "updated_at_text TEXT NOT NULL, " +
                    "FOREIGN KEY (writing_id) REFERENCES " + TABLE_WRITINGS + "(id) ON DELETE CASCADE" +
                    ")");


            db.execSQL("CREATE INDEX IF NOT EXISTS idx_episodes_writing_id ON " + TABLE_EPISODES + "(writing_id)");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_WRITINGS + " ADD COLUMN content TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN image_uri TEXT");
        }
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CURRENT_SESSION);
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CURRENT_SESSION + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_email TEXT)");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN role TEXT DEFAULT 'user'");
        }
        
        if (oldVersion < 7) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EPISODES + " (" +
                    "episode_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "writing_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "content_html TEXT NOT NULL, " +
                    "privacy_settings TEXT NOT NULL CHECK(privacy_settings IN ('public','private')) DEFAULT 'public', " +
                    "episode_no INTEGER, " +
                    "created_at_text TEXT NOT NULL, " +
                    "updated_at_text TEXT NOT NULL, " +
                    "FOREIGN KEY (writing_id) REFERENCES " + TABLE_WRITINGS + "(id) ON DELETE CASCADE" +
                    ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_episodes_writing_id ON " + TABLE_EPISODES + "(writing_id)");
        }
    }

    public void ensureEpisodesTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EPISODES + " (" +
                "episode_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "writing_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "content_html TEXT NOT NULL, " +
                "privacy_settings TEXT NOT NULL CHECK(privacy_settings IN ('public','private')) DEFAULT 'public', " +
                "episode_no INTEGER, " +
                "created_at_text TEXT NOT NULL, " +
                "updated_at_text TEXT NOT NULL, " +
                "FOREIGN KEY (writing_id) REFERENCES " + TABLE_WRITINGS + "(id) ON DELETE CASCADE" +
                ")");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ---------- Session ----------
    public boolean saveLoginSession(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_email", email);
        db.delete(TABLE_CURRENT_SESSION, null, null);
        long result = db.insert(TABLE_CURRENT_SESSION, null, values);
        return result != -1;
    }

    public String getLoggedInUserEmail() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT user_email FROM " + TABLE_CURRENT_SESSION + " LIMIT 1", null)) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndex("user_email");
                if (idx != -1) return c.getString(idx);
            }
        }
        return null;
    }

    public boolean clearLoginSession() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_CURRENT_SESSION, null, null) > 0;
    }

    // ---------- Users ----------
    public boolean insertUser(String username, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("email", email);
        v.put("password", password);
        v.put("role", role);
        return db.insert(TABLE_USERS, null, v) != -1;
    }

    public boolean insertUser(String username, String email, String password) {
        return insertUser(username, email, password, "user");
    }

    public boolean updateUser(String email, String newUsername, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", newUsername);
        v.put("password", newPassword);
        return db.update(TABLE_USERS, v, "email = ?", new String[]{email}) > 0;
    }

    public boolean updateUser(String email, String newUsername, String newPassword, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        if (newUsername != null) v.put("username", newUsername);
        if (newPassword != null) v.put("password", newPassword);
        if (imageUri != null)  v.put("image_uri", imageUri);
        if (v.size() == 0) return false;
        return db.update(TABLE_USERS, v, "email = ?", new String[]{email}) > 0;
    }

    public boolean deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USERS, "email = ?", new String[]{email}) > 0;
    }

    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE email = ?", new String[]{email});
    }

    public Cursor getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username = ?", new String[]{username});
    }

    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_USERS + " WHERE email = ? LIMIT 1", new String[]{email})) {
            return c.moveToFirst();
        }
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_USERS + " WHERE username = ? LIMIT 1", new String[]{username})) {
            return c.moveToFirst();
        }
    }

    public boolean updateUserPassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", newPassword);
        return db.update(TABLE_USERS, cv, "email = ?", new String[]{email}) > 0;
    }

    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String role = null;
        try (Cursor c = db.rawQuery("SELECT role FROM " + TABLE_USERS + " WHERE email = ?", new String[]{email})) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndex("role");
                if (idx != -1) role = c.getString(idx);
            }
        }
        Log.d("DBHelper", "getUserRole for " + email + ": " + role);
        return role;
    }

    public String getUserImageUri(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT image_uri FROM " + TABLE_USERS + " WHERE email = ?", new String[]{email})) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndex("image_uri");
                if (idx != -1) return c.getString(idx);
            }
        }
        return null;
    }

    public boolean checkUserCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean ok = false;
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_USERS + " WHERE email = ? AND password = ? LIMIT 1",
                new String[]{email, password})) {
            ok = c.moveToFirst();
        }
        Log.d("DBHelper", "Credentials check result: " + ok);
        return ok;
    }

    // ---------- Writings ----------
    public long insertWriting(String title, String tagline, String tag, String category, String imagePath, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("tagline", tagline);
        cv.put("tag", tag);
        cv.put("category", category);
        cv.put("image_path", imagePath);
        cv.put("content", content);
        return db.insert(TABLE_WRITINGS, null, cv);
    }

    public boolean updateWriting(int id, String title, String tagline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("tagline", tagline);
        return db.update(TABLE_WRITINGS, v, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteWriting(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WRITINGS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor getAllWritingsCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " ORDER BY id DESC", null);
    }
    public Cursor getAllWritings() { return getAllWritingsCursor(); }

    public List<WritingItem> getAllWritingItems() {
        List<WritingItem> list = new ArrayList<>();
        try (Cursor c = getAllWritingsCursor()) {
            if (c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow("id"));
                    String title = safeGet(c, "title");
                    String tagline = safeGet(c, "tagline");
                    String tag = safeGet(c, "tag");
                    String category = safeGet(c, "category");
                    String imagePath = safeGet(c, "image_path");
                    // String content = safeGet(c, "content"); // มีได้ แต่ยังไม่ใช้กับ WritingItem

                    list.add(new WritingItem(id, title, tagline, tag, category, imagePath));
                } while (c.moveToNext());
            }
        }
        return list;
    }

    public Cursor getWritingById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE id = ?", new String[]{String.valueOf(id)});
    }

    public boolean insertBook(String title, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("image_path", imageUri);
        return db.insert(TABLE_WRITINGS, null, v) != -1;
    }

    public boolean writingExists(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_WRITINGS + " WHERE id=? LIMIT 1",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst();
        }
    }

    public List<WritingItem> getWritingItemsByTag(String rawTag, int limit) {
        if (rawTag == null) rawTag = "";
        String norm = rawTag.trim().toLowerCase();
        String alt = norm.replace("-", "");

        SQLiteDatabase db = getReadableDatabase();
        String sql =
                "SELECT id, title, tagline, tag, category, image_path " +
                        "FROM " + TABLE_WRITINGS + " " +
                        "WHERE " +
                        "  LOWER(IFNULL(category,'')) = ? " +
                        "  OR LOWER(REPLACE(IFNULL(category,''),'-','')) = ? " +
                        "  OR (',' || LOWER(REPLACE(IFNULL(tag,''), ' ', '')) || ',') LIKE ? " +
                        "  OR (',' || LOWER(REPLACE(IFNULL(tag,''), ' ', '')) || ',') LIKE ? " +
                        "ORDER BY id DESC " +
                        (limit > 0 ? "LIMIT " + limit : "");
        String like1 = "%," + norm + ",%";
        String like2 = "%," + alt + ",%";

        List<WritingItem> list = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, new String[]{norm, alt, like1, like2})) {
            while (c.moveToNext()) {
                list.add(new WritingItem(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3),
                        c.getString(4),
                        c.getString(5)
                ));
            }
        }
        return list;
    }

    public List<WritingItem> getRecentWritings(int limit) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT id, title, tagline, tag, category, image_path FROM " + TABLE_WRITINGS +
                " ORDER BY id DESC " + (limit > 0 ? "LIMIT " + limit : "");
        List<WritingItem> list = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                list.add(new WritingItem(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3),
                        c.getString(4),
                        c.getString(5)
                ));
            }
        }
        return list;
    }

    // ---------- Writings by Username ----------
    public List<WritingItem> getWritingItemsByUsername(String username) {
        List<WritingItem> list = new ArrayList<>();
        if (username == null || username.isEmpty()) return list;

        SQLiteDatabase db = this.getReadableDatabase();

        boolean hasAuthorUsername = hasColumn(db, TABLE_WRITINGS, "author_username");
        boolean hasAuthorEmail    = hasColumn(db, TABLE_WRITINGS, "author_email");
        boolean hasUserIdFk       = hasColumn(db, TABLE_WRITINGS, "user_id");

        Cursor c = null;
        try {
            if (hasAuthorUsername) {
                // กรณีตาราง writings มีคอลัมน์ author_username
                c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE author_username=? ORDER BY id DESC",
                        new String[]{ username });
            } else if (hasAuthorEmail) {
                // ถ้ามี author_email → หา email ของ user จากตาราง users ก่อน
                String email = null;
                Cursor u = getUserByUsername(username);
                if (u != null && u.moveToFirst()) {
                    int idx = u.getColumnIndex("email");
                    if (idx >= 0) email = u.getString(idx);
                    u.close();
                }
                if (email != null) {
                    c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE author_email=? ORDER BY id DESC",
                            new String[]{ email });
                }
            } else if (hasUserIdFk) {
                // ถ้ามี user_id FK → หา id ของ user ก่อน
                Integer userId = null;
                Cursor u = db.rawQuery("SELECT id FROM " + TABLE_USERS + " WHERE username=? LIMIT 1",
                        new String[]{ username });
                if (u != null && u.moveToFirst()) {
                    userId = u.getInt(0);
                    u.close();
                }
                if (userId != null) {
                    c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE user_id=? ORDER BY id DESC",
                            new String[]{ String.valueOf(userId) });
                }
            }

            // ถ้าไม่มีคอลัมน์พวกนี้เลย → fallback ดึงทั้งหมด
            if (c == null) {
                c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " ORDER BY id DESC", null);
            }

            if (c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow("id"));
                    String title = safeGet(c, "title");
                    String tagline = safeGet(c, "tagline");
                    String tag = safeGet(c, "tag");
                    String category = safeGet(c, "category");
                    String imagePath = safeGet(c, "image_path");

                    WritingItem item = new WritingItem(id, title, tagline, tag, category, imagePath);
                    list.add(item);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }

        return list;
    }

    // helper สำหรับเช็คว่าตารางมีคอลัมน์นั้นไหม
    private boolean hasColumn(SQLiteDatabase db, String table, String column) {
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                if (column.equalsIgnoreCase(name)) return true;
            }
        } finally {
            if (c != null) c.close();
        }
        return false;
    }


    // ---------- Episodes ----------
    public boolean insertEpisode(int writingId, String title, String html, boolean isPrivate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("writing_id", writingId);
        cv.put("title", title);
        cv.put("content_html", html);
        cv.put("privacy_settings", isPrivate ? "private" : "public");

        int nextNo = getMaxEpisodeNoForWriting(writingId) + 1;
        cv.put("episode_no", nextNo);

        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yy HH:mm:ss 'GMT+7'");
        f.setTimeZone(java.util.TimeZone.getTimeZone("GMT+7"));
        String nowText = f.format(new java.util.Date());
        cv.put("created_at_text", nowText);
        cv.put("updated_at_text", nowText);

        try {
            return db.insertOrThrow(TABLE_EPISODES, null, cv) != -1;
        } catch (Exception e) {
            Log.e("DBHelper", "insertEpisode failed", e);
            return false;
        }
    }

    public boolean updateEpisode(int episodeId, String title, String html, boolean isPrivate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("content_html", html);
        cv.put("privacy_settings", isPrivate ? "private" : "public");

        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yy HH:mm:ss 'GMT+7'");
        f.setTimeZone(java.util.TimeZone.getTimeZone("GMT+7"));
        cv.put("updated_at_text", f.format(new java.util.Date()));

        return db.update(TABLE_EPISODES, cv, "episode_id=?", new String[]{String.valueOf(episodeId)}) > 0;
    }

    public boolean deleteEpisode(int episodeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EPISODES, "episode_id=?", new String[]{String.valueOf(episodeId)}) > 0;
    }

    public List<Episode> getEpisodesByWritingId(int writingId) {
        List<Episode> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT episode_id, writing_id, title, content_html, privacy_settings, episode_no, created_at_text, updated_at_text " +
                        "FROM " + TABLE_EPISODES + " WHERE writing_id=? ORDER BY episode_no ASC, episode_id ASC",
                new String[]{String.valueOf(writingId)})) {
            while (c.moveToNext()) {
                Episode e = new Episode();
                e.episodeId = c.getInt(0);
                e.writingId = c.getInt(1);
                e.title = c.getString(2);
                e.contentHtml = c.getString(3);
                e.isPrivate = "private".equalsIgnoreCase(c.getString(4));
                e.episodeNo = c.isNull(5) ? 0 : c.getInt(5);
                e.createdAt = 0;
                e.updatedAt = 0;
                list.add(e);
            }
        }
        return list;
    }

    public Episode getEpisodeById(int episodeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Episode e = null;
        try (Cursor c = db.rawQuery(
                "SELECT episode_id, writing_id, title, content_html, privacy_settings, episode_no, created_at_text, updated_at_text " +
                        "FROM " + TABLE_EPISODES + " WHERE episode_id=? LIMIT 1",
                new String[]{String.valueOf(episodeId)})) {
            if (c.moveToFirst()) {
                e = new Episode();
                e.episodeId = c.getInt(0);
                e.writingId = c.getInt(1);
                e.title = c.getString(2);
                e.contentHtml = c.getString(3);
                e.isPrivate = "private".equalsIgnoreCase(c.getString(4));
                e.episodeNo = c.isNull(5) ? 0 : c.getInt(5);
                e.createdAt = 0;
                e.updatedAt = 0;
            }
        }
        return e;
    }

    public void backfillEpisodeNumbersIfNeeded() {
        SQLiteDatabase dbw = getWritableDatabase();
        try (Cursor cw = dbw.rawQuery("SELECT id FROM " + TABLE_WRITINGS, null)) {
            while (cw.moveToNext()) {
                int wid = cw.getInt(0);
                int no = getMaxEpisodeNoForWriting(wid) + 1;
                try (Cursor ce = dbw.rawQuery(
                        "SELECT episode_id FROM " + TABLE_EPISODES +
                                " WHERE writing_id=? AND (episode_no IS NULL OR episode_no=0) ORDER BY episode_id ASC",
                        new String[]{String.valueOf(wid)})) {
                    while (ce.moveToNext()) {
                        int eid = ce.getInt(0);
                        ContentValues cv = new ContentValues();
                        cv.put("episode_no", no++);
                        dbw.update(TABLE_EPISODES, cv, "episode_id=?", new String[]{String.valueOf(eid)});
                    }
                }
            }
        }
    }

    private int getMaxEpisodeNoForWriting(int writingId) {
        int max = 0;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT MAX(episode_no) FROM " + TABLE_EPISODES + " WHERE writing_id=?",
                new String[]{String.valueOf(writingId)})) {
            if (c.moveToFirst()) max = c.isNull(0) ? 0 : c.getInt(0);
        }
        return max;
    }

    private String safeGet(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getString(idx) : null;
    }

    // Feed สำหรับหน้า “ล่าสุด”
    public static class EpisodeFeed {
        public int episodeId, writingId, episodeNo;
        public String episodeTitle, writingTitle;
    }

    public List<EpisodeFeed> getEpisodeFeed() {
        SQLiteDatabase db = getReadableDatabase();
        String sql =
                "SELECT e.episode_id, e.writing_id, IFNULL(e.episode_no,0), e.title AS ep_title, w.title AS w_title " +
                        "FROM " + TABLE_EPISODES + " e " +
                        "JOIN " + TABLE_WRITINGS + " w ON e.writing_id = w.id " +
                        "WHERE e.privacy_settings = 'public' " +
                        "ORDER BY e.episode_id DESC";
        List<EpisodeFeed> list = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                EpisodeFeed f = new EpisodeFeed();
                f.episodeId = c.getInt(0);
                f.writingId = c.getInt(1);
                f.episodeNo = c.getInt(2);
                f.episodeTitle = c.getString(3);
                f.writingTitle = c.getString(4);
                list.add(f);
            }
        }
        return list;
    }
}
