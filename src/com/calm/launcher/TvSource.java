package com.calm.launcher;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * Keeps MStar TVs on their Android ("storage") input. If the firmware still thinks a TV input
 * with no signal is selected, its no-signal timer shuts the TV down while Android apps play.
 * Reflection keeps the launcher working on non-MStar devices.
 */
final class TvSource {

    private static final String TAG = "Calm";
    private static final int INPUT_SOURCE_STORAGE = 34;

    static void useAndroid() {
        new Thread(() -> {
            try {
                Class<?> c = Class.forName("com.mstar.android.tv.TvCommonManager");
                Object manager = c.getMethod("getInstance").invoke(null);
                int current = (Integer) c.getMethod("getCurrentTvInputSource").invoke(manager);
                if (current == INPUT_SOURCE_STORAGE) return;
                Method set = c.getMethod("setInputSource", int.class);
                set.invoke(manager, INPUT_SOURCE_STORAGE);
                Log.i(TAG, "Input source " + current + " -> storage");
            } catch (ClassNotFoundException e) {
                // Not an MStar TV.
            } catch (Throwable t) {
                Log.w(TAG, "Couldn't switch input source", t);
            }
        }).start();
    }
}
