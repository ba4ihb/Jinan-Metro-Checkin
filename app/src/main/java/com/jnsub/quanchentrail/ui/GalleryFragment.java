package com.jnsub.quanchentrail.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jnsub.quanchentrail.R;
import com.jnsub.quanchentrail.data.AppDb;
import com.jnsub.quanchentrail.data.GalleryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class GalleryFragment extends Fragment {
    public interface Listener {
        void onGalleryBack();
    }

    private Listener listener;
    private RecyclerView list;
    private TextView empty;

    public static GalleryFragment newInstance() {
        return new GalleryFragment();
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
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btn_gallery_back).setOnClickListener(v -> {
            if (listener != null) listener.onGalleryBack();
        });
        list = view.findViewById(R.id.gallery_list);
        empty = view.findViewById(R.id.gallery_empty);
        list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        refresh();
    }

    public void refresh() {
        if (list == null || !isAdded()) return;
        List<GalleryItem> items = new AppDb(requireContext()).galleryItems();
        list.setAdapter(new GalleryAdapter(items));
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private final class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.Holder> {
        private final List<GalleryItem> items;
        private final SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

        GalleryAdapter(List<GalleryItem> items) {
            this.items = items;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gallery, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            GalleryItem item = items.get(position);
            h.station.setText(item.station);
            h.meta.setText(format.format(new Date(item.checkedInAt))
                    + "  ·  " + item.lines
                    + (item.photoCount > 0 ? "  ·  " + item.photoCount + " 张" : ""));
            try {
                h.lineChip.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor(item.color)));
            } catch (Exception ignored) {}
            h.lineChip.setText(item.lines);

            if (item.photoUri == null || item.photoUri.isEmpty()) {
                h.image.setImageDrawable(null);
                h.noImage.setVisibility(View.VISIBLE);
                h.noImage.setText(item.photoCount > 0 ? "图片待加载" : "无图片");
            } else {
                h.noImage.setVisibility(View.GONE);
                try {
                    h.image.setImageURI(Uri.parse(item.photoUri));
                } catch (Exception e) {
                    h.image.setImageDrawable(null);
                    h.noImage.setVisibility(View.VISIBLE);
                    h.noImage.setText("图片无法读取");
                }
            }

            h.itemView.setOnClickListener(v -> {
                if (item.photoUri == null || item.photoUri.isEmpty()) return;
                try {
                    ImageView full = new ImageView(requireContext());
                    full.setImageURI(Uri.parse(item.photoUri));
                    full.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    full.setAdjustViewBounds(true);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(item.station + "  ·  " + format.format(new Date(item.checkedInAt)))
                            .setView(full)
                            .setPositiveButton("关闭", null)
                            .show();
                } catch (Exception ignored) {}
            });
        }

        @Override public int getItemCount() { return items.size(); }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView noImage, station, meta, lineChip;
            Holder(View v) {
                super(v);
                image = v.findViewById(R.id.gallery_image);
                noImage = v.findViewById(R.id.gallery_no_image);
                station = v.findViewById(R.id.gallery_station);
                meta = v.findViewById(R.id.gallery_meta);
                lineChip = v.findViewById(R.id.gallery_line_chip);
            }
        }
    }
}
