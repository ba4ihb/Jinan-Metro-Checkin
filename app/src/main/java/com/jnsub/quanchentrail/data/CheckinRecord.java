package com.jnsub.quanchentrail.data;

public final class CheckinRecord {
    public final String station;
    public final String line;
    public final String color;
    public final long checkedInAt;
    public final int photoCount;

    public CheckinRecord(String station, String line, String color, long checkedInAt, int photoCount) {
        this.station = station;
        this.line = line;
        this.color = color;
        this.checkedInAt = checkedInAt;
        this.photoCount = photoCount;
    }
}
