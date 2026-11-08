package com.jnsub.quanchentrail.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.jnsub.quanchentrail.R;
import com.jnsub.quanchentrail.agent.CheckinAgent;
import com.jnsub.quanchentrail.data.AppDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class HomeFragment extends Fragment {
    public interface Listener {
        void onLocationPermissionNeeded();
        void onRefreshLocation();
        void onConfirmCheckin(boolean withPhotos);
        void onConfirmStation(String station, String line, boolean withPhotos);
        void onLater();
    }

    private Listener listener;
    private TextView statusTitle;
    private TextView statusBody;
    private View permissionButton;
    private android.widget.Button checkinButton;
    private android.widget.LinearLayout nearestList;
    private android.widget.ScrollView homeScroll;
    private MaterialCardView promptCard;
    private android.widget.LinearLayout promptExtra;
    private TextView promptLineChip;
    private TextView promptStation;
    private TextView promptDistance;
    private TextView promptPois;
    private TextView promptTransfer;
    private TextView promptWarning;
    private TextView agentJson;
    private View agentCard;

    public static HomeFragment newInstance() {
        return new HomeFragment();
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
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        statusTitle = view.findViewById(R.id.status_title);
        statusBody = view.findViewById(R.id.status_body);
        permissionButton = view.findViewById(R.id.btn_permission);
        checkinButton = view.findViewById(R.id.btn_checkin_home);
        nearestList = view.findViewById(R.id.nearest_list);
        homeScroll = view.findViewById(R.id.home_scroll);
        promptCard = view.findViewById(R.id.prompt_card);
        promptExtra = view.findViewById(R.id.prompt_extra_container);
        promptLineChip = view.findViewById(R.id.prompt_line_chip);
        promptStation = view.findViewById(R.id.prompt_station);
        promptDistance = view.findViewById(R.id.prompt_distance);
        promptPois = view.findViewById(R.id.prompt_pois);
        promptTransfer = view.findViewById(R.id.prompt_transfer);
        promptWarning = view.findViewById(R.id.prompt_warning);
        agentJson = view.findViewById(R.id.agent_json);
        agentCard = view.findViewById(R.id.agent_card);
        boolean showAgentJson = requireContext().getSharedPreferences(
                "quanchen_settings", Context.MODE_PRIVATE).getBoolean("show_agent_json", false);
        agentCard.setVisibility(showAgentJson ? View.VISIBLE : View.GONE);

        permissionButton.setOnClickListener(v -> {
            if (listener != null) listener.onLocationPermissionNeeded();
        });
        view.findViewById(R.id.btn_later).setOnClickListener(v -> {
            if (listener != null) listener.onLater();
        });
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            if (listener != null) listener.onConfirmCheckin(false);
        });
        view.findViewById(R.id.btn_photo).setOnClickListener(v -> {
            if (listener != null) listener.onConfirmCheckin(true);
        });
        view.findViewById(R.id.status_card).setOnClickListener(v -> {
            if (listener != null) listener.onRefreshLocation();
        });

    }

    public void setLocationStatus(String title, String body, boolean showPermissionButton) {
        if (!isAdded() || getView() == null || statusTitle == null) return;
        statusTitle.setText(title);
        statusBody.setText(body);
        permissionButton.setVisibility(showPermissionButton ? View.VISIBLE : View.GONE);
    }

    public void updateNearestList(java.util.List<CheckinAgent.Nearest> stations) {
        if (!isAdded() || getView() == null || nearestList == null) return;
        nearestList.removeAllViews();
        if (stations == null || stations.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("等待定位，或将显示最近 3 个车站");
            empty.setTextColor(getResources().getColor(R.color.muted, null));
            empty.setTextSize(13);
            empty.setPadding(0, 8, 0, 8);
            nearestList.addView(empty);
            return;
        }
        int radius = getContext().getSharedPreferences("quanchen_settings", Context.MODE_PRIVATE)
                .getInt("checkin_radius_m", 100);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < stations.size() && i < 3; i++) {
            CheckinAgent.Nearest item = stations.get(i);
            View row = inflater.inflate(R.layout.item_nearest, nearestList, false);
            TextView rank = row.findViewById(R.id.nearest_rank);
            TextView name = row.findViewById(R.id.nearest_name);
            TextView lines = row.findViewById(R.id.nearest_lines);
            TextView distance = row.findViewById(R.id.nearest_distance);
            TextView state = row.findViewById(R.id.nearest_state);
            TextView poi = row.findViewById(R.id.nearest_poi);
            rank.setText(String.valueOf(i + 1));
            name.setText(item.station.name);
            lines.setText(item.lineLabel);
            java.util.List<com.jnsub.quanchentrail.data.Poi> poiList =
                    new AppDb(getContext()).pois(item.station.name);
            poi.setText(poiList.isEmpty()
                    ? "周边地标：—"
                    : "周边：" + poiList.get(0).poiName);
            distance.setText(getString(R.string.distance_format, item.roundedDistance));
            if (item.checked) {
                state.setText("已打卡");
                state.setTextColor(getResources().getColor(R.color.success, null));
            } else if (item.distanceMeters <= radius) {
                state.setText("可打卡");
                state.setTextColor(getResources().getColor(R.color.primary, null));
            } else {
                state.setText("附近");
                state.setTextColor(getResources().getColor(R.color.muted, null));
            }
            nearestList.addView(row);
        }
    }

    public void clearNearest() {
        if (nearestList != null) nearestList.removeAllViews();
    }

    public void showAction(JSONObject action) {
        Log.d("QuanchenTrail", "showAction prompt=" + promptCard + " view=" + getView() + " action=" + action);
        if (!isAdded() || getView() == null || promptCard == null || action == null) return;
        String type = action.optString("action", "");
        try {
            agentJson.setText(action.toString(2));
        } catch (Exception ignored) {
            agentJson.setText(action.toString());
        }
        if (agentCard != null && getContext() != null) {
            boolean showAgentJson = getContext().getSharedPreferences(
                    "quanchen_settings", Context.MODE_PRIVATE).getBoolean("show_agent_json", false);
            agentCard.setVisibility(showAgentJson ? View.VISIBLE : View.GONE);
        }

        if (checkinButton != null) {
            checkinButton.setText("SHOW_CHECKIN_PROMPT".equals(type)
                    ? "请在下方卡片完成打卡"
                    : "打卡区域 · 进入范围后使用下方卡片打卡");
        }
        if (!"SHOW_CHECKIN_PROMPT".equals(type)) {
            promptCard.setVisibility(View.GONE);
            showExtraPrompts(null);
            return;
        }

        String station = action.optString("station", "—");
        String line = action.optString("line", "");
        int distance = action.optInt("distance_m", 0);
        if (getContext() == null) return;
        AppDb db = new AppDb(getContext());
        String lineName = db.lineName(line);
        String color = db.lineColor(line);

        promptCard.setVisibility(View.VISIBLE);
        promptLineChip.setText(lineName);
        tintChip(promptLineChip, color);
        promptStation.setText(station);
        promptDistance.setText(getString(R.string.distance_format, distance));

        JSONArray pois = action.optJSONArray("nearby_pois");
        List<String> poiNames = new ArrayList<>();
        if (pois != null) {
            for (int i = 0; i < pois.length(); i++) poiNames.add(pois.optString(i));
        }
        promptPois.setText(poiNames.isEmpty()
                ? "周边看点：站外的泉城街景"
                : "周边看点：" + String.join("、", poiNames));

        String transfer = action.optString("transfer_note", "");
        if (transfer == null || transfer.isEmpty() || "null".equals(transfer)) {
            promptTransfer.setVisibility(View.GONE);
        } else {
            promptTransfer.setVisibility(View.VISIBLE);
            promptTransfer.setText(transfer);
        }
        promptWarning.setText(stripMarkdown(action.optString("soft_warning", "")));
    }

    public void scrollToPrompt() {
        if (homeScroll == null || promptCard == null) return;
        homeScroll.post(() -> homeScroll.smoothScrollTo(0, Math.max(0, promptCard.getTop() - 80)));
    }

    public void hidePrompt() {
        if (isAdded() && getView() != null && promptCard != null) {
            promptCard.setVisibility(View.GONE);
        }
        showExtraPrompts(null);
    }

    public void showExtraPrompts(java.util.List<JSONObject> extras) {
        if (!isAdded() || getView() == null || promptExtra == null) return;
        promptExtra.removeAllViews();
        if (extras == null || extras.isEmpty() || getContext() == null) return;
        AppDb db = new AppDb(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (JSONObject action : extras) {
            View card = inflater.inflate(R.layout.item_prompt_card, promptExtra, false);
            TextView chip = card.findViewById(R.id.extra_line_chip);
            TextView stationView = card.findViewById(R.id.extra_station);
            TextView distance = card.findViewById(R.id.extra_distance);
            TextView pois = card.findViewById(R.id.extra_pois);
            TextView transfer = card.findViewById(R.id.extra_transfer);
            TextView warning = card.findViewById(R.id.extra_warning);
            String line = action.optString("line", "");
            String stationName = action.optString("station", "—");
            chip.setText(db.lineName(line));
            tintChip(chip, db.lineColor(line));
            stationView.setText(stationName);
            distance.setText(getString(R.string.distance_format, action.optInt("distance_m", 0)));

            java.util.List<String> poiNames = new java.util.ArrayList<>();
            for (com.jnsub.quanchentrail.data.Poi p : db.pois(stationName)) poiNames.add(p.poiName);
            pois.setText(poiNames.isEmpty()
                    ? "周边看点：站外的泉城街景"
                    : "周边看点：" + String.join("、", poiNames));

            if (db.isTransfer(stationName)) {
                StringBuilder sb = new StringBuilder();
                java.util.List<String> tLines = db.transferLines(stationName);
                for (int i = 0; i < tLines.size(); i++) {
                    if (i > 0) sb.append("、");
                    String l = tLines.get(i);
                    sb.append(l.matches("\\d+") ? l + "号线" : l);
                }
                transfer.setVisibility(View.VISIBLE);
                transfer.setText("本站在 " + sb + " 之间换乘，打卡后将同步更新所有关联线路状态");
            } else {
                transfer.setVisibility(View.GONE);
            }

            warning.setText(stripMarkdown("你已进入 **" + stationName + "** 的打卡范围。"
                    + "地铁经过时匆忙打卡可能会错过这个站真正的样子——"
                    + "如果时间允许，不妨出站看看 " + (poiNames.isEmpty() ? "站外的泉城街景" : String.join("、", poiNames))
                    + "，探索后再回来打卡。仍然现在打卡吗？"));

            card.findViewById(R.id.extra_btn_later).setOnClickListener(v -> promptExtra.removeView(card));
            card.findViewById(R.id.extra_btn_confirm).setOnClickListener(v -> {
                if (listener != null) listener.onConfirmStation(stationName, line, false);
            });
            card.findViewById(R.id.extra_btn_photo).setOnClickListener(v -> {
                if (listener != null) listener.onConfirmStation(stationName, line, true);
            });
            promptExtra.addView(card);
        }
    }

    private void tintChip(TextView chip, String color) {
        try {
            int parsed = Color.parseColor(color);
            chip.setBackgroundTintList(ColorStateList.valueOf(parsed));
            int luminance = (Color.red(parsed) * 299 + Color.green(parsed) * 587 + Color.blue(parsed) * 114) / 1000;
            chip.setTextColor(luminance > 170 ? Color.BLACK : Color.WHITE);
        } catch (Exception ignored) {
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6A4C93")));
            chip.setTextColor(Color.WHITE);
        }
    }

    private String stripMarkdown(String text) {
        return text == null ? "" : text.replace("**", "");
    }
}








