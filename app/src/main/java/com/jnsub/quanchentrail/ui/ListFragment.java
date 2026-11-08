package com.jnsub.quanchentrail.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jnsub.quanchentrail.R;
import com.jnsub.quanchentrail.data.AchievementProgress;
import com.jnsub.quanchentrail.data.AppDb;

public final class ListFragment extends Fragment {
    public interface Listener {
        void onLineClicked(String line);
        void onAchievementClicked(AchievementProgress item);
        void onGalleryClicked();
    }

    private Listener listener;
    private View galleryButton;
    private View badgeScroll;
    private android.widget.LinearLayout badgeList;
    public static final int MODE_LINES = 1;
    public static final int MODE_ACHIEVEMENTS = 2;
    public static final int MODE_RECORDS = 3;

    private int mode;
    private RecyclerView list;
    private TextView empty;

    public static ListFragment newInstance(int mode) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putInt("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getArguments() == null ? MODE_LINES : getArguments().getInt("mode", MODE_LINES);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        list = view.findViewById(R.id.list);
        empty = view.findViewById(R.id.empty);
        galleryButton = view.findViewById(R.id.btn_open_gallery);
        badgeScroll = view.findViewById(R.id.achievement_badge_scroll);
        badgeList = view.findViewById(R.id.achievement_badge_list);
        if (galleryButton != null) {
            galleryButton.setVisibility(mode == MODE_RECORDS ? View.VISIBLE : View.GONE);
            galleryButton.setOnClickListener(v -> {
                if (listener != null) listener.onGalleryClicked();
            });
        }
        TextView title = view.findViewById(R.id.list_title);
        TextView subtitle = view.findViewById(R.id.list_subtitle);
        if (mode == MODE_ACHIEVEMENTS) {
            title.setText("成就图鉴");
            subtitle.setText("每一次到站，都在点亮泉城的轨迹");
        } else if (mode == MODE_RECORDS) {
            title.setText("打卡记录");
            subtitle.setText("你走过的站，都会被温柔记住");
        } else {
            title.setText("线路时刻");
            subtitle.setText("首末班车与完成进度，一眼掌握");
        }
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        refresh();
    }

    private void populateBadges(java.util.List<AchievementProgress> achievements) {
        if (badgeScroll == null || badgeList == null) return;
        badgeList.removeAllViews();
        int unlocked = 0;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AchievementProgress item : achievements) {
            if (!item.unlocked()) continue;
            unlocked++;
            View badge = inflater.inflate(R.layout.item_achievement_badge, badgeList, false);
            TextView name = badge.findViewById(R.id.badge_name);
            name.setText(item.name);
            badgeList.addView(badge);
        }
        badgeScroll.setVisibility(unlocked > 0 ? View.VISIBLE : View.GONE);
    }

    public void refresh() {
        if (list == null || !isAdded()) return;
        AppDb db = new AppDb(requireContext());
        int emptyText;
        switch (mode) {
            case MODE_ACHIEVEMENTS: {
                java.util.List<AchievementProgress> achievements = db.achievementProgress();
                java.util.Collections.sort(achievements, (a, b) -> {
                    if (a.unlocked() != b.unlocked()) return a.unlocked() ? -1 : 1;
                    return Integer.compare(b.current, a.current);
                });
                list.setAdapter(new Adapters.AchievementAdapter(achievements, item -> {
                    if (listener != null) listener.onAchievementClicked(item);
                }));
                populateBadges(achievements);
                emptyText = R.string.empty_achievements;
                break;
            }
            case MODE_RECORDS:
                list.setAdapter(new Adapters.RecordAdapter(db.recentCheckins()));
                emptyText = R.string.empty_records;
                break;
            case MODE_LINES:
            default:
                list.setAdapter(new Adapters.LineAdapter(db.lineProgress(), line -> {
                    if (listener != null) listener.onLineClicked(line);
                }));
                emptyText = R.string.empty_lines;
                break;
        }
        boolean isEmpty = list.getAdapter() == null || list.getAdapter().getItemCount() == 0;
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        empty.setText(emptyText);
    }
}





