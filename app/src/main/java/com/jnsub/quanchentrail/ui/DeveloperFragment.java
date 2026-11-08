package com.jnsub.quanchentrail.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jnsub.quanchentrail.R;

public final class DeveloperFragment extends Fragment {
    public interface Listener {
        void onDeveloperBack();
        void onSendTestNotification();
        void onSimulateLocation(double lat, double lng, float accuracy);
        void onSimulateCheckin(String station, String line);
    }

    private Listener listener;

    public static DeveloperFragment newInstance() {
        return new DeveloperFragment();
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
        return inflater.inflate(R.layout.fragment_developer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btn_dev_back).setOnClickListener(v -> {
            if (listener != null) listener.onDeveloperBack();
        });
        view.findViewById(R.id.btn_test_notification).setOnClickListener(v -> {
            if (listener != null) listener.onSendTestNotification();
        });

        EditText lat = view.findViewById(R.id.dev_lat);
        EditText lng = view.findViewById(R.id.dev_lng);
        EditText accuracy = view.findViewById(R.id.dev_accuracy);
        view.findViewById(R.id.btn_simulate_location).setOnClickListener(v -> {
            try {
                if (listener != null) {
                    listener.onSimulateLocation(
                            Double.parseDouble(lat.getText().toString().trim()),
                            Double.parseDouble(lng.getText().toString().trim()),
                            Float.parseFloat(accuracy.getText().toString().trim()));
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "请输入有效的经纬度和精度", Toast.LENGTH_SHORT).show();
            }
        });

        AutoCompleteTextView station = view.findViewById(R.id.dev_station);
        java.util.List<com.jnsub.quanchentrail.data.Station> stations =
                new com.jnsub.quanchentrail.data.AppDb(requireContext()).stations(true);
        java.util.List<String> stationNames = new java.util.ArrayList<>();
        for (com.jnsub.quanchentrail.data.Station item : stations) stationNames.add(item.name);
        station.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, stationNames));
        station.setThreshold(1);
        EditText line = view.findViewById(R.id.dev_line);
        view.findViewById(R.id.btn_simulate_checkin).setOnClickListener(v -> {
            String stationName = station.getText().toString().trim();
            String lineName = line.getText().toString().trim();
            if (stationName.isEmpty()) {
                Toast.makeText(getContext(), "请输入车站名称", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onSimulateCheckin(stationName, lineName);
        });
    }
}

