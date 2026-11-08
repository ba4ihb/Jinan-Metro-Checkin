package com.jnsub.quanchentrail.data;

public final class GalleryItem {
    public final String checkinGroup;
    public final String station;
    public final String lines;
    public final String color;
    public final long checkedInAt;
    public final int photoCount;
    public final String photoUri;

    public GalleryItem(String checkinGroup, String station, String lines, String color,
                       long checkedInAt, int photoCount, String photoUri) {
        this.checkinGroup = checkinGroup;
        this.station = station;
        this.lines = lines;
        this.color = color;
        this.checkedInAt = checkedInAt;
        this.photoCount = photoCount;
        this.photoUri = photoUri;
    }
}
