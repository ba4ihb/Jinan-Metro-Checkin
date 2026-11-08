package com.jnsub.quanchentrail;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.MediaStore;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jnsub.quanchentrail.agent.CheckinAgent;
import com.jnsub.quanchentrail.data.AppDb;
import com.jnsub.quanchentrail.ui.AboutFragment;
import com.jnsub.quanchentrail.data.AchievementProgress;
import com.jnsub.quanchentrail.ui.HomeFragment;
import com.jnsub.quanchentrail.ui.DeveloperFragment;
import com.jnsub.quanchentrail.ui.LineDetailFragment;
import com.jnsub.quanchentrail.ui.GalleryFragment;
import com.jnsub.quanchentrail.ui.ListFragment;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements HomeFragment.Listener, AboutFragment.Listener,
        ListFragment.Listener, LineDetailFragment.Listener, DeveloperFragment.Listener,
        GalleryFragment.Listener {
    public static final String SETTINGS_NAME = "quanchen_settings";
    public static final String SETTING_NOTIFY = "notify_prompts";
    public static final String SETTING_AGENT_JSON = "show_agent_json";
    public static final String SETTING_RADIUS = "checkin_radius_m";
    private static final String CHANNEL_ID = "checkin_alerts";
    private static final int NOTIFICATION_ID_BASE = 4100;

    private AppDb db;
    private CheckinAgent agent;
    private LocationManager locationManager;
    private Location lastLocation;
    private Location lastRealLocation;
    private boolean developerLocationLock;
    private JSONObject lastAction;
    private JSONObject pendingAction;
    private String lastNotifiedStation;
    private HomeFragment homeFragment;
    private ListFragment listFragment;
    private int currentMode = -1;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionResult);

    private final ActivityResultLauncher<PickVisualMediaRequest> photoLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(3),
                    this::onPhotosPicked);

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    this::onCameraResult);
    private Uri pendingCameraUri;
    private List<Uri> pendingPhotoUris = new ArrayList<>();

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            lastRealLocation = location;
            if (developerLocationLock) return;
            handleLocation(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            updateLocationStatus("定位已启用", "正在等待下一次定位更新…", false);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            updateLocationStatus(getString(R.string.location_disabled),
                    "请打开系统定位服务后返回本页", true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initializeApp(savedInstanceState);
        } catch (Throwable error) {
            CrashLog.write(error);
            showFallback(error);
        }
    }

    private void initializeApp(Bundle savedInstanceState) {
        db = new AppDb(this);
        db.openOrRecover();
        agent = new CheckinAgent(this, db);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_dark));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.surface));
            getWindow().getDecorView().setSystemUiVisibility(
                    0);
        }
        createNotificationChannel();
        setupNavigation();
        handleIntent(getIntent());
        ensurePermissions();
    }

    private void showFallback(Throwable error) {
        try {
            setContentView(R.layout.activity_fallback);
            TextView message = findViewById(R.id.fallback_message);
            if (message != null) {
                message.setText(getString(R.string.startup_error_desc)
                        + "\n\n" + error.getClass().getSimpleName());
            }
            View retry = findViewById(R.id.btn_retry);
            if (retry != null) retry.setOnClickListener(v -> recreate());
            View exit = findViewById(R.id.btn_exit);
            if (exit != null) exit.setOnClickListener(v -> finish());
        } catch (Throwable fallbackError) {
            CrashLog.write(fallbackError);
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        if (locationManager != null) {
            try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showHome();
            } else if (id == R.id.nav_lines) {
                showList(ListFragment.MODE_LINES);
            } else if (id == R.id.nav_achievements) {
                showList(ListFragment.MODE_ACHIEVEMENTS);
            } else if (id == R.id.nav_records) {
                showList(ListFragment.MODE_RECORDS);
            } else if (id == R.id.nav_about) {
                showAbout();
            }
            return true;
        });
        nav.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) showHome();
            else if (item.getItemId() == R.id.nav_lines) showList(ListFragment.MODE_LINES);
            else if (item.getItemId() == R.id.nav_achievements) showList(ListFragment.MODE_ACHIEVEMENTS);
            else if (item.getItemId() == R.id.nav_records) showList(ListFragment.MODE_RECORDS);
            else if (item.getItemId() == R.id.nav_about) showAbout();
        });
        showHome();
    }

    private void showLineDetail(String line) {
        currentMode = -3;
        listFragment = null;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, LineDetailFragment.newInstance(line), "line_detail")
                .commitAllowingStateLoss();
    }

    @Override
    public void onLineDetailBack() {
        showList(ListFragment.MODE_LINES);
    }

    @Override
    public void onLineClicked(String line) {
        showLineDetail(line);
    }

    @Override
    public void onAchievementClicked(AchievementProgress item) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_achievement, null, false);
        TextView title = view.findViewById(R.id.dialog_achievement_title);
        TextView desc = view.findViewById(R.id.dialog_achievement_desc);
        TextView progress = view.findViewById(R.id.dialog_achievement_progress);
        TextView stations = view.findViewById(R.id.dialog_achievement_stations);
        android.widget.ProgressBar bar = view.findViewById(R.id.dialog_achievement_bar);
        title.setText(item.name);
        desc.setText(item.description);
        progress.setText(getString(R.string.progress_format, item.current, item.target)
                + "  ·  " + item.percent() + "%");
        bar.setProgress(item.percent());
        java.util.List<String> stationNames = new AppDb(this).achievementStations(item.id);
        stations.setText(stationNames.isEmpty()
                ? "该成就按条件动态判定，暂无固定车站列表"
                : "组成车站：" + "\n" + String.join("、", stationNames));
        new MaterialAlertDialogBuilder(this)
                .setTitle("成就进度")
                .setView(view)
                .setPositiveButton("知道了", null)
                .show();
    }

    @Override
    public void onDeveloperOpen() {
        if (!BuildConfig.DEBUG) return;
        currentMode = -4;
        listFragment = null;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, DeveloperFragment.newInstance(), "developer")
                .commitAllowingStateLoss();
    }

    @Override
    public void onDeveloperBack() {
        showAbout();
    }

    @Override
    public void onSendTestNotification() {
        try {
            JSONObject action = new JSONObject();
            action.put("action", "SHOW_CHECKIN_PROMPT");
            action.put("station", "千佛山");
            action.put("line", "4");
            action.put("line_color", db.lineColor("4"));
            action.put("distance_m", 87);
            action.put("nearby_pois", new org.json.JSONArray().put("千佛山风景区（A口步行约300m）"));
            action.put("transfer_note", org.json.JSONObject.NULL);
            action.put("soft_warning", "这是开发者测试通知，用于检查状态栏展示与点击跳转。");
            action.put("checkin_radius_m", getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE)
                    .getInt(SETTING_RADIUS, 100));
            notifyPrompt(action);
            Toast.makeText(this, "测试通知已发送", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            CrashLog.write(e);
            Toast.makeText(this, "测试通知失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSimulateLocation(double lat, double lng, float accuracy) {
        Log.d("QuanchenTrail", "simulate location " + lat + "," + lng + "," + accuracy);
        Location location = new Location("developer");
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setAccuracy(accuracy);
        location.setTime(System.currentTimeMillis());
        developerLocationLock = true;
        if (locationManager != null) {
            try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) {}
        }
        handleLocation(location);
        Log.d("QuanchenTrail", "simulate action=" + lastAction);
        showHome();
        updateLocationStatus("模拟定位中", "点击这里恢复真实定位", false);
        Toast.makeText(this, "已应用模拟位置并返回首页", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSimulateCheckin(String stationName, String lineName) {
        com.jnsub.quanchentrail.data.Station station = db.station(stationName);
        if (station == null) {
            Toast.makeText(this, "车站不存在：" + stationName, Toast.LENGTH_SHORT).show();
            return;
        }
        String line = (lineName == null || lineName.isEmpty()) ? station.primaryLine() : lineName;
        if (!station.lines.contains(line)) line = station.primaryLine();
        JSONObject result = agent.confirm(station.lat, station.lng, station.name, line, 0);
        lastAction = result;
        pendingAction = null;
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
            homeFragment.showAction(result);
            homeFragment.hidePrompt();
        }
        refreshCurrentList();
        String action = result.optString("action", "");
        Toast.makeText(this, "CHECKIN_CONFIRMED".equals(action)
                ? "模拟打卡成功" : action, Toast.LENGTH_SHORT).show();
    }

    private void showGallery() {
        currentMode = -5;
        listFragment = null;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, GalleryFragment.newInstance(), "gallery")
                .commitAllowingStateLoss();
    }

    @Override
    public void onGalleryClicked() {
        showGallery();
    }

    @Override
    public void onGalleryBack() {
        showList(ListFragment.MODE_RECORDS);
    }

    private void showAbout() {
        currentMode = -2;
        listFragment = null;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, AboutFragment.newInstance(), "about")
                .commitAllowingStateLoss();
    }

    private void showHome() {
        currentMode = -1;
        if (homeFragment == null) homeFragment = HomeFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, homeFragment, "home")
                .commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
        applyHomeStateDeferred();
    }


    private void applyHomeStateDeferred() {
        View container = findViewById(R.id.content_container);
        if (container == null) return;
        container.postDelayed(() -> {
        Log.d("QuanchenTrail", "deferred start last=" + lastAction);
            if (homeFragment == null || !homeFragment.isAdded() || homeFragment.getView() == null) return;
            if (lastAction != null) homeFragment.showAction(lastAction);
            Log.d("QuanchenTrail", "deferred fragment=" + homeFragment.isAdded() + " view=" + (homeFragment.getView() != null));
            if (lastLocation != null && agent != null) {
                homeFragment.updateNearestList(agent.nearestStations(
                        lastLocation.getLatitude(), lastLocation.getLongitude(), 3));
                String primaryStation = lastAction == null
                        ? "" : lastAction.optString("station", "");
                homeFragment.showExtraPrompts(collectExtraPrompts(lastLocation, primaryStation));
            }
        }, 150);
    }
    private void showList(int mode) {
        currentMode = mode;
        listFragment = ListFragment.newInstance(mode);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, listFragment, "list_" + mode)
                .commitAllowingStateLoss();
        listFragment.refresh();
    }

    private void refreshCurrentList() {
        if (listFragment != null && currentMode != -1) listFragment.refresh();
    }

    private void ensurePermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!needed.isEmpty()) {
            permissionLauncher.launch(needed.toArray(new String[0]));
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onNotificationPermissionNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
        } else {
            Toast.makeText(this, "当前系统版本无需单独的通知权限", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationPermissionNeeded() {
        ensurePermissions();
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        if (!result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)
                && !result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Boolean notified = result.get(Manifest.permission.POST_NOTIFICATIONS);
            Toast.makeText(this, Boolean.TRUE.equals(notified)
                    ? "通知权限已开启" : "通知权限未开启", Toast.LENGTH_SHORT).show();
            return;
        }
        Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
        Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
            startLocationUpdates();
        } else {
            updateLocationStatus(getString(R.string.location_denied),
                    getString(R.string.permission_settings), true);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            updateLocationStatus(getString(R.string.location_denied),
                    getString(R.string.permission_settings), true);
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            updateLocationStatus(getString(R.string.location_disabled),
                    "系统定位服务不可用", true);
            return;
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        2000L, 0f, locationListener, getMainLooper());
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        3000L, 0f, locationListener, getMainLooper());
            }
            Location last = null;
            try {
                last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {}
            if (last == null) {
                try {
                    last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (Exception ignored) {}
            }
            if (last != null) handleLocation(last);
            else updateLocationStatus(getString(R.string.location_waiting),
                    "请到室外或打开系统定位服务后稍候", false);
        } catch (SecurityException e) {
            updateLocationStatus(getString(R.string.location_denied),
                    getString(R.string.permission_settings), true);
        }
    }

    private void handleLocation(Location location) {
        lastLocation = location;
        JSONObject action = agent.evaluate(location.getLatitude(), location.getLongitude(),
                location.getAccuracy(), location.getTime());
        lastAction = action;
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
            homeFragment.showAction(action);
            if ("SHOW_CHECKIN_PROMPT".equals(action.optString("action", ""))) {
                homeFragment.scrollToPrompt();
            }
            homeFragment.setLocationStatus("定位已更新",
                    "精度 ±" + Math.round(location.getAccuracy()) + " m · "
                            + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.CHINA)
                            .format(new java.util.Date(location.getTime())),
                    false);
            updateNearestFromAction(action);
            homeFragment.showExtraPrompts(collectExtraPrompts(location,
                    action.optString("station", "")));
        }

        String type = action.optString("action", "");
        if ("SHOW_CHECKIN_PROMPT".equals(type)) {
            String station = action.optString("station", "");
            pendingAction = action;
            if (!station.equals(lastNotifiedStation)) {
                notifyPrompt(action);
                lastNotifiedStation = station;
            }
        } else {
            pendingAction = null;
            if ("NO_ELIGIBLE_STATION".equals(type)) lastNotifiedStation = null;
        }
    }

    private void updateNearestFromAction(JSONObject action) {
        if (homeFragment == null || !homeFragment.isAdded() || homeFragment.getView() == null
                || lastLocation == null || agent == null) {
            return;
        }
        homeFragment.updateNearestList(agent.nearestStations(
                lastLocation.getLatitude(), lastLocation.getLongitude(), 3));
    }
    private void updateLocationStatus(String title, String body, boolean showPermission) {
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) homeFragment.setLocationStatus(title, body, showPermission);
    }

    private void showPagePrompt(JSONObject action) {
        if (action == null) return;
        pendingAction = action;
        lastAction = action;
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
            homeFragment.showAction(action);
            homeFragment.scrollToPrompt();
        } else {
            showHome();
        }
    }

    @Override
    public void onRefreshLocation() {
        if (developerLocationLock) {
            developerLocationLock = false;
            if (lastRealLocation != null) {
                handleLocation(lastRealLocation);
            } else {
                startLocationUpdates();
            }
            return;
        }
        if (lastLocation != null) handleLocation(lastLocation);
        else startLocationUpdates();
    }

    @Override
    public void onLater() {
        if (pendingAction == null) return;
        String station = pendingAction.optString("station", "");
        JSONObject cancelled = agent.cancel(station, "user_cancelled");
        lastAction = cancelled;
        pendingAction = null;
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
            homeFragment.showAction(cancelled);
            homeFragment.hidePrompt();
        }
        Toast.makeText(this, "已取消本次打卡", Toast.LENGTH_SHORT).show();
    }

    private java.util.List<JSONObject> collectExtraPrompts(Location location, String primaryStation) {
        java.util.List<JSONObject> result = new ArrayList<>();
        int radius = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE)
                .getInt("checkin_radius_m", 100);
        for (CheckinAgent.Nearest n : agent.nearestStations(
                location.getLatitude(), location.getLongitude(), 5)) {
            if (n.station.name.equals(primaryStation) || n.distanceMeters > radius) continue;
            try {
                JSONObject o = new JSONObject();
                o.put("action", "SHOW_CHECKIN_PROMPT");
                o.put("station", n.station.name);
                o.put("line", n.station.lines.isEmpty() ? "" : n.station.lines.get(0));
                o.put("distance_m", (int) Math.round(n.distanceMeters));
                result.add(o);
            } catch (Exception ignored) {}
        }
        return result;
    }

    @Override
    public void onConfirmStation(String station, String line, boolean withPhotos) {
        if (lastLocation == null) {
            Toast.makeText(this, "定位已失效，请重新靠近车站", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject action = new JSONObject();
            action.put("action", "SHOW_CHECKIN_PROMPT");
            action.put("station", station);
            action.put("line", line);
            pendingAction = action;
            lastAction = action;
            onConfirmCheckin(withPhotos);
        } catch (Exception ignored) {}
    }

    @Override
    public void onConfirmCheckin(boolean withPhotos) {
        if (pendingAction == null) {
            Toast.makeText(this, "当前不在打卡范围", Toast.LENGTH_SHORT).show();
            return;
        }
        if (withPhotos) {
            showPhotoSourceDialog();
        } else {
            executeConfirm(0);
        }
    }

    private void onPhotosPicked(List<Uri> uris) {
        if (pendingAction == null) return;
        if (uris == null || uris.isEmpty()) {
            executeConfirm(0);
            return;
        }
        pendingPhotoUris = new ArrayList<>(uris.subList(0, Math.min(3, uris.size())));
        executeConfirm(pendingPhotoUris.size());
    }

    private void onCameraResult(Boolean success) {
        if (pendingAction == null) return;
        if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
            pendingPhotoUris = new ArrayList<>();
            pendingPhotoUris.add(pendingCameraUri);
            executeConfirm(1);
        } else {
            executeConfirm(0);
        }
    }

    private void showPhotoSourceDialog() {
        if (pendingAction == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("上传打卡图片")
                .setItems(new String[]{"现场拍摄", "从图库选择", "暂不上传"}, (dialog, which) -> {
                    if (which == 0) launchCamera();
                    else if (which == 1) launchPhotoPicker();
                    else executeConfirm(0);
                })
                .setNegativeButton("返回", null)
                .show();
    }

    private void launchCamera() {
        try {
            File dir = new File(getFilesDir(), "checkin_photos");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("Cannot create photo dir");
            File file = File.createTempFile("checkin_", ".jpg", dir);
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            CrashLog.write(e);
            Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show();
            showPhotoSourceDialog();
        }
    }

    private List<String> persistPhotoUris() {
        List<String> saved = new ArrayList<>();
        int index = 0;
        for (Uri uri : pendingPhotoUris) {
            if (uri == null) continue;
            try {
                File dir = new File(getFilesDir(), "checkin_photos");
                if (!dir.exists()) dir.mkdirs();
                File target = new File(dir, "saved_" + System.currentTimeMillis() + "_" + (index++) + ".jpg");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new java.io.FileOutputStream(target)) {
                    if (in == null) throw new java.io.IOException("Cannot open image");
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                }
                saved.add(Uri.fromFile(target).toString());
            } catch (Exception e) {
                saved.add(uri.toString());
            }
        }
        return saved;
    }

    private void executeConfirm(int photoCount) {
        if (pendingAction == null || lastLocation == null) {
            Toast.makeText(this, "定位已失效，请重新靠近车站", Toast.LENGTH_SHORT).show();
            return;
        }
        String station = pendingAction.optString("station", "");
        String line = pendingAction.optString("line", "");
        JSONObject result = agent.confirm(lastLocation.getLatitude(), lastLocation.getLongitude(),
                station, line, photoCount);
        lastAction = result;
        pendingAction = null;
        if ("CHECKIN_CONFIRMED".equals(result.optString("action", "")) && photoCount > 0) {
            String checkinGroup = result.optString("checkin_group", "");
            if (!checkinGroup.isEmpty()) {
                db.updatePhotos(checkinGroup, photoCount, persistPhotoUris());
            }
        }
        pendingPhotoUris.clear();
        pendingCameraUri = null;
        String action = result.optString("action", "");
        if ("CHECKIN_CONFIRMED".equals(action)) {
            int unlocked = result.optJSONArray("newly_unlocked") == null
                    ? 0 : result.optJSONArray("newly_unlocked").length();
            Toast.makeText(this, unlocked > 0
                    ? "打卡成功，新增 " + unlocked + " 项成就"
                    : "打卡成功", Toast.LENGTH_LONG).show();
            lastNotifiedStation = station;
            refreshCurrentList();
            // 重新按当前位置评估，保留仍在范围内的其他车站卡片，而不是全部清空
            handleLocation(lastLocation);
        } else {
            if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
                homeFragment.showAction(result);
                homeFragment.hidePrompt();
            }
            if ("CHECKIN_CANCELLED".equals(action)) {
                Toast.makeText(this, "out_of_radius".equals(result.optString("reason"))
                        ? "确认时已超出打卡范围，未写入记录"
                        : "已取消打卡", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "数据状态已更新", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void notifyPrompt(JSONObject action) {
        if (!getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE)
                .getBoolean(SETTING_NOTIFY, true)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String station = action.optString("station", "车站");
        String warning = action.optString("soft_warning", "").replace("**", "")
                .replace("\n\n", " ");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("pending_action_json", action.toString());
        intent.putExtra("pending_lat", lastLocation == null ? 0 : lastLocation.getLatitude());
        intent.putExtra("pending_lng", lastLocation == null ? 0 : lastLocation.getLongitude());
        intent.putExtra("pending_accuracy", lastLocation == null ? 0 : lastLocation.getAccuracy());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this,
                station.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title) + "：" + station)
                .setContentText(warning)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(warning))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pending);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_BASE + Math.abs(station.hashCode() % 10000), builder.build());
        }
    }

    private void launchPhotoPicker() {
        photoLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("进入轨道交通车站 100 m 范围时的打卡确认提醒");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String json = intent.getStringExtra("pending_action_json");
        if (json != null && !json.isEmpty()) {
            try {
                JSONObject action = new JSONObject(json);
                Location location = new Location("notification");
                location.setLatitude(intent.getDoubleExtra("pending_lat", 0));
                location.setLongitude(intent.getDoubleExtra("pending_lng", 0));
                location.setAccuracy(intent.getFloatExtra("pending_accuracy", 0));
                lastLocation = location;
                lastAction = action;
                if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) homeFragment.showAction(action);
            showPagePrompt(action);
            } catch (Exception ignored) {}
            Intent clean = new Intent(intent);
            clean.replaceExtras(new Bundle());
            setIntent(clean);
        }
    }
}



























