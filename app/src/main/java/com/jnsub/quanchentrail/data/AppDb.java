package com.jnsub.quanchentrail.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class AppDb extends SQLiteOpenHelper {
    public static final String USER_ID = "local_user";
    private static final String DB_NAME = "quanchen_trail.db";
    private static final int DB_VERSION = 5;
    private final Context context;

    public AppDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
    }

    public SQLiteDatabase openOrRecover() {
        try {
            return getWritableDatabase();
        } catch (RuntimeException first) {
            try {
                close();
                context.deleteDatabase(DB_NAME);
                return getWritableDatabase();
            } catch (RuntimeException second) {
                second.addSuppressed(first);
                throw second;
            }
        }
    }

    public static final class CheckinWrite {
        public final java.util.List<String> affectedLines;
        public final String group;

        CheckinWrite(java.util.List<String> affectedLines, String group) {
            this.affectedLines = affectedLines;
            this.group = group;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE achievements ADD COLUMN description TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE checkins ADD COLUMN photo_uris_json TEXT NOT NULL DEFAULT '[]'");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE checkins ADD COLUMN has_photo INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE checkins_v5 (checkin_id INTEGER PRIMARY KEY AUTOINCREMENT, checkin_group TEXT NOT NULL, user_id TEXT NOT NULL, station_name TEXT NOT NULL, line TEXT NOT NULL, checked_in_at INTEGER NOT NULL, photo_count INTEGER NOT NULL DEFAULT 0, has_photo INTEGER NOT NULL DEFAULT 0, photo_uris_json TEXT NOT NULL DEFAULT '[]')");
            db.execSQL("INSERT INTO checkins_v5 (checkin_group,user_id,station_name,line,checked_in_at,photo_count,has_photo,photo_uris_json) SELECT 'legacy_'||rowid,user_id,station_name,line,checked_in_at,photo_count,has_photo,photo_uris_json FROM checkins");
            db.execSQL("DROP TABLE checkins");
            db.execSQL("ALTER TABLE checkins_v5 RENAME TO checkins");
            db.execSQL("CREATE INDEX idx_checkins_group ON checkins(checkin_group)");
            db.execSQL("CREATE INDEX idx_checkins_station_time ON checkins(user_id,station_name,checked_in_at)");
        }
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE stations ("
                + "station_id TEXT PRIMARY KEY,"
                + "name TEXT NOT NULL UNIQUE,"
                + "lines_json TEXT NOT NULL,"
                + "lat REAL NOT NULL,"
                + "lng REAL NOT NULL,"
                + "system_type TEXT NOT NULL,"
                + "operational_status TEXT NOT NULL)");
        db.execSQL("CREATE TABLE transfers ("
                + "station_name TEXT PRIMARY KEY,"
                + "lines_json TEXT NOT NULL)");
        db.execSQL("CREATE TABLE poi_mapping ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "station_name TEXT NOT NULL,"
                + "poi_name TEXT NOT NULL,"
                + "category TEXT NOT NULL)");
        db.execSQL("CREATE TABLE checkins ("
                + "checkin_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "checkin_group TEXT NOT NULL,"
                + "user_id TEXT NOT NULL,"
                + "station_name TEXT NOT NULL,"
                + "line TEXT NOT NULL,"
                + "checked_in_at INTEGER NOT NULL,"
                + "photo_count INTEGER NOT NULL DEFAULT 0,"
                + "has_photo INTEGER NOT NULL DEFAULT 0,"
                + "photo_uris_json TEXT NOT NULL DEFAULT '[]')");
        db.execSQL("CREATE INDEX idx_checkins_group ON checkins(checkin_group)");
        db.execSQL("CREATE INDEX idx_checkins_station_time ON checkins(user_id,station_name,checked_in_at)");
        db.execSQL("CREATE TABLE achievements ("
                + "achievement_id TEXT PRIMARY KEY,"
                + "name TEXT NOT NULL,"
                + "condition_type TEXT NOT NULL,"
                + "description TEXT NOT NULL DEFAULT '',"
                + "line TEXT,"
                + "target INTEGER NOT NULL,"
                + "target_set_json TEXT NOT NULL)");
        db.execSQL("CREATE TABLE lines ("
                + "line TEXT PRIMARY KEY,"
                + "name TEXT NOT NULL,"
                + "system_type TEXT NOT NULL,"
                + "color TEXT NOT NULL,"
                + "route TEXT NOT NULL,"
                + "target INTEGER NOT NULL,"
                + "status TEXT NOT NULL,"
                + "sort_order INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_checkins_station ON checkins(station_name)");
        db.execSQL("CREATE INDEX idx_poi_station ON poi_mapping(station_name)");
    }


    private void seed(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            JSONArray lines = new JSONArray(readAsset("lines.json"));
            for (int i = 0; i < lines.length(); i++) {
                JSONObject o = lines.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("line", o.getString("line"));
                v.put("name", o.getString("name"));
                v.put("system_type", o.getString("system_type"));
                v.put("color", o.getString("color"));
                v.put("route", o.getString("from") + " ↔ " + o.getString("to"));
                v.put("target", o.getInt("target"));
                v.put("status", o.getString("status"));
                v.put("sort_order", i);
                db.insert("lines", null, v);
            }

            JSONArray stations = new JSONArray(readAsset("stations.json"));
            for (int i = 0; i < stations.length(); i++) {
                JSONObject o = stations.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("station_id", o.getString("station_id"));
                v.put("name", o.getString("name"));
                v.put("lines_json", o.getJSONArray("lines").toString());
                v.put("lat", o.getDouble("lat"));
                v.put("lng", o.getDouble("lng"));
                v.put("system_type", o.getString("system_type"));
                v.put("operational_status", o.getString("operational_status"));
                db.insert("stations", null, v);
            }

            JSONArray transfers = new JSONArray(readAsset("transfers.json"));
            for (int i = 0; i < transfers.length(); i++) {
                JSONObject o = transfers.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("station_name", o.getString("station_name"));
                v.put("lines_json", o.getJSONArray("lines").toString());
                db.insert("transfers", null, v);
            }

            JSONArray pois = new JSONArray(readAsset("poi_mapping.json"));
            for (int i = 0; i < pois.length(); i++) {
                JSONObject o = pois.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("station_name", o.getString("station_name"));
                v.put("poi_name", o.getString("poi_name"));
                v.put("category", o.getString("category"));
                db.insert("poi_mapping", null, v);
            }

            JSONArray achievements = new JSONArray(readAsset("achievements.json"));
            for (int i = 0; i < achievements.length(); i++) {
                JSONObject o = achievements.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("achievement_id", o.getString("achievement_id"));
                v.put("name", o.getString("name"));
                v.put("description", o.optString("description", ""));
                v.put("condition_type", o.getString("condition_type"));
                v.put("line", o.has("line") ? o.optString("line", null) : null);
                v.put("target", o.getInt("target"));
                v.put("target_set_json", o.getJSONArray("target_station_set").toString());
                db.insert("achievements", null, v);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to seed station knowledge base", e);
        } finally {
            db.endTransaction();
        }
    }

    private String readAsset(String name) {
        try (InputStream in = context.getAssets().open(name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Missing asset: " + name, e);
        }
    }

    public List<Station> stations(boolean activeOnly) {
        List<Station> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] args = activeOnly ? new String[]{"active"} : null;
        try (Cursor c = db.query("stations", null,
                activeOnly ? "operational_status=?" : null, args,
                null, null, "name COLLATE NOCASE")) {
            while (c.moveToNext()) {
                result.add(readStation(c));
            }
        }
        return result;
    }

    public Station station(String name) {
        if (name == null) return null;
        try (Cursor c = getReadableDatabase().query("stations", null, "name=?",
                new String[]{name}, null, null, null, "1")) {
            return c.moveToFirst() ? readStation(c) : null;
        }
    }

    private Station readStation(Cursor c) {
        try {
            JSONArray arr = new JSONArray(c.getString(c.getColumnIndexOrThrow("lines_json")));
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) lines.add(arr.getString(i));
            return new Station(
                    c.getString(c.getColumnIndexOrThrow("station_id")),
                    c.getString(c.getColumnIndexOrThrow("name")),
                    lines,
                    c.getDouble(c.getColumnIndexOrThrow("lat")),
                    c.getDouble(c.getColumnIndexOrThrow("lng")),
                    c.getString(c.getColumnIndexOrThrow("system_type")),
                    c.getString(c.getColumnIndexOrThrow("operational_status")));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid station row", e);
        }
    }

    public boolean isTransfer(String stationName) {
        try (Cursor c = getReadableDatabase().query("transfers", new String[]{"station_name"},
                "station_name=?", new String[]{stationName}, null, null, null, "1")) {
            return c.moveToFirst();
        }
    }

    public List<String> transferLines(String stationName) {
        Station station = station(stationName);
        if (station == null) return Collections.emptyList();
        try (Cursor c = getReadableDatabase().query("transfers", new String[]{"lines_json"},
                "station_name=?", new String[]{stationName}, null, null, null, "1")) {
            if (c.moveToFirst()) {
                try {
                    JSONArray arr = new JSONArray(c.getString(0));
                    List<String> lines = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) lines.add(arr.getString(i));
                    return lines;
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid transfer row", e);
                }
            }
        }
        return new ArrayList<>(station.lines);
    }

    public List<String> affectedLines(String stationName) {
        LinkedHashSet<String> lines = new LinkedHashSet<>(transferLines(stationName));
        Station station = station(stationName);
        if (station != null) lines.addAll(station.lines);
        return new ArrayList<>(lines);
    }

    public List<Poi> pois(String stationName) {
        List<Poi> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("poi_mapping",
                new String[]{"station_name", "poi_name", "category"},
                "station_name=?", new String[]{stationName}, null, null, "_id")) {
            while (c.moveToNext()) {
                result.add(new Poi(c.getString(0), c.getString(1), c.getString(2)));
            }
        }
        return result;
    }

    public boolean isChecked(String stationName) {
        try (Cursor c = getReadableDatabase().query("checkins", new String[]{"line"},
                "user_id=? AND station_name=?", new String[]{USER_ID, stationName},
                null, null, null, "1")) {
            return c.moveToFirst();
        }
    }

    public String lineColor(String line) {
        return scalar("SELECT color FROM lines WHERE line=?", line, "#746C7D");
    }

    public String lineName(String line) {
        return scalar("SELECT name FROM lines WHERE line=?", line, line);
    }

    public String lineStatus(String line) {
        return scalar("SELECT status FROM lines WHERE line=?", line, "pending");
    }

    private String scalar(String sql, String arg, String fallback) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{arg})) {
            return c.moveToFirst() ? c.getString(0) : fallback;
        }
    }

    public CheckinWrite writeCheckin(String stationName, String line, int photoCount, long now) {
        Station station = station(stationName);
        if (station == null || !"active".equals(station.operationalStatus)) {
            throw new IllegalArgumentException("Station is not active: " + stationName);
        }
        java.util.List<String> affected = affectedLines(stationName);
        String checkinGroup = java.util.UUID.randomUUID().toString();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int safePhotos = Math.max(0, Math.min(3, photoCount));
            for (String affectedLine : affected) {
                ContentValues v = new ContentValues();
                v.put("checkin_group", checkinGroup);
                v.put("user_id", USER_ID);
                v.put("station_name", stationName);
                v.put("line", affectedLine);
                v.put("checked_in_at", now);
                v.put("photo_count", safePhotos);
                v.put("has_photo", safePhotos > 0 ? 1 : 0);
                v.put("photo_uris_json", "[]");
                db.insert("checkins", null, v);
            }
            db.setTransactionSuccessful();
            return new CheckinWrite(affected, checkinGroup);
        } finally {
            db.endTransaction();
        }
    }


    private Map<String, String[]> loadLineSchedules() {
        Map<String, String[]> schedules = new LinkedHashMap<>();
        try {
            JSONArray array = new JSONArray(readAsset("lines.json"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                schedules.put(o.getString("line"), new String[]{
                        o.optString("first_train", ""),
                        o.optString("last_train", ""),
                        o.optString("schedule_note", ""),
                        o.optString("last_train_weekend", ""),
                        o.optString("schedule_display", "")
                });
            }
        } catch (Exception ignored) {
            // Schedule details are optional; progress cards still render without them.
        }
        return schedules;
    }

    public List<LineProgress> lineProgress() {
        List<LineProgress> result = new ArrayList<>();
        Map<String, String[]> schedules = loadLineSchedules();
        try (Cursor c = getReadableDatabase().query("lines", null, null, null,
                null, null, "sort_order")) {
            while (c.moveToNext()) {
                String line = c.getString(c.getColumnIndexOrThrow("line"));
                String[] schedule = schedules.getOrDefault(line, new String[]{"", "", "", "", ""});
                int target = c.getInt(c.getColumnIndexOrThrow("target"));
                int current = 0;
                if (target > 0) {
                    for (Station station : lineStations(line)) {
                        if (isChecked(station.name)) current++;
                    }
                }
                result.add(new LineProgress(
                        line,
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("system_type")),
                        c.getString(c.getColumnIndexOrThrow("color")),
                        c.getString(c.getColumnIndexOrThrow("route")).split(" ↔ ")[0],
                        c.getString(c.getColumnIndexOrThrow("route")).split(" ↔ ")[1],
                        current,
                        target,
                        c.getString(c.getColumnIndexOrThrow("status")),
                        schedule[0],
                        effectiveLastTrain(schedule[1], schedule[3]),
                        schedule[4],
                        schedule[2],
                        serviceStatus(schedule[0], effectiveLastTrain(schedule[1], schedule[3]),
                                c.getString(c.getColumnIndexOrThrow("status")))));
            }
        }
        return result;
    }

    private String effectiveLastTrain(String weekdayLast, String weekendLast) {
        if (LocalDate.now().getDayOfWeek().getValue() >= 5
                && weekendLast != null && !weekendLast.isEmpty()) {
            return weekendLast;
        }
        return weekdayLast == null ? "" : weekdayLast;
    }

    private String serviceStatus(String firstTrain, String lastTrain, String operationalStatus) {
        if (!"active".equals(operationalStatus)) return "未开通";
        try {
            if (firstTrain == null || firstTrain.isEmpty()
                    || lastTrain == null || lastTrain.isEmpty()) return "运营中";
            LocalTime start = LocalTime.parse(firstTrain);
            LocalTime end = LocalTime.parse(lastTrain);
            LocalTime now = LocalTime.now();
            boolean open = !now.isBefore(start) && !now.isAfter(end);
            return open ? "运营中" : "休息中";
        } catch (Exception ignored) {
            return "运营中";
        }
    }

    private java.util.Map<String, java.util.List<String>> loadLineOrder() {
        java.util.Map<String, java.util.List<String>> result = new LinkedHashMap<>();
        try {
            JSONObject root = new JSONObject(readAsset("line_station_order.json"));
            java.util.Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String line = keys.next();
                JSONArray array = root.getJSONArray(line);
                java.util.List<String> names = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) names.add(array.getString(i));
                result.put(line, names);
            }
        } catch (Exception ignored) {
            // Optional order asset; fallback keeps the page usable.
        }
        return result;
    }

    public java.util.List<Station> lineStations(String line) {
        java.util.List<Station> result = new java.util.ArrayList<>();
        java.util.Map<String, Station> byName = new LinkedHashMap<>();
        for (Station station : stations(true)) byName.put(station.name, station);
        java.util.List<String> order = loadLineOrder().get(line);
        if (order != null) {
            for (String name : order) {
                Station station = byName.get(name);
                if (station != null) result.add(station);
            }
            return result;
        }
        for (Station station : byName.values()) {
            if (station.lines.contains(line)) result.add(station);
        }
        return result;
    }

    public List<AchievementProgress> achievementProgress() {
        List<AchievementProgress> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("achievements", null, null, null,
                null, null, "rowid")) {
            while (c.moveToNext()) {
                String id = c.getString(c.getColumnIndexOrThrow("achievement_id"));
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String type = c.getString(c.getColumnIndexOrThrow("condition_type"));
                String description = c.isNull(c.getColumnIndexOrThrow("description"))
                        ? "" : c.getString(c.getColumnIndexOrThrow("description"));
                int target = c.getInt(c.getColumnIndexOrThrow("target"));
                String line = c.isNull(c.getColumnIndexOrThrow("line"))
                        ? null : c.getString(c.getColumnIndexOrThrow("line"));
                String setJson = c.getString(c.getColumnIndexOrThrow("target_set_json"));
                int current = calculateCurrent(type, line, setJson);
                result.add(new AchievementProgress(id, name, type, description, current, target));
            }
        }
        return result;
    }

    private int calculateCurrent(String type, String line, String setJson) {
        SQLiteDatabase db = getReadableDatabase();
        switch (type) {
            case "line":
                return count("SELECT COUNT(DISTINCT station_name) FROM checkins WHERE user_id=? AND line=?",
                        new String[]{USER_ID, line});
            case "station_set": {
                try {
                    JSONArray set = new JSONArray(setJson);
                    if (set.length() == 0) return 0;
                    StringBuilder placeholders = new StringBuilder();
                    List<String> args = new ArrayList<>();
                    args.add(USER_ID);
                    for (int i = 0; i < set.length(); i++) {
                        if (i > 0) placeholders.append(',');
                        placeholders.append('?');
                        args.add(set.getString(i));
                    }
                    return count("SELECT COUNT(DISTINCT station_name) FROM checkins "
                                    + "WHERE user_id=? AND station_name IN (" + placeholders + ")",
                            args.toArray(new String[0]));
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid achievement station set", e);
                }
            }
            case "any_transfer":
                return count("SELECT COUNT(DISTINCT c.station_name) FROM checkins c "
                                + "JOIN transfers t ON c.station_name=t.station_name "
                                + "WHERE c.user_id=?",
                        new String[]{USER_ID});
            case "system_types":
                // 只计入制式单一的打卡组：换乘站跨制式联动会把一次签到拆成多条不同制式的
                // checkins，若不过滤会凭空解锁“全制式行者”这类需要真正乘坐过该制式的成就。
                return count("SELECT COUNT(DISTINCT l.system_type) FROM checkins c "
                                + "JOIN lines l ON c.line=l.line WHERE c.user_id=? "
                                + "AND (SELECT COUNT(DISTINCT l2.system_type) FROM checkins c2 "
                                + "JOIN lines l2 ON c2.line=l2.line "
                                + "WHERE c2.checkin_group=c.checkin_group)=1",
                        new String[]{USER_ID});
            case "photo_checkins":
                return count("SELECT COUNT(DISTINCT station_name) FROM checkins "
                                + "WHERE user_id=? AND photo_count>0",
                        new String[]{USER_ID});
            default:
                return 0;
        }
    }

    public void updatePhotos(String checkinGroup, int photoCount, List<String> photoUris) {
        int safeCount = Math.max(0, Math.min(3, photoCount));
        JSONArray array = new JSONArray();
        if (photoUris != null) {
            for (String uri : photoUris) {
                if (uri != null && !uri.isEmpty()) array.put(uri);
            }
        }
        ContentValues values = new ContentValues();
        values.put("photo_count", safeCount);
        values.put("has_photo", safeCount > 0 ? 1 : 0);
        values.put("photo_uris_json", array.toString());
        SQLiteDatabase db = getWritableDatabase();
        db.update("checkins", values,
                "user_id=? AND checkin_group=?",
                new String[]{USER_ID, checkinGroup});
    }


    public java.util.List<String> achievementStations(String achievementId) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String type = null;
        String line = null;
        String setJson = "[]";
        try (Cursor c = getReadableDatabase().query("achievements",
                new String[]{"condition_type", "line", "target_set_json"},
                "achievement_id=?", new String[]{achievementId}, null, null, null, "1")) {
            if (c.moveToFirst()) {
                type = c.getString(0);
                line = c.isNull(1) ? null : c.getString(1);
                setJson = c.getString(2);
            }
        }
        if (type == null) return result;
        switch (type) {
            case "line":
                for (Station station : lineStations(line)) result.add(station.name);
                break;
            case "station_set":
                try {
                    JSONArray set = new JSONArray(setJson);
                    for (int i = 0; i < set.length(); i++) result.add(set.getString(i));
                } catch (Exception ignored) {}
                break;
            case "any_transfer":
                try {
                    JSONArray transfers = new JSONArray(readAsset("transfers.json"));
                    for (int i = 0; i < transfers.length(); i++) {
                        result.add(transfers.getJSONObject(i).getString("station_name"));
                    }
                } catch (Exception ignored) {}
                break;
            case "system_types":
                for (String system : new String[]{"metro", "jiyang", "yunba"}) {
                    for (Station station : stations(true)) {
                        if (system.equals(station.systemType)) {
                            result.add(station.name);
                            break;
                        }
                    }
                }
                break;
            case "photo_checkins":
                try (Cursor c = getReadableDatabase().rawQuery(
                        "SELECT DISTINCT station_name FROM checkins "
                                + "WHERE user_id=? AND photo_count>0 ORDER BY checked_in_at",
                        new String[]{USER_ID})) {
                    while (c.moveToNext()) result.add(c.getString(0));
                }
                break;
            default:
                break;
        }
        return result;
    }

    private int count(String sql, String[] args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public Map<String, Integer> achievementMap(List<AchievementProgress> progress) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (AchievementProgress p : progress) map.put(p.id, p.current);
        return map;
    }

    public List<String> newlyUnlocked(Map<String, Integer> before,
                                      List<AchievementProgress> after) {
        List<String> unlocked = new ArrayList<>();
        for (AchievementProgress p : after) {
            int old = before.containsKey(p.id) ? before.get(p.id) : 0;
            if (p.unlocked() && old < p.target) unlocked.add(p.id);
        }
        return unlocked;
    }

    private static final class CheckinDraft {
        String group;
        String station;
        final java.util.LinkedHashSet<String> lines = new java.util.LinkedHashSet<>();
        String color = "#746C7D";
        long time;
        int photoCount;
        String photoUris = "[]";
    }

    private java.util.List<CheckinDraft> loadCheckinDrafts() {
        java.util.Map<String, CheckinDraft> drafts = new LinkedHashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT c.checkin_group,c.station_name,c.line,c.checked_in_at,c.photo_count,c.photo_uris_json,l.color "
                        + "FROM checkins c LEFT JOIN lines l ON c.line=l.line "
                        + "WHERE c.user_id=? ORDER BY c.checked_in_at DESC",
                new String[]{USER_ID})) {
            while (c.moveToNext()) {
                String group = c.getString(0);
                CheckinDraft draft = drafts.get(group);
                if (draft == null) {
                    draft = new CheckinDraft();
                    draft.group = group;
                    draft.station = c.getString(1);
                    draft.time = c.getLong(3);
                    drafts.put(group, draft);
                }
                draft.lines.add(c.getString(2));
                int photos = c.getInt(4);
                if (photos >= draft.photoCount) {
                    draft.photoCount = photos;
                    draft.photoUris = c.getString(5);
                }
                if (!c.isNull(6)) draft.color = c.getString(6);
            }
        }
        return new ArrayList<>(drafts.values());
    }

    public List<CheckinRecord> recentCheckins() {
        List<CheckinRecord> result = new ArrayList<>();
        for (CheckinDraft draft : loadCheckinDrafts()) {
            result.add(new CheckinRecord(draft.station,
                    String.join(" / ", draft.lines), draft.color,
                    draft.time, draft.photoCount));
        }
        return result;
    }

    public List<GalleryItem> galleryItems() {
        List<GalleryItem> result = new ArrayList<>();
        for (CheckinDraft draft : loadCheckinDrafts()) {
            List<String> uris = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(draft.photoUris);
                for (int i = 0; i < array.length(); i++) {
                    String uri = array.optString(i, "");
                    if (!uri.isEmpty()) uris.add(uri);
                }
            } catch (Exception ignored) {}
            String lines = String.join(" / ", draft.lines);
            if (uris.isEmpty()) {
                result.add(new GalleryItem(draft.group, draft.station, lines,
                        draft.color, draft.time, draft.photoCount, null));
            } else {
                for (String uri : uris) {
                    result.add(new GalleryItem(draft.group, draft.station, lines,
                            draft.color, draft.time, draft.photoCount, uri));
                }
            }
        }
        return result;
    }


    public void clearCheckins() {
        getWritableDatabase().delete("checkins", "user_id=?", new String[]{USER_ID});
    }
}








