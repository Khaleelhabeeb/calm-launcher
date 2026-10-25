package com.calm.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Installed launchable apps. Icons are loaded lazily and kept, since a TV has few apps. */
final class AppStore {

    static final class App {
        final ComponentName component;
        final String label;
        final ActivityInfo info;
        Drawable icon;

        App(ActivityInfo info, String label) {
            this.info = info;
            this.label = label;
            this.component = new ComponentName(info.packageName, info.name);
        }

        String id() {
            return component.flattenToShortString();
        }
    }

    // Some vendor activities have unhelpful labels.
    private static final Map<String, String> LABELS = new HashMap<>();
    static {
        LABELS.put("com.mstar.tv.tvplayer.ui", "TV inputs");
    }

    private final PackageManager pm;
    private final String self;
    private final List<App> apps = new ArrayList<>();
    private final Map<String, App> extras = new HashMap<>();

    AppStore(Context context) {
        pm = context.getPackageManager();
        self = context.getPackageName();
        reload();
    }

    void reload() {
        apps.clear();
        extras.clear();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            if (self.equals(ri.activityInfo.packageName)) continue;
            apps.add(new App(ri.activityInfo, labelOf(ri.activityInfo)));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    List<App> all() {
        return apps;
    }

    Drawable icon(App app) {
        if (app.icon == null) app.icon = app.info.loadIcon(pm);
        return app.icon;
    }

    /** Finds an app by flattened component, including activities without a launcher entry. */
    App find(String id) {
        ComponentName cn = ComponentName.unflattenFromString(id);
        if (cn == null) return null;
        for (App a : apps) if (a.component.equals(cn)) return a;
        App extra = extras.get(id);
        if (extra != null) return extra;
        try {
            ActivityInfo info = pm.getActivityInfo(cn, 0);
            extra = new App(info, labelOf(info));
            extras.put(id, extra);
            return extra;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Resolves "package" or "package/.Activity" to an app, or null if it isn't installed. */
    App resolve(String spec) {
        if (spec.contains("/")) return find(spec);
        Intent launch = pm.getLaunchIntentForPackage(spec);
        return launch == null ? null : find(launch.getComponent().flattenToShortString());
    }

    private String labelOf(ActivityInfo info) {
        String fixed = LABELS.get(info.packageName);
        return fixed != null ? fixed : String.valueOf(info.loadLabel(pm));
    }
}
