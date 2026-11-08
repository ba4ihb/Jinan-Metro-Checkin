package com.jnsub.quanchentrail.data;

public final class LineProgress {
    public final String line;
    public final String name;
    public final String systemType;
    public final String color;
    public final String from;
    public final String to;
    public final int current;
    public final int target;
    public final String status;
    public final String firstTrain;
    public final String lastTrain;
    public final String scheduleDisplay;
    public final String scheduleNote;
    public final String serviceStatus;

    public LineProgress(String line, String name, String systemType, String color,
                        String from, String to, int current, int target, String status,
                        String firstTrain, String lastTrain, String scheduleDisplay,
                        String scheduleNote, String serviceStatus) {
        this.line = line;
        this.name = name;
        this.systemType = systemType;
        this.color = color;
        this.from = from;
        this.to = to;
        this.current = current;
        this.target = target;
        this.status = status;
        this.firstTrain = firstTrain == null ? "" : firstTrain;
        this.lastTrain = lastTrain == null ? "" : lastTrain;
        this.scheduleDisplay = scheduleDisplay == null ? "" : scheduleDisplay;
        this.scheduleNote = scheduleNote == null ? "" : scheduleNote;
        this.serviceStatus = serviceStatus == null ? "" : serviceStatus;
    }

    public int percent() {
        return target <= 0 ? 0 : Math.min(100, Math.round(current * 100f / target));
    }
}
