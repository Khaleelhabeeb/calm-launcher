package com.calm.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The ordered list of dock apps, saved as flattened component names. */
final class DockStore {

    static final int MAX = 8;

    private static final String[] DEFAULTS = {
            "org.smarttube.beta",
            "com.mstar.tv.tvplayer.ui/.RootActivity",
            "com.android.browser",
            "com.android.settings",
    };

    private final SharedPreferences prefs;
    private final AppStore apps;

    DockStore(Context context, AppStore apps) {
        this.prefs = context.getSharedPreferences("dock", Context.MODE_PRIVATE);
        this.apps = apps;
    }

    List<String> load() {
        String saved = prefs.getString("ids", null);
        if (saved != null) {
            return new ArrayList<>(saved.isEmpty()
                    ? new ArrayList<String>() : Arrays.asList(saved.split(";")));
        }
        List<String> ids = new ArrayList<>();
        for (String spec : DEFAULTS) {
            AppStore.App app = apps.resolve(spec);
            if (app != null) ids.add(app.id());
        }
        save(ids);
        return ids;
    }

    void save(List<String> ids) {
        prefs.edit().putString("ids", TextUtils.join(";", ids)).apply();
    }

    boolean contains(String id) {
        return load().contains(id);
    }

    boolean add(String id) {
        List<String> ids = load();
        if (ids.contains(id) || ids.size() >= MAX) return false;
        ids.add(id);
        save(ids);
        return true;
    }

    void remove(String id) {
        List<String> ids = load();
        ids.remove(id);
        save(ids);
    }

    /** Moves an app by delta places; returns its new index. */
    int move(String id, int delta) {
        List<String> ids = load();
        int from = ids.indexOf(id);
        int to = Math.max(0, Math.min(ids.size() - 1, from + delta));
        if (from < 0 || from == to) return from;
        ids.remove(from);
        ids.add(to, id);
        save(ids);
        return to;
    }
}
