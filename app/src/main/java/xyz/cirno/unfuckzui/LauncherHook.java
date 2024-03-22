package xyz.cirno.unfuckzui;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LauncherHook {
    private static final String PERF_NAME = "unfuckzui";
    private static final String PERF_KEY_DISABLE_TASKBAR = "disable_taskbar";
    private static final String PERF_KEY_DISABLE_FORCE_STOP = "disable_force_stop";

    private volatile boolean disableTaskBar = false;
    private volatile boolean disableForceStop = false;


    private SharedPreferences sp;


    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedHelpers.findAndHookMethod(android.app.Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var app = (android.app.Application) param.thisObject;
                sp = app.createDeviceProtectedStorageContext().getSharedPreferences(PERF_NAME, Context.MODE_PRIVATE);
                disableTaskBar = sp.getBoolean(PERF_KEY_DISABLE_TASKBAR, false);
                disableForceStop = sp.getBoolean(PERF_KEY_DISABLE_FORCE_STOP, true);
            }
        });



        // disable force stop - hooks
        var amwclass = XposedHelpers.findClass("com.android.systemui.shared.system.ActivityManagerWrapper", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(amwclass, "removeAllRunningAppProcesses", android.content.Context.class, java.util.ArrayList.class, new DisableForceStopHook());
        XposedHelpers.findAndHookMethod(amwclass, "removeAppProcess", android.content.Context.class, int.class, String.class, int.class, new DisableForceStopHook());


        final var zuiPreferenceCategory_class = XposedHelpers.findClass("zui.preference.PreferenceCategory", lpparam.classLoader);
        final var zuiPreferenceCategory_ctor = XposedHelpers.findConstructorExact(zuiPreferenceCategory_class, android.content.Context.class);

        XposedHelpers.findAndHookMethod("com.zui.launcher.settings.ZuiLauncherFragment", lpparam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var fragment = (android.preference.PreferenceFragment) param.thisObject;

                var modulectx = fragment.getContext().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);

                var screen = fragment.getPreferenceScreen();
                var divider = (android.preference.Preference) zuiPreferenceCategory_ctor.newInstance(screen.getContext());
                var category = new android.preference.PreferenceCategory(screen.getContext());
                category.setTitle("UnfuckZUI");
                screen.addPreference(divider);
                screen.addPreference(category);

                var disableForceStopPreference = new android.preference.SwitchPreference(screen.getContext());
                disableForceStopPreference.setTitle(modulectx.getString(R.string.disable_force_stop_title));
                disableForceStopPreference.setSummary(modulectx.getString(R.string.disable_force_stop_summary));
                disableForceStopPreference.setChecked(disableForceStop);
                disableForceStopPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        disableForceStop = Boolean.TRUE.equals(newValue);
                        var editor = sp.edit();
                        editor.putBoolean(PERF_KEY_DISABLE_FORCE_STOP, disableForceStop);
                        editor.apply();
                        return true;
                    }
                });
                screen.addPreference(disableForceStopPreference);

            }
        });

        XposedHelpers.findAndHookMethod("com.zui.launcher.DeviceProfile$Builder", lpparam.classLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.hasThrowable()) return;
                var profile = param.getResult();
                if (XposedHelpers.getIntField(profile, "displayId") == 0) {
                    XposedHelpers.setBooleanField(profile, "isTaskbarPresent", false);
                }
            }
        });


    }

    private class DisableForceStopHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (disableForceStop) {
                param.setResult(null);
            }
        }
    }
}
