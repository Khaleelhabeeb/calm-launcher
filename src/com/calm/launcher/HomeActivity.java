package com.calm.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Home screen: blurred wallpaper, a big clock, and a dock. "Apps" opens every app in a grid. */
public class HomeActivity extends Activity {

    private AppStore apps;
    private DockStore dockStore;

    private FrameLayout root;
    private LinearLayout dock;
    private FrameLayout overlay;
    private GridView grid;
    private AppsAdapter adapter;
    private View selectedGridTile;
    private NetworkIcon netIcon;
    private TextView netLabel;

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            apps.reload();
            if (adapter != null) adapter.notifyDataSetChanged();
            rebuildDock(-1);
        }
    };

    private final BroadcastReceiver networkChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNetwork();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        apps = new AppStore(this);
        dockStore = new DockStore(this, apps);

        root = new FrameLayout(this);
        root.setClipChildren(false);
        root.setBackground(Wallpaper.load(this));
        root.addView(buildClock(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.TOP));

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        dockLp.bottomMargin = dp(60);
        root.addView(dock, dockLp);

        FrameLayout.LayoutParams netLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        netLp.topMargin = dp(36);
        netLp.rightMargin = dp(48);
        root.addView(buildNetwork(), netLp);

        setContentView(root);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packagesChanged, filter);

        IntentFilter net = new IntentFilter();
        net.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        net.addAction(WifiManager.RSSI_CHANGED_ACTION);
        net.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        net.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(networkChanged, net);
        updateNetwork();

        rebuildDock(0);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        unregisterReceiver(networkChanged);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyDroppedWallpaper();
    }

    /** Pressing Home while we're showing closes the app grid. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (isAppsShown()) hideApps();
    }

    @Override
    public void onBackPressed() {
        if (isAppsShown()) hideApps();
        // On the home screen Back does nothing, so nobody gets stuck.
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isAppsShown()) {
                Object item = grid.getSelectedItem();
                if (item != null) showAppMenu((AppStore.App) item);
            } else {
                View focus = getCurrentFocus();
                Object app = focus == null ? null : focus.getTag(R.id.tile_app);
                if (app != null) showDockMenu((AppStore.App) app);
                else showHomeMenu();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---- Clock ----

    private View buildClock() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, dp(150), 0, 0);

        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 120);
        time.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        styleText(time);
        col.addView(time, fullWidth());

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        date.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        styleText(date);
        date.setTextColor(0xDDFFFFFF);
        col.addView(date, fullWidth());
        return col;
    }

    /** Full-width rows with centred text, so the clock stays centred whatever its width. */
    private static LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void styleText(TextView tv) {
        tv.setTextColor(0xFFFFFFFF);
        tv.setShadowLayer(12, 0, 2, 0x88000000);
        tv.setIncludeFontPadding(false);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    // ---- Network ----

    private View buildNetwork() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        netLabel = new TextView(this);
        netLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        netLabel.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        netLabel.setTextColor(0xFFFFFFFF);
        netLabel.setShadowLayer(8, 0, 1, 0x99000000);
        netLabel.setPadding(0, 0, dp(12), 0);
        row.addView(netLabel);

        netIcon = new NetworkIcon();
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(netIcon);
        row.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        return row;
    }

    private void updateNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            netIcon.set(NetworkIcon.OFFLINE, 0);
            netLabel.setText("Not connected");
        } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
            netIcon.set(NetworkIcon.ETHERNET, 0);
            netLabel.setText("Ethernet");
        } else {
            WifiInfo wifi = ((WifiManager) getSystemService(WIFI_SERVICE)).getConnectionInfo();
            int bars = wifi == null ? 0 : WifiManager.calculateSignalLevel(wifi.getRssi(), 4);
            String ssid = wifi == null ? null : wifi.getSSID();
            if (ssid != null) ssid = ssid.replace("\"", "");
            netIcon.set(NetworkIcon.WIFI, Math.max(1, bars));
            netLabel.setText(ssid == null || ssid.isEmpty() || ssid.contains("unknown") ? "Wi-Fi" : ssid);
        }
    }

    // ---- Dock ----

    private void rebuildDock(int focusIndex) {
        if (focusIndex < 0) {
            View f = dock.getFocusedChild();
            focusIndex = f == null ? -1 : dock.indexOfChild(f);
        }
        dock.removeAllViews();
        for (String id : dockStore.load()) {
            AppStore.App app = apps.find(id);
            if (app == null) continue;
            View tile = dockTile(apps.icon(app), app.label);
            tile.setTag(R.id.tile_app, app);
            tile.setOnClickListener(v -> launch(app));
            tile.setOnLongClickListener(v -> {
                showDockMenu(app);
                return true;
            });
            dock.addView(tile);
        }
        View all = dockTile(new Tiles.Dots(), "Apps");
        all.setOnClickListener(v -> showApps());
        all.setOnLongClickListener(v -> {
            showHomeMenu();
            return true;
        });
        dock.addView(all);

        if (focusIndex >= 0 && !isAppsShown()) {
            View target = dock.getChildAt(Math.min(focusIndex, dock.getChildCount() - 1));
            target.post(target::requestFocus);
        }
    }

    private View dockTile(android.graphics.drawable.Drawable icon, String label) {
        LinearLayout tile = Tiles.make(this, 104, false);
        Tiles.Holder h = (Tiles.Holder) tile.getTag();
        h.icon.setImageDrawable(icon);
        h.label.setText(label);
        tile.setFocusable(true);
        tile.setOnFocusChangeListener((v, has) -> Tiles.setFocused(v, has, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = lp.rightMargin = dp(16);
        tile.setLayoutParams(lp);
        return tile;
    }

    // ---- App grid ----

    private boolean isAppsShown() {
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    private void showApps() {
        if (overlay == null) buildOverlay();
        adapter.notifyDataSetChanged();
        overlay.setAlpha(0f);
        overlay.setVisibility(View.VISIBLE);
        overlay.animate().alpha(1f).setDuration(160).start();
        grid.setSelection(0);
        grid.requestFocus();
    }

    private void hideApps() {
        overlay.setVisibility(View.GONE);
        if (selectedGridTile != null) Tiles.reset(selectedGridTile);
        selectedGridTile = null;
        View all = dock.getChildAt(dock.getChildCount() - 1);
        if (all != null) all.requestFocus();
    }

    private void buildOverlay() {
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xB0000000);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(80), dp(48), dp(80), 0);
        col.setClipChildren(false);

        TextView title = new TextView(this);
        title.setText("Apps");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        styleText(title);
        title.setPadding(dp(12), 0, 0, dp(24));
        col.addView(title);

        grid = new GridView(this);
        grid.setNumColumns(7);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(28));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(16), 0, dp(48));
        grid.setVerticalScrollBarEnabled(false);
        adapter = new AppsAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (selectedGridTile != null && selectedGridTile != view) {
                    Tiles.setFocused(selectedGridTile, false, true);
                }
                selectedGridTile = view;
                Tiles.setFocused(view, true, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        grid.setOnItemClickListener((p, v, pos, id) -> {
            launch(adapter.getItem(pos));
            hideApps();
        });
        grid.setOnItemLongClickListener((p, v, pos, id) -> {
            showAppMenu(adapter.getItem(pos));
            return true;
        });
        col.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        overlay.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setVisibility(View.GONE);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private final class AppsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return apps.all().size();
        }

        @Override
        public AppStore.App getItem(int position) {
            return apps.all().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) view = Tiles.make(HomeActivity.this, 92, true);
            AppStore.App app = getItem(position);
            Tiles.Holder h = (Tiles.Holder) view.getTag();
            h.icon.setImageDrawable(apps.icon(app));
            h.label.setText(app.label);
            boolean selected = grid.hasFocus() && position == grid.getSelectedItemPosition();
            if (selected) {
                Tiles.setFocused(view, true, true);
                selectedGridTile = view;
            } else {
                if (view == selectedGridTile) selectedGridTile = null;
                Tiles.reset(view);
            }
            return view;
        }
    }

    // ---- Menus ----

    private void showDockMenu(AppStore.App app) {
        String[] items = {"Move left", "Move right", "Remove from dock", "App info", "Uninstall",
                "Change wallpaper"};
        menu(app.label, items, which -> {
            switch (which) {
                case 0: rebuildDock(dockStore.move(app.id(), -1)); break;
                case 1: rebuildDock(dockStore.move(app.id(), 1)); break;
                case 2: dockStore.remove(app.id()); rebuildDock(-1); break;
                case 3: appInfo(app); break;
                case 4: uninstall(app); break;
                case 5: chooseWallpaper(); break;
            }
        });
    }

    private void showAppMenu(AppStore.App app) {
        boolean docked = dockStore.contains(app.id());
        String[] items = {docked ? "Remove from dock" : "Add to dock", "App info", "Uninstall"};
        menu(app.label, items, which -> {
            switch (which) {
                case 0:
                    if (docked) dockStore.remove(app.id());
                    else if (!dockStore.add(app.id())) toast("The dock is full");
                    rebuildDock(-1);
                    break;
                case 1: appInfo(app); break;
                case 2: uninstall(app); break;
            }
        });
    }

    private void showHomeMenu() {
        menu("Calm launcher", new String[]{"Change wallpaper", "Android settings"}, which -> {
            if (which == 0) chooseWallpaper();
            else start(new Intent(Settings.ACTION_SETTINGS));
        });
    }

    private interface OnPick {
        void pick(int which);
    }

    private void menu(String title, String[] items, OnPick onPick) {
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(title)
                .setItems(items, (d, which) -> onPick.pick(which))
                .show();
    }

    // ---- Wallpaper ----

    // Writable over adb even though this TV's shared storage is read-only.
    private static final File DROP_DIR = new File("/data/local/tmp/wallpapers");

    /** Uses the newest image pushed to DROP_DIR, once per new file. */
    private void applyDroppedWallpaper() {
        List<File> files = new ArrayList<>();
        collectImages(DROP_DIR, files);
        File newest = null;
        for (File f : files) if (newest == null || f.lastModified() > newest.lastModified()) newest = f;
        if (newest == null) return;
        String key = newest.getPath() + "@" + newest.lastModified();
        android.content.SharedPreferences prefs = getSharedPreferences("wallpaper", MODE_PRIVATE);
        if (key.equals(prefs.getString("dropped", null))) return;
        prefs.edit().putString("dropped", key).apply();
        setWallpaper(newest.getPath());
    }

    private void setWallpaper(String path) {
        new Thread(() -> {
            boolean ok = Wallpaper.apply(this, path);
            runOnUiThread(() -> {
                if (ok) root.setBackground(Wallpaper.load(this));
                else toast("Couldn't open that image");
            });
        }).start();
    }

    private static final String[] IMAGE_DIRS = {"", "Pictures", "Download", "DCIM", "DCIM/Camera"};

    /** Lists images from internal storage and USB drives; "Default" restores the built-in glow. */
    private void chooseWallpaper() {
        List<File> files = new ArrayList<>();
        File sd = Environment.getExternalStorageDirectory();
        for (String dir : IMAGE_DIRS) collectImages(new File(sd, dir), files);
        // Readable by apps and writable over adb, for TVs whose storage is read-only.
        collectImages(DROP_DIR, files);
        File[] usb = new File("/mnt/usb").listFiles();
        if (usb != null) for (File drive : usb) collectImages(drive, files);

        String[] names = new String[files.size() + 1];
        names[0] = "Default";
        for (int i = 0; i < files.size(); i++) names[i + 1] = files.get(i).getName();
        if (files.isEmpty()) toast("Put a .jpg or .png in Pictures or Download to use it here");

        menu("Wallpaper", names, which -> {
            if (which == 0) {
                Wallpaper.reset(this);
                root.setBackground(Wallpaper.load(this));
                return;
            }
            String path = files.get(which - 1).getPath();
            toast("Setting wallpaper…");
            setWallpaper(path);
        });
    }

    private static void collectImages(File dir, List<File> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            String n = f.getName().toLowerCase(Locale.US);
            if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                    || n.endsWith(".webp"))) {
                out.add(f);
            }
        }
    }

    // ---- Launching ----

    private void launch(AppStore.App app) {
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        start(i);
    }

    private void appInfo(AppStore.App app) {
        start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.component.getPackageName(), null)));
    }

    private void uninstall(AppStore.App app) {
        start(new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", app.component.getPackageName(), null)));
    }

    private void start(Intent intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            toast("Couldn't open that");
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(float v) {
        return Tiles.dp(this, v);
    }
}
