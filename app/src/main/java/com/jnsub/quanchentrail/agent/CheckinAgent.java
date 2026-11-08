package com.jnsub.quanchentrail.agent;

import android.content.Context;

import com.jnsub.quanchentrail.data.AppDb;
import com.jnsub.quanchentrail.data.AchievementProgress;
import com.jnsub.quanchentrail.data.Poi;
import com.jnsub.quanchentrail.data.Station;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * The local, deterministic implementation of the prompt's structured Agent protocol.
 * Every public response is one of the six JSON actions defined in section 5.
 */
public final class CheckinAgent {
    public static final int CHECKIN_RADIUS_M = 100;
    private final AppDb db;
    private final android.content.SharedPreferences settings;

    public CheckinAgent(Context context) {
        this(context, new AppDb(context));
    }

    public CheckinAgent(Context context, AppDb db) {
        this.db = db;
        this.settings = context.getSharedPreferences("quanchen_settings", Context.MODE_PRIVATE);
    }

    public CheckinAgent(AppDb db) {
        this.db = db;
        this.settings = null;
    }

    private int radiusMeters() {
        if (settings == null) return CHECKIN_RADIUS_M;
        int value = settings.getInt("checkin_radius_m", CHECKIN_RADIUS_M);
        return Math.max(50, Math.min(500, value));
    }

    public static final class Nearest {
        public final Station station;
        public final double distanceMeters;
        public final int roundedDistance;
        public final boolean checked;
        public final String lineLabel;

        Nearest(Station station, double distanceMeters, boolean checked, String lineLabel) {
            this.station = station;
            this.distanceMeters = distanceMeters;
            this.roundedDistance = Geo.roundedMeters(distanceMeters);
            this.checked = checked;
            this.lineLabel = lineLabel;
        }
    }

    public java.util.List<Nearest> nearestStations(double latitude, double longitude, int limit) {
        java.util.List<Nearest> all = new java.util.ArrayList<>();
        for (Station station : db.stations(true)) {
            double distance = Geo.distanceMeters(latitude, longitude, station.lat, station.lng);
            String label = String.join(" / ", db.transferLines(station.name));
            all.add(new Nearest(station, distance, db.isChecked(station.name), label));
        }
        all.sort((a, b) -> Double.compare(a.distanceMeters, b.distanceMeters));
        if (limit > 0 && all.size() > limit) {
            return new java.util.ArrayList<>(all.subList(0, limit));
        }
        return all;
    }

