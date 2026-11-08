package com.jnsub.quanchentrail.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jnsub.quanchentrail.R;
import com.jnsub.quanchentrail.data.AppDb;
import com.jnsub.quanchentrail.data.LineProgress;
import com.jnsub.quanchentrail.data.Station;

import java.util.List;

public final class LineDetailFragment extends Fragment {
    public interface Listener {
        void onLineDetailBack();
    }

    private Listener listener;
    private String line;

    public static LineDetailFragment newInstance(String line) {
        LineDetailFragment fragment = new LineDetailFragment();
        Bundle args = new Bundle();
        args.putString("line", line);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_line_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        line = getArguments() == null ? "1" : getArguments().getString("line", "1");
        AppDb db = new AppDb(requireContext());

        view.findViewById(R.id.btn_line_back).setOnClickListener(v -> {
            if (listener != null) listener.onLineDetailBack();
        });

        LineProgress progress = null;
        for (LineProgress item : db.lineProgress()) {
            if (item.line.equals(line)) {
                progress = item;
                break;
            }
        }
        if (progress == null) return;

        TextView chip = view.findViewById(R.id.detail_line_chip);
        TextView name = view.findViewById(R.id.detail_line_name);
        TextView status = view.findViewById(R.id.detail_line_status);
        TextView route = view.findViewById(R.id.detail_line_route);
        TextView schedule = view.findViewById(R.id.detail_line_schedule);
        TextView scheduleNote = view.findViewById(R.id.detail_line_schedule_note);
        TextView progressText = view.findViewById(R.id.detail_progress_text);
        ProgressBar bar = view.findViewById(R.id.detail_progress_bar);
        TextView stationSummary = view.findViewById(R.id.detail_station_summary);

        chip.setText(progress.line);
        try {
            int color = Color.parseColor(progress.color);
            chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            int luminance = (Color.red(color) * 299 + Color.green(color) * 587
                    + Color.blue(color) * 114) / 1000;
            chip.setTextColor(luminance > 170 ? Color.BLACK : Color.WHITE);
        } catch (Exception ignored) {}
        name.setText(progress.name);
        status.setText(progress.serviceStatus);
        status.setTextColor(Color.parseColor(
                "运营中".equals(progress.serviceStatus) ? "#10883A"
                        : "休息中".equals(progress.serviceStatus) ? "#C77700" : "#6C7C84"));
        route.setText(progress.from + " ↔ " + progress.to);
        schedule.setText(!progress.scheduleDisplay.isEmpty()
                ? progress.scheduleDisplay
                : "首班 " + progress.firstTrain + "  ·  末班 " + progress.lastTrain);
        scheduleNote.setText(progress.scheduleNote);
        progressText.setText(getString(R.string.progress_format, progress.current, progress.target));
        bar.setProgress(progress.percent());

        List<Station> stations = db.lineStations(line);
        int checkedCount = 0;
        for (Station station : stations) if (db.isChecked(station.name)) checkedCount++;
        stationSummary.setText("已打卡 " + checkedCount + " / " + stations.size() + " 站");

        RecyclerView list = view.findViewById(R.id.detail_station_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new StationStatusAdapter(stations, db));
    }

    private static final class StationStatusAdapter
            extends RecyclerView.Adapter<StationStatusAdapter.Holder> {
        private final List<Station> stations;
        private final AppDb db;

        StationStatusAdapter(List<Station> stations, AppDb db) {
            this.stations = stations;
            this.db = db;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_station, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Station station = stations.get(position);
            boolean checked = db.isChecked(station.name);
            h.name.setText(station.name);
            h.lines.setText(String.join(" / ", station.lines));
            h.state.setText(checked ? "已打卡" : "未打卡");
            h.state.setTextColor(Color.parseColor(checked ? "#10883A" : "#6C7C84"));
            h.dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor(checked ? "#10883A" : "#DCE7EB")));
        }

        @Override public int getItemCount() { return stations.size(); }

        static final class Holder extends RecyclerView.ViewHolder {
            final View dot;
            final TextView name, lines, state;
            Holder(View v) {
                super(v);
                dot = v.findViewById(R.id.station_dot);
                name = v.findViewById(R.id.station_name);
                lines = v.findViewById(R.id.station_lines);
                state = v.findViewById(R.id.station_state);
            }
        }
    }
}

