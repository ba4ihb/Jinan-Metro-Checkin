package com.jnsub.quanchentrail.data;

public final class AchievementProgress {
    public final String id;
    public final String name;
    public final String conditionType;
    public final String description;
    public final int current;
    public final int target;

    public AchievementProgress(String id, String name, String conditionType,
                               String description, int current, int target) {
        this.id = id;
        this.name = name;
        this.conditionType = conditionType;
        this.description = description == null ? "" : description;
        this.current = current;
        this.target = target;
    }

    public boolean unlocked() {
        return target > 0 && current >= target;
    }

    public int percent() {
        return target <= 0 ? 0 : Math.min(100, Math.round(current * 100f / target));
    }
}
