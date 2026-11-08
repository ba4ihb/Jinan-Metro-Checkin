package com.jnsub.quanchentrail.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jnsub.quanchentrail.R;
import com.jnsub.quanchentrail.data.AchievementProgress;
import com.jnsub.quanchentrail.data.CheckinRecord;
import com.jnsub.quanchentrail.data.LineProgress;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Adapters {
    private Adapters() {}

    public interface OnLineClickListener {
        void onLineClick(String line);
    }

    public interface OnAchievementClickListener {
        void onAchievementClick(AchievementProgress item);
    }

    public static final class LineAdapter extends RecyclerView.Adapter<LineAdapter.Holder> {
        private final List<LineProgress> items;
        private final OnLineClickListener listener;

        public LineAdapter(List<LineProgress> items, OnLineClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_line, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            LineProgress item = items.get(position);
            h.chip.setText(item.line);
            h.name.setText(item.name);
            h.route.setText(item.from + " ↔ " + item.to);
            String schedule = !item.scheduleDisplay.isEmpty()
                    ? item.scheduleDisplay
                    : item.firstTrain.isEmpty() && item.lastTrain.isEmpty()
                    ? "时刻表整理中"
                    : "首班 " + item.firstTrain + "  ·  末班 " + item.lastTrain;
            h.schedule.setText(schedule);
            h.scheduleNote.setText(item.scheduleNote.isEmpty()
                    ? "实际运营时间以车站公告为准"
                    : item.scheduleNote);
            h.status.setText(item.serviceStatus);
            if ("运营中".equals(item.serviceStatus)) {
                h.status.setTextColor(Color.parseColor("#10883A"));
            } else if ("休息中".equals(item.serviceStatus)) {
                h.status.setTextColor(Color.parseColor("#C77700"));
            } else {
                h.status.setTextColor(Color.parseColor("#6C7C84"));
            }
            h.progress.setText(h.itemView.getContext().getString(
                    R.string.progress_format, item.current, item.target));
            h.bar.setProgress(item.percent());
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onLineClick(item.line);
            });
            try {
                int color = Color.parseColor(item.color);
                h.chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                int luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
                h.chip.setTextColor(luminance > 170 ? Color.BLACK : Color.WHITE);
                h.bar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return items.size(); }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView chip, name, route, status, progress, schedule, scheduleNote;
            final ProgressBar bar;
            Holder(View v) {
                super(v);
                chip = v.findViewById(R.id.line_chip);
                name = v.findViewById(R.id.line_name);
                route = v.findViewById(R.id.line_route);
                status = v.findViewById(R.id.line_status);
                progress = v.findViewById(R.id.line_progress);
                schedule = v.findViewById(R.id.line_schedule);
                scheduleNote = v.findViewById(R.id.line_schedule_note);
                bar = v.findViewById(R.id.line_bar);
            }
        }
    }

    public static final class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.Holder> {
        private final List<AchievementProgress> items;
        private final OnAchievementClickListener listener;

        public AchievementAdapter(List<AchievementProgress> items, OnAchievementClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_achievement, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            AchievementProgress item = items.get(position);
            h.name.setText(item.name);
            h.id.setText(item.description);
            h.progress.setText(h.itemView.getContext().getString(
                    R.string.progress_format, item.current, item.target));
            h.state.setText(item.unlocked() ? "已解锁" : item.current == 0 ? "未开始" : "进行中");
            h.bar.setProgress(item.percent());
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAchievementClick(item);
            });
            if (item.unlocked()) {
                h.state.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#10883A")));
                h.state.setTextColor(Color.WHITE);
            } else {
                h.state.setBackgroundTintList(null);
                h.state.setTextColor(h.itemView.getContext().getResources()
                        .getColor(R.color.primary, null));
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView name, id, state, progress;
            final ProgressBar bar;
            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.achievement_name);
                id = v.findViewById(R.id.achievement_id);
                state = v.findViewById(R.id.achievement_state);
                progress = v.findViewById(R.id.achievement_progress);
                bar = v.findViewById(R.id.achievement_bar);
            }
        }
    }

    public static final class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.Holder> {
        private final List<CheckinRecord> items;
        private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

        public RecordAdapter(List<CheckinRecord> items) { this.items = items; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_record, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            CheckinRecord item = items.get(position);
            h.station.setText(item.station);
            h.meta.setText(format.format(new Date(item.checkedInAt))
                    + (item.photoCount > 0 ? "  ·  图片 ×" + item.photoCount : ""));
            h.line.setText(item.line);
            try {
                int color = Color.parseColor(item.color);
                h.line.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                int luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
                h.line.setTextColor(luminance > 170 ? Color.BLACK : Color.WHITE);
            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return items.size(); }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView station, meta, line;
            Holder(View v) {
                super(v);
                station = v.findViewById(R.id.record_station);
                meta = v.findViewById(R.id.record_meta);
                line = v.findViewById(R.id.record_line);
            }
        }
    }
}





