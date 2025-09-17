package com.example.storysphere_appbar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import com.example.storysphere_appbar.Episode;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SPdb.db";
    public static final String TABLE_USERS = "User";
    public static final String TABLE_WRITINGS = "TABLE_WRITINGS";
    public static final String TABLE_EPISODES = "episodes";

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    // สร้างตาราง episodes ถ้ายังไม่มี (ใช้ชื่อคงเดิม: TABLE_EPISODES/TABLE_WRITINGS)
    // สร้างตาราง episodes สคีมาใหม่ (TEXT + FK → TABLE_WRITINGS)
    public void ensureEpisodesTable() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_EPISODES + " (" +
                        "episode_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "writing_id INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "content_html TEXT NOT NULL, " +
                        "privacy_settings TEXT NOT NULL CHECK(privacy_settings IN ('public','private')) DEFAULT 'public', " +
                        "episode_no INTEGER, " +
                        "created_at_text TEXT NOT NULL, " +
                        "updated_at_text TEXT NOT NULL, " +
                        "FOREIGN KEY (writing_id) REFERENCES " + TABLE_WRITINGS + "(id) ON DELETE CASCADE" +
                        ")"
        );
    }


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 7);
    }

    // ✅ ใช้ไฟล์ฐานข้อมูลจาก assets → ไม่สร้างตารางซ้ำ
    @Override
    public void onCreate(SQLiteDatabase db) {
        // no-op: ใช้สคีมาจากไฟล์ SPdb.db
    }

    // ✅ ไม่แก้สคีมาจากโค้ด เพื่อไม่ชนกับไฟล์จริง
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // อนุญาต FK ขณะไมเกรต
        db.execSQL("PRAGMA foreign_keys=OFF;");
        db.beginTransaction();
        try {
            if (oldVersion < 7) {
                // สร้างตารางใหม่ตามสคีมาใหม่
                db.execSQL(
                        "CREATE TABLE IF NOT EXISTS episodes_new (" +
                                "episode_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "writing_id INTEGER NOT NULL, " +
                                "title TEXT NOT NULL, " +
                                "content_html TEXT NOT NULL, " +
                                "privacy_settings TEXT NOT NULL CHECK(privacy_settings IN ('public','private')) DEFAULT 'public', " +
                                "episode_no INTEGER, " +
                                "created_at_text TEXT NOT NULL, " +
                                "updated_at_text TEXT NOT NULL, " +
                                "FOREIGN KEY (writing_id) REFERENCES " + TABLE_WRITINGS + "(id) ON DELETE CASCADE" +
                                ")"
                );

                // คัดลอกข้อมูลจากตาราง episodes เก่าหากมี
                // รองรับทุกกรณี: มีคอลัมน์ is_private/created_at/updated_at หรือไม่มี
                db.execSQL(
                        "INSERT INTO episodes_new (" +
                                "episode_id, writing_id, title, content_html, privacy_settings, episode_no, created_at_text, updated_at_text" +
                                ") " +
                                "SELECT " +
                                "episode_id, " +
                                "writing_id, " +
                                "title, " +
                                "content_html, " +
                                // map is_private (0/1) → 'public'/'private' ถ้ามี; ถ้ามี privacy_settings แล้วให้ใช้เลย
                                "CASE " +
                                "WHEN (SELECT 1 FROM pragma_table_info('episodes') WHERE name='privacy_settings') = 1 THEN privacy_settings " +
                                "WHEN (SELECT 1 FROM pragma_table_info('episodes') WHERE name='is_private') = 1 THEN CASE WHEN is_private=1 THEN 'private' ELSE 'public' END " +
                                "ELSE 'public' " +
                                "END AS privacy_settings, " +
                                "episode_no, " +
                                // สร้างข้อความเวลา ถ้ามี created_at_text/updated_at_text ก็ใช้เลย
                                "CASE " +
                                "WHEN (SELECT 1 FROM pragma_table_info('episodes') WHERE name='created_at_text') = 1 THEN created_at_text " +
                                "WHEN (SELECT 1 FROM pragma_table_info('episodes') WHERE name='created_at') = 1 THEN " +
                                "(strftime('%d/%m/%y %H:%M:%S', created_at/1000, 'unixepoch', '+7 hours') || ' GMT+7') " +
                                "ELSE (strftime('%d/%m/%y %H:%M:%S','now','localtime') || ' GMT+7') " +
                                "END AS created_at_text, " +
                                "CASE " +
                                "WHEN (SELECT 1 FROM pragma_table_info('episodes') WHERE name='updated_at_text') = 1 THEN updated_at_text " +
                                "WHEN (SELECT 1 FROM pragma_table_info('episodes') WHERE name='updated_at') = 1 THEN " +
                                "(strftime('%d/%m/%y %H:%M:%S', updated_at/1000, 'unixepoch', '+7 hours') || ' GMT+7') " +
                                "ELSE (strftime('%d/%m/%y %H:%M:%S','now','localtime') || ' GMT+7') " +
                                "END AS updated_at_text " +
                                "FROM episodes"
                );

                // สลับตาราง
                db.execSQL("DROP TABLE IF EXISTS episodes;");
                db.execSQL("ALTER TABLE episodes_new RENAME TO episodes;");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ======================== Users ========================
    // ปลอดภัย: ถ้า email ว่าง ให้คืน null/false โดยไม่ยิง SQL
    public Cursor getUserByEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE email = ?", new String[]{ email });
    }

    public boolean updateUser(String email, String newUsername, String newPassword) {
        if (email == null || email.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", newUsername);
        values.put("password", newPassword);
        int rows = db.update(TABLE_USERS, values, "email = ?", new String[]{ email });
        return rows > 0;
    }

    public boolean updateUser(String email, String newUsername, String newPassword, String imageUri) {
        if (email == null || email.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", newUsername);
        values.put("password", newPassword);
        if (imageUri != null) values.put("image_uri", imageUri);
        int rows = db.update(TABLE_USERS, values, "email = ?", new String[]{ email });
        return rows > 0;
    }

    public boolean deleteUser(String email) {
        if (email == null || email.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USERS, "email = ?", new String[]{ email }) > 0;
    }
    public boolean insertUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("email", email);
        values.put("password", password);
        return db.insert(TABLE_USERS, null, values) != -1;
    }

    // ======================== Writings ========================
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

    public Cursor getAllWritingsCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " ORDER BY id DESC", null);
    }
    public Cursor getAllWritings() {
        return getAllWritingsCursor();
    }

    public List<WritingItem> getAllWritingItems() {
        List<WritingItem> writingList = new ArrayList<>();
        Cursor cursor = getAllWritingsCursor();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String tagline = cursor.getString(cursor.getColumnIndexOrThrow("tagline"));
                String tag = cursor.getString(cursor.getColumnIndexOrThrow("tag"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));

                WritingItem item = new WritingItem(id, title, tagline, tag, category, imagePath);
                // ถ้าต้องการเก็บ content ใน item ใช้ setter ที่คุณมีอยู่
                item.setTag(content);
                writingList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return writingList;
    }

    public boolean deleteWriting(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WRITINGS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean updateWriting(int id, String title, String tagline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("tagline", tagline);
        int result = db.update(TABLE_WRITINGS, values, "id = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public Cursor getWritingById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE id = ?", new String[]{String.valueOf(id)});
    }

    // 📌 เพิ่ม method นี้เข้าไปใน DBHelper (คงชื่อเดิม)
    public boolean insertBook(String title, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("image_path", imageUri);
        long result = db.insert(TABLE_WRITINGS, null, values);
        return result != -1;
    }
    public boolean writingExists(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_WRITINGS + " WHERE id=? LIMIT 1",
                new String[]{ String.valueOf(id) })) {
            return c.moveToFirst();
        }
    }

    // ดึงนิยายตามแท็ก/หมวดแบบไม่สนตัวพิมพ์ และรองรับ tag ที่คั่นด้วยจุลภาค
    public List<WritingItem> getWritingItemsByTag(String rawTag, int limit) {
        if (rawTag == null) rawTag = "";
        String norm = rawTag.trim().toLowerCase();       // "romance", "sci-fi", ...
        String alt  = norm.replace("-", "");             // sci-fi -> scifi

        SQLiteDatabase db = getReadableDatabase();
        String sql =
                "SELECT id, title, tagline, tag, category, image_path " +
                        "FROM " + TABLE_WRITINGS + " " +
                        "WHERE " +
                        // เทียบ category ตรงตัว (ใส่ตัวพิมพ์เล็ก) และแบบตัดขีด
                        "  LOWER(IFNULL(category,'')) = ? " +
                        "  OR LOWER(REPLACE(IFNULL(category,''),'-','')) = ? " +
                        // หาใน tag (คั่นด้วย ,) ป้องกันติดคำย่อย
                        "  OR (',' || LOWER(REPLACE(IFNULL(tag,''), ' ', '')) || ',') LIKE ? " +
                        "  OR (',' || LOWER(REPLACE(IFNULL(tag,''), ' ', '')) || ',') LIKE ? " +
                        "ORDER BY id DESC " +
                        (limit > 0 ? "LIMIT " + limit : "");

        String like1 = "%," + norm + ",%";
        String like2 = "%," + alt  + ",%";

        Cursor c = db.rawQuery(sql, new String[]{ norm, alt, like1, like2 });
        List<WritingItem> list = new ArrayList<>();
        try {
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
        } finally { c.close(); }
        return list;
    }

    // ดึงรายการล่าสุดทั้งหมด (ไว้ใช้กับ "You may also like" / "Top Chart" เบื้องต้น)
    public List<WritingItem> getRecentWritings(int limit) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT id, title, tagline, tag, category, image_path " +
                "FROM " + TABLE_WRITINGS + " ORDER BY id DESC " +
                (limit > 0 ? "LIMIT " + limit : "");
        Cursor c = db.rawQuery(sql, null);
        List<WritingItem> list = new ArrayList<>();
        try {
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
        } finally { c.close(); }
        return list;
    }

    // ======================== Episodes ========================
    public boolean insertEpisode(int writingId, String title, String html, boolean isPrivate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("writing_id", writingId);
        cv.put("title", title);
        cv.put("content_html", html);

        // privacy_settings เป็นข้อความ
        cv.put("privacy_settings", isPrivate ? "private" : "public");

        // เลขตอนถัดไป
        int nextNo = getMaxEpisodeNoForWriting(writingId) + 1;
        cv.put("episode_no", nextNo);

        // เวลาเป็นข้อความ (GMT+7)
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yy HH:mm:ss 'GMT+7'");
        f.setTimeZone(java.util.TimeZone.getTimeZone("GMT+7"));
        String nowText = f.format(new java.util.Date());
        cv.put("created_at_text", nowText);
        cv.put("updated_at_text", nowText);

        long rowId;
        try {
            rowId = db.insertOrThrow(TABLE_EPISODES, null, cv);
        } catch (Exception e) {
            android.util.Log.e("DBHelper", "insertEpisode failed", e);
            rowId = -1;
        }
        return rowId != -1;
    }
    public boolean updateEpisode(int episodeId, String title, String html, boolean isPrivate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("content_html", html);
        cv.put("privacy_settings", isPrivate ? "private" : "public");

        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yy HH:mm:ss 'GMT+7'");
        f.setTimeZone(java.util.TimeZone.getTimeZone("GMT+7"));
        String nowText = f.format(new java.util.Date());
        cv.put("updated_at_text", nowText);

        int rows = db.update(TABLE_EPISODES, cv, "episode_id=?", new String[]{String.valueOf(episodeId)});
        return rows > 0;
    }

    public boolean deleteEpisode(int episodeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EPISODES, "episode_id=?", new String[]{String.valueOf(episodeId)}) > 0;
    }
    public List<Episode> getEpisodesByWritingId(int writingId) {
        List<Episode> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT episode_id, writing_id, title, content_html, privacy_settings, episode_no, created_at_text, updated_at_text " +
                        "FROM " + TABLE_EPISODES + " WHERE writing_id=? " +
                        "ORDER BY episode_no ASC, episode_id ASC",
                new String[]{ String.valueOf(writingId) }
        );
        if (c != null) {
            while (c.moveToNext()) {
                Episode e = new Episode();
                e.episodeId   = c.getInt(0);
                e.writingId   = c.getInt(1);
                e.title       = c.getString(2);
                e.contentHtml = c.getString(3);
                String privacy = c.getString(4);
                e.isPrivate   = "private".equalsIgnoreCase(privacy);
                e.episodeNo   = c.isNull(5) ? 0 : c.getInt(5);
                // created_at_text / updated_at_text เป็นข้อความ แต่ใน Episode เป็น long → ปล่อยเป็น 0
                e.createdAt   = 0;
                e.updatedAt   = 0;
                list.add(e);
            }
            c.close();
        }
        return list;
    }
    public Episode getEpisodeById(int episodeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT episode_id, writing_id, title, content_html, privacy_settings, episode_no, created_at_text, updated_at_text " +
                        "FROM " + TABLE_EPISODES + " WHERE episode_id=? LIMIT 1",
                new String[]{ String.valueOf(episodeId) }
        );
        Episode e = null;
        if (c != null && c.moveToFirst()) {
            e = new Episode();
            e.episodeId   = c.getInt(0);
            e.writingId   = c.getInt(1);
            e.title       = c.getString(2);
            e.contentHtml = c.getString(3);
            String privacy = c.getString(4);
            e.isPrivate   = "private".equalsIgnoreCase(privacy);
            e.episodeNo   = c.isNull(5) ? 0 : c.getInt(5);
            e.createdAt   = 0;
            e.updatedAt   = 0;
            c.close();
        }
        return e;
    }

    public void backfillEpisodeNumbersIfNeeded() {
        SQLiteDatabase dbw = getWritableDatabase();
        Cursor cw = dbw.rawQuery("SELECT id FROM " + TABLE_WRITINGS, null);
        if (cw != null) {
            while (cw.moveToNext()) {
                int wid = cw.getInt(0);
                Cursor ce = dbw.rawQuery(
                        "SELECT episode_id FROM " + TABLE_EPISODES +
                                " WHERE writing_id=? AND (episode_no IS NULL OR episode_no=0) ORDER BY episode_id ASC",
                        new String[]{ String.valueOf(wid) }
                );
                int no = getMaxEpisodeNoForWriting(wid) + 1;
                if (ce != null) {
                    while (ce.moveToNext()) {
                        int eid = ce.getInt(0);
                        ContentValues cv = new ContentValues();
                        cv.put("episode_no", no++);
                        dbw.update(TABLE_EPISODES, cv, "episode_id=?", new String[]{ String.valueOf(eid) });
                    }
                    ce.close();
                }
            }
            cw.close();
        }
    }

    private int getMaxEpisodeNoForWriting(int writingId) {
        int max = 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT MAX(episode_no) FROM " + TABLE_EPISODES + " WHERE writing_id=?",
                new String[]{ String.valueOf(writingId) }
        );
        if (c != null && c.moveToFirst()) {
            max = c.isNull(0) ? 0 : c.getInt(0);
            c.close();
        }
        return max;
    }

    private boolean tableExists(SQLiteDatabase db, String tableName) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{ tableName }
            );
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }
    // helper ตรวจว่าตารางมีคอลัมน์ไหม (เพิ่มไว้ในคลาส DBHelper ตำแหน่งไหนก็ได้)
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

    // ===== Feed สำหรับหน้า ReadingMainActivity =====
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
                        "ORDER BY e.episode_id DESC"; // เรียงตอนล่าสุดก่อน
        Cursor c = db.rawQuery(sql, null);
        List<EpisodeFeed> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                EpisodeFeed f = new EpisodeFeed();
                f.episodeId = c.getInt(0);
                f.writingId = c.getInt(1);
                f.episodeNo = c.getInt(2);
                f.episodeTitle = c.getString(3);
                f.writingTitle = c.getString(4);
                list.add(f);
            }
        } finally { c.close(); }
        return list;
    }

    // --- Users ---
    public Cursor getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username = ?", new String[]{username});
    }

    // --- Writings by author username (fallback ถ้าไม่มีคอลัมน์ผูกผู้เขียน) ---
    public List<WritingItem> getWritingItemsByUsername(String username) {
        SQLiteDatabase db = getReadableDatabase();

        // ตรวจว่ามีคอลัมน์อ้างผู้เขียนหรือไม่
        boolean hasAuthorUsername = hasColumn(db, TABLE_WRITINGS, "author_username");
        boolean hasAuthorEmail    = hasColumn(db, TABLE_WRITINGS, "author_email");
        boolean hasUserIdFk       = hasColumn(db, TABLE_WRITINGS, "user_id");

        List<WritingItem> list = new java.util.ArrayList<>();

        Cursor c = null;
        try {
            if (hasAuthorUsername) {
                c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE author_username=? ORDER BY id DESC",
                        new String[]{ username });
            } else if (hasAuthorEmail) {
                // กรณีฐานข้อมูลเก่าเก็บ email แต่เรามี username → map ผ่านตาราง User
                String email = null;
                Cursor u = getUserByUsername(username);
                if (u != null && u.moveToFirst()) {
                    int idx = u.getColumnIndex("email");
                    if (idx >= 0) email = u.getString(idx);
                }
                if (u != null) u.close();

                if (email != null) {
                    c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE author_email=? ORDER BY id DESC",
                            new String[]{ email });
                }
            } else if (hasUserIdFk) {
                // ถ้า WRITINGS ผูกกับ user_id
                Integer userId = null;
                Cursor u = db.rawQuery("SELECT id FROM " + TABLE_USERS + " WHERE username=? LIMIT 1",
                        new String[]{ username });
                if (u != null && u.moveToFirst()) userId = u.getInt(0);
                if (u != null) u.close();

                if (userId != null) {
                    c = db.rawQuery("SELECT * FROM " + TABLE_WRITINGS + " WHERE user_id=? ORDER BY id DESC",
                            new String[]{ String.valueOf(userId) });
                }
            }

            // ถ้ายังหาไม่ได้ (ไม่มีคอลัมน์ผูกผู้เขียน) → fallback ทั้งหมด
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

    // helper ปลอดภัยเวลาคอลัมน์อาจไม่มี
    private String safeGet(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getString(idx) : null;
    }

}