    public JSONObject evaluate(double latitude, double longitude,                               double accuracy, long timestamp) {
        List<Station> stations = db.stations(true);
        if (stations.isEmpty()) {
            return dataMissing("stations");
        }

        Station nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Station station : stations) {
            double distance = Geo.distanceMeters(latitude, longitude, station.lat, station.lng);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = station;
            }
        }
        if (nearest == null) {
            return dataMissing("stations");
        }

        int roundedDistance = Geo.roundedMeters(nearestDistance);
        int radius = radiusMeters();
        if (nearestDistance > radius) {
            return noEligible(nearest.name, roundedDistance);
        }

        String triggerLine = nearest.primaryLine();
        return showPrompt(nearest, triggerLine, roundedDistance);
    }

    public JSONObject confirm(double latitude, double longitude,
                              String stationName, String line, int photoCount) {
        Station station = db.station(stationName);
        if (station == null || !"active".equals(station.operationalStatus)) {
            return dataMissing(stationName == null ? "station" : stationName);
        }
        if (line == null || !station.lines.contains(line)) {
            return dataMissing(line == null ? "line" : line);
        }

        double distance = Geo.distanceMeters(latitude, longitude, station.lat, station.lng);
        if (distance > radiusMeters()) {
            return cancelled(stationName, "out_of_radius");
        }
        Map<String, Integer> before = db.achievementMap(db.achievementProgress());
        AppDb.CheckinWrite write = db.writeCheckin(stationName, line, photoCount,
                System.currentTimeMillis());
        List<String> affected = write.affectedLines;
        List<AchievementProgress> after = db.achievementProgress();
        List<String> newlyUnlocked = db.newlyUnlocked(before, after);

        try {
            JSONObject response = new JSONObject();
            response.put("action", "CHECKIN_CONFIRMED");
            response.put("station", stationName);
            response.put("line", line);
            response.put("affected_lines", new JSONArray(affected));
            response.put("checkin_group", write.group);
            response.put("newly_unlocked", new JSONArray(newlyUnlocked));
            response.put("achievements", achievementJson(after));
            response.put("photo_count", Math.max(0, Math.min(3, photoCount)));
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode CHECKIN_CONFIRMED", e);
        }
    }

    public JSONObject cancel(String stationName, String reason) {
        String safeReason = "out_of_radius".equals(reason) ? "out_of_radius" : "user_cancelled";
        return cancelled(stationName, safeReason);
    }

    public JSONObject dataMissing(String query) {
        try {
            JSONObject response = new JSONObject();
            response.put("action", "DATA_MISSING");
            response.put("query", query == null ? "unknown" : query);
            response.put("detail", "该站点不在当前运营数据中 / 该换乘关系不存在");
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode DATA_MISSING", e);
        }
    }

    private JSONObject noEligible(String stationName, int distance) {
        try {
            JSONObject response = new JSONObject();
            response.put("action", "NO_ELIGIBLE_STATION");
            response.put("nearest_station", stationName);
            response.put("distance_m", distance);
            response.put("message", "附近无可打卡站点（最近：" + stationName + "，距离 " + distance + " m）");
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode NO_ELIGIBLE_STATION", e);
        }
    }

    private JSONObject alreadyChecked(String stationName, String line, boolean transfer) {
        try {
            JSONObject response = new JSONObject();
            response.put("action", "ALREADY_CHECKED_IN");
            response.put("station", stationName);
            response.put("line", line);
            response.put("transfer_note", transfer
                    ? "该站已在关联线路中处于打卡状态" : JSONObject.NULL);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode ALREADY_CHECKED_IN", e);
        }
    }

    private JSONObject showPrompt(Station station, String line, int distance) {
        try {
            List<Poi> pois = db.pois(station.name);
            JSONArray poiArray = new JSONArray();
            List<String> poiNames = new java.util.ArrayList<>();
            for (Poi poi : pois) {
                poiArray.put(poi.poiName);
                poiNames.add(poi.poiName);
            }
            String poiText = poiNames.isEmpty() ? "站外的泉城街景" : String.join("、", poiNames);
            List<String> transferLines = db.isTransfer(station.name)
                    ? db.transferLines(station.name) : db.transferLines(station.name);
            String lineList = displayLineList(transferLines);
            String transferNote = db.isTransfer(station.name)
                    ? "本站在 " + lineList + " 之间换乘，打卡后将同步更新所有关联线路状态"
                    : null;

            String warning = "你已进入 **" + station.name + "** 的打卡范围。"
                    + "地铁经过时匆忙打卡可能会错过这个站真正的样子——"
                    + "如果时间允许，不妨出站看看 " + poiText + "，探索后再回来打卡。"
                    + "仍然现在打卡吗？";
            if (db.isTransfer(station.name)) {
                warning += "\n\n这是 **" + lineList + "** 的换乘站。"
                        + "在此打卡后，两边的打卡状态将同步更新。";
            }

            JSONObject response = new JSONObject();
            response.put("action", "SHOW_CHECKIN_PROMPT");
            response.put("station", station.name);
            response.put("line", line);
            response.put("line_color", db.lineColor(line));
            response.put("distance_m", distance);
            response.put("nearby_pois", poiArray);
            response.put("transfer_note", transferNote == null ? JSONObject.NULL : transferNote);
            response.put("soft_warning", warning);
            response.put("checkin_radius_m", radiusMeters());
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode SHOW_CHECKIN_PROMPT", e);
        }
    }

    private JSONObject cancelled(String stationName, String reason) {
        try {
            JSONObject response = new JSONObject();
            response.put("action", "CHECKIN_CANCELLED");
            response.put("station", stationName == null ? "" : stationName);
            response.put("reason", reason);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode CHECKIN_CANCELLED", e);
        }
    }

    private JSONArray achievementJson(List<AchievementProgress> progress) {
        JSONArray array = new JSONArray();
        try {
            for (AchievementProgress p : progress) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                o.put("current", p.current);
                o.put("target", p.target);
                array.put(o);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode achievements", e);
        }
        return array;
    }

    private String displayLineList(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append("、");
            String line = lines.get(i);
            sb.append(line.matches("\\d+") ? line + "号线" : line);
        }
        return sb.toString();
    }
}



