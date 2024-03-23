package xyz.cirno.unfuckzui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class FeatureControl {
    private final XSharedPreferences xsp;
    private volatile boolean receiverRegistered = false;
    private final Object lock = new Object();
    private final List<Runnable> subListener = new ArrayList<>();

    private FeatureControl() {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, "feature_config");
    }

    public static FeatureControl getInstance() {
        return LazyInitHolder.INSTANCE;
    }

    private static final class LazyInitHolder {
        static final FeatureControl INSTANCE = new FeatureControl();
    }

    public boolean isFeatureEnabled(String featureName) {
        return xsp.getBoolean(featureName, true);
    }

    private void ensureXspChangeListener() {
        if (receiverRegistered) {
            return;
        }
        synchronized (lock) {
            if (!receiverRegistered) {
                XposedHelpers.findAndHookMethod(android.app.Application.class, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        var app = (android.app.Application) param.thisObject;
                        var context = app.getApplicationContext();
                        context.registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                xsp.reload();
                                for (Runnable listener : subListener) {
                                    listener.run();
                                }
                            }
                        }, new android.content.IntentFilter("xyz.cirno.unfuckzui.RELOAD_CONFIG"), Context.RECEIVER_EXPORTED);
                    }
                });
                receiverRegistered = true;
            }
        }
    }

    public void registerFeatureChangeListener(Runnable listener) {
        ensureXspChangeListener();
        subListener.add(listener);
    }

    public void unregisterFeatureChangeListener(Runnable listener) {
        subListener.remove(listener);
    }
}
