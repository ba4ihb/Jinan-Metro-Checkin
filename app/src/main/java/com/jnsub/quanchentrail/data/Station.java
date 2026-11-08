package com.jnsub.quanchentrail.data;

public final class Station {
    public final String stationId;
    public final String name;
    public final java.util.List<String> lines;
    public final double lat;
    public final double lng;
    public final String systemType;
    public final String operationalStatus;

    public Station(String stationId, String name, java.util.List<String> lines,
                   double lat, double lng, String systemType, String operationalStatus) {
        this.stationId = stationId;
        this.name = name;
        this.lines = lines;
        this.lat = lat;
        this.lng = lng;
        this.systemType = systemType;
        this.operationalStatus = operationalStatus;
    }

    public String primaryLine() {
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public boolean isTransfer() {
        return lines.size() > 1;
    }
}
