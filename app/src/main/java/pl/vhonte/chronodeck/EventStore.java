package pl.vhonte.chronodeck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class EventStore extends SQLiteOpenHelper {
    static final String DB_NAME = "chronodeck.db";
    static final int DB_VERSION = 1;
    static final String PLANNED = "PLANNED", RUNNING = "RUNNING", ENDED = "ENDED", DONE = "DONE";

    static final class Event {
        long id; String title; long day; int startMin; int endMin; String status;
        Event(long id, String title, long day, int startMin, int endMin, String status) {
            this.id=id; this.title=title; this.day=day; this.startMin=startMin; this.endMin=endMin; this.status=status;
        }
        String timeRange() { return clock(startMin) + " – " + clock(endMin); }
    }

    EventStore(Context context) { super(context, DB_NAME, null, DB_VERSION); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day INTEGER NOT NULL, start_min INTEGER NOT NULL, end_min INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'PLANNED')");
        db.execSQL("CREATE INDEX events_day_start ON events(day, start_min)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    static long todayKey() {
        java.text.SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return Long.parseLong(f.format(new Date()));
    }
    List<Event> forToday() {
        List<Event> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("events", null, "day=?", new String[]{String.valueOf(todayKey())}, null, null, "start_min ASC, _id ASC")) {
            while (c.moveToNext()) list.add(new Event(c.getLong(c.getColumnIndexOrThrow("_id")), c.getString(c.getColumnIndexOrThrow("title")), c.getLong(c.getColumnIndexOrThrow("day")), c.getInt(c.getColumnIndexOrThrow("start_min")), c.getInt(c.getColumnIndexOrThrow("end_min")), c.getString(c.getColumnIndexOrThrow("status"))));
        }
        return list;
    }
    long add(String title, int start, int end) {
        ContentValues v = new ContentValues(); v.put("title", title); v.put("day", todayKey()); v.put("start_min", start); v.put("end_min", end); v.put("status", PLANNED);
        return getWritableDatabase().insert("events", null, v);
    }
    void setStatus(long id, String status) {
        if (!PLANNED.equals(status) && !RUNNING.equals(status) && !ENDED.equals(status) && !DONE.equals(status)) return;
        ContentValues v = new ContentValues(); v.put("status", status);
        getWritableDatabase().update("events", v, "_id=?", new String[]{String.valueOf(id)});
    }
    Event byId(long id) {
        try (Cursor c = getReadableDatabase().query("events", null, "_id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) return new Event(c.getLong(c.getColumnIndexOrThrow("_id")), c.getString(c.getColumnIndexOrThrow("title")), c.getLong(c.getColumnIndexOrThrow("day")), c.getInt(c.getColumnIndexOrThrow("start_min")), c.getInt(c.getColumnIndexOrThrow("end_min")), c.getString(c.getColumnIndexOrThrow("status")));
        }
        return null;
    }
    Event currentOrNext() {
        List<Event> list = forToday();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int now = cal.get(java.util.Calendar.HOUR_OF_DAY)*60 + cal.get(java.util.Calendar.MINUTE);
        for (Event e : list) if (RUNNING.equals(e.status)) return e;
        for (Event e : list) if (!DONE.equals(e.status) && !ENDED.equals(e.status) && now >= e.startMin && now < e.endMin) return e;
        for (Event e : list) if (!DONE.equals(e.status) && !ENDED.equals(e.status) && e.startMin >= now) return e;
        for (Event e : list) if (!DONE.equals(e.status) && !ENDED.equals(e.status)) return e;
        return null;
    }
    static String clock(int min) { return String.format(Locale.getDefault(), "%02d:%02d", min/60, min%60); }
}
