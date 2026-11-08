package com.jnsub.quanchentrail.ui;

import android.content.Context;
import android.os.Build;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.jnsub.quanchentrail.BuildConfig;
import com.jnsub.quanchentrail.MainActivity;
import com.jnsub.quanchentrail.R;
import com.jnsub.quanchentrail.data.AppDb;

public final class AboutFragment extends Fragment {
    public interface Listener {
        void onLocationPermissionNeeded();
        void onNotificationPermissionNeeded();
        void onDeveloperOpen();
    }

    private Listener listener;

    public static AboutFragment newInstance() {
        return new AboutFragment();
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
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView version = view.findViewById(R.id.about_version);
        TextView build = view.findViewById(R.id.about_build);
        TextView dataDate = view.findViewById(R.id.about_data_date);
        TextView permission = view.findViewById(R.id.about_permission_status);
        MaterialSwitch notify = view.findViewById(R.id.switch_notify);
        MaterialSwitch agentJson = view.findViewById(R.id.switch_agent_json);
        Slider radiusSlider = view.findViewById(R.id.radius_slider);
        TextView radiusValue = view.findViewById(R.id.radius_value);
        MaterialButton permissionButton = view.findViewById(R.id.btn_about_permission);
        TextView notificationStatus = view.findViewById(R.id.about_notification_status);
        MaterialButton notificationButton = view.findViewById(R.id.btn_about_notify);
        MaterialButton clear = view.findViewById(R.id.btn_clear_records);
        View developerCard = view.findViewById(R.id.developer_card);
        MaterialButton developer = view.findViewById(R.id.btn_developer);
        if (BuildConfig.DEBUG) {
            developerCard.setVisibility(View.VISIBLE);
            developer.setOnClickListener(v -> {
                if (listener != null) listener.onDeveloperOpen();
            });
        } else {
            developerCard.setVisibility(View.GONE);
        }

        version.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
        build.setText(getString(R.string.build_format, BuildConfig.VERSION_CODE));
        dataDate.setText(getString(R.string.data_date_format, "2026-11-06"));

        SharedPreferences prefs = requireContext().getSharedPreferences(
                MainActivity.SETTINGS_NAME, Context.MODE_PRIVATE);
        notify.setChecked(prefs.getBoolean(MainActivity.SETTING_NOTIFY, true));
        agentJson.setChecked(prefs.getBoolean(MainActivity.SETTING_AGENT_JSON, false));
        int radius = prefs.getInt(MainActivity.SETTING_RADIUS, 100);
        radiusSlider.setValue(radius);
        radiusValue.setText(radius + " m");
        radiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            int selected = Math.round(value);
            radiusValue.setText(selected + " m");
            if (fromUser) prefs.edit().putInt(MainActivity.SETTING_RADIUS, selected).apply();
        });

        notify.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(MainActivity.SETTING_NOTIFY, checked).apply());
        agentJson.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(MainActivity.SETTING_AGENT_JSON, checked).apply());

        boolean locationAllowed = androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
                || androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        permission.setText(locationAllowed ? "定位权限已开启" : "定位权限未开启");
        permissionButton.setText(locationAllowed ? "重新检查定位" : "开启定位权限");
        permissionButton.setOnClickListener(v -> {
            if (listener != null) listener.onLocationPermissionNeeded();
        });
        boolean notificationAllowed = Build.VERSION.SDK_INT < 33
                || androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        notificationStatus.setText(notificationAllowed ? "通知权限已开启" : "通知权限未开启");
        notificationButton.setText(notificationAllowed ? "重新检查通知" : "开启通知权限");
        notificationButton.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationPermissionNeeded();
        });

        clear.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_records)
                .setMessage(R.string.clear_confirm)
                .setPositiveButton(R.string.clear_records, (d, w) -> {
                    new AppDb(requireContext()).clearCheckins();
                    Toast.makeText(requireContext(), R.string.clear_done, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.later, null)
                .show());
    }
}





