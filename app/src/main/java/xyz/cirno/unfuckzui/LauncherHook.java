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
    private Method taskbarActivityContext_isThreeButtonNav;
    private Method taskbarLauncherStateController_applyState;
    private Method taskbarLauncherStateController_updateStateForFlag;
    private Method launcherTaskbarUIController_onStashedInAppChanged;
    private Method taskbarStashController_updateAndAnimateIsManuallyStashedInApp;
    private boolean initialized = false;


    private SharedPreferences sp;
    private Object taskbarLauncherStateController;
    private Object taskbarActivityContext;
    private Object taskbarUIController;
    private Object taskbarStashController;


    private boolean isThreeButtonNav() {
        if (taskbarActivityContext_isThreeButtonNav == null) {
            return true;
        }
        try {
            var result = taskbarActivityContext_isThreeButtonNav.invoke(taskbarActivityContext);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignore) {}
        return false;
    }

    private void showTaskbar(boolean show) {
        // FIXME: doesn't work for the first launcher instance (started on boot)
        try {
            XposedBridge.invokeOriginalMethod(taskbarLauncherStateController_updateStateForFlag, taskbarLauncherStateController, new Object[]{1, !show});
            var animator = (android.animation.Animator) taskbarLauncherStateController_applyState.invoke(taskbarLauncherStateController, 300L, false);
            if (animator != null) {
                animator.start();
            } else {
                // flag not changed, toggle it to trigger action
                XposedBridge.invokeOriginalMethod(taskbarLauncherStateController_updateStateForFlag, taskbarLauncherStateController, new Object[]{1, show});
                var animator2 = (android.animation.Animator) taskbarLauncherStateController_applyState.invoke(taskbarLauncherStateController, 0L, false);
                animator2.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        try {
                            XposedBridge.invokeOriginalMethod(taskbarLauncherStateController_updateStateForFlag, taskbarLauncherStateController, new Object[]{1, !show});
                            taskbarLauncherStateController_applyState.invoke(taskbarLauncherStateController, 300L, true);
                        } catch (Exception e) {
                            Log.e("UnfuckZUI", "showTaskbar " + show, e);
                        }
                    }
                });
                animator2.start();
            }
        } catch (Exception e) {
            Log.e("UnfuckZUI", "showTaskbar " + show, e);
        }
    }

    private void onDisableTaskbarChanged() {
        try {
            if (isThreeButtonNav()) {
                showTaskbar(!disableTaskBar);
                // trigger update of animation bounds
                launcherTaskbarUIController_onStashedInAppChanged.invoke(taskbarUIController);
            } else {
                if (disableTaskBar) {
                    // change taskbar mode from swipe up to long press
                    var cr = AndroidAppHelper.currentApplication().getContentResolver();
                    var taskbar_mode = Settings.Secure.getInt(cr, "open_taskbar", 1);
                    if (taskbar_mode != 0) {
                        Settings.Secure.putInt(cr, "open_taskbar", 0);
                    }
                    // hide (stash) taskbar
                    taskbarStashController_updateAndAnimateIsManuallyStashedInApp.invoke(taskbarStashController, true);
                }
            }
        } catch (Exception ignore) {}
    }

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


        // disable taskbar - hooks

        final var TaskbarActivityContextClass = XposedHelpers.findClass("com.zui.launcher.taskbar.TaskbarActivityContext", lpparam.classLoader);
        taskbarActivityContext_isThreeButtonNav = XposedHelpers.findMethodExact(TaskbarActivityContextClass, "isThreeButtonNav");
        XposedHelpers.findAndHookMethod(TaskbarActivityContextClass, "init", "com.zui.launcher.taskbar.TaskbarSharedState", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                taskbarActivityContext = param.thisObject;
                initialized = true;
            }
        });


        final var taskbarLauncherStateControllerClass = XposedHelpers.findClass("com.zui.launcher.taskbar.TaskbarLauncherStateController", lpparam.classLoader);
        taskbarLauncherStateController_applyState = XposedHelpers.findMethodExact(taskbarLauncherStateControllerClass, "applyState", long.class, boolean.class);
        taskbarLauncherStateController_updateStateForFlag = XposedHelpers.findMethodExact(taskbarLauncherStateControllerClass, "updateStateForFlag", int.class, boolean.class);
        XposedBridge.hookMethod(taskbarLauncherStateController_updateStateForFlag, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (taskbarLauncherStateController == null) {
                    taskbarLauncherStateController = param.thisObject;
                }
                var bit = (int) param.args[0];
                var set = (boolean) param.args[1];
                if (initialized && bit == 1 && disableTaskBar && isThreeButtonNav()) {
                    param.args[1] = Boolean.TRUE;
                }
            }
        });


        final var taskbarStashController_class = XposedHelpers.findClass("com.zui.launcher.taskbar.TaskbarStashController", lpparam.classLoader);
        taskbarStashController_updateAndAnimateIsManuallyStashedInApp = XposedHelpers.findMethodExact(taskbarStashController_class, "updateAndAnimateIsManuallyStashedInApp", boolean.class);

        // affects DeviceProfile.isTaskbarPresentInApps -> SyncRtSurfaceTransactionApplierCompat$SurfaceParams$Builder.withWindowCrop
        XposedHelpers.findAndHookMethod(taskbarStashController_class, "isStashedInApp", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (initialized && disableTaskBar && isThreeButtonNav()) {
                    param.setResult(Boolean.TRUE);
                }
            }
        });

        XposedHelpers.findAndHookMethod("com.zui.launcher.taskbar.TaskbarStashController", lpparam.classLoader, "init", "com.zui.launcher.taskbar.TaskbarControllers", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                taskbarStashController = param.thisObject;
            }
        });

        XposedHelpers.findAndHookMethod(taskbarStashController_class, "updateStateForFlag", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var bit = (int) param.args[0];
                var set = (boolean) param.args[1];
                // Log.d("UnfuckZUI", "TaskbarStashController updateStateForFlag bit=" + bit + " set=" + set);
                if (initialized && bit == 2 && disableTaskBar && !isThreeButtonNav()) {
                    param.args[1] = Boolean.TRUE;
                }
            }
        });

        XposedHelpers.findAndHookMethod(taskbarStashController_class, "startUnstashHint", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (initialized && disableTaskBar && !isThreeButtonNav()) {
                    param.setResult(null);
                }
            }
        });

        XposedHelpers.findAndHookMethod(taskbarStashController_class, "onLongPressToUnstashTaskbar", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (initialized && disableTaskBar && !isThreeButtonNav()) {
                    param.setResult(Boolean.FALSE);
                }
            }
        });

//        final var featureFlag_class = XposedHelpers.findClass("com.zui.launcher.config.FeatureFlags", lpparam.classLoader);
//        final var booleanFlag_class = XposedHelpers.findClass("com.zui.launcher.config.FeatureFlags$BooleanFlag", lpparam.classLoader);
//        final var booleanFlag_value = XposedHelpers.findField(booleanFlag_class, "defaultValue");
//
//        XposedHelpers.findAndHookMethod(featureFlag_class, "initialize", android.content.Context.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                var app = AndroidAppHelper.currentApplication();
////                sp = app.createDeviceProtectedStorageContext().getSharedPreferences(PERF_NAME, Context.MODE_PRIVATE);
////                disableTaskBar = sp.getBoolean(PERF_KEY_DISABLE_TASKBAR, false);
////                disableForceStop = sp.getBoolean(PERF_KEY_DISABLE_FORCE_STOP, true);
//////                featureFlagTaskbar = XposedHelpers.getStaticObjectField(featureFlag_class, "ENABLE_TASKBAR");
//////                featureFlagZuiTaskbar = XposedHelpers.getStaticObjectField(featureFlag_class, "ENABLE_ZUI_TASKBAR");
//                var enableZuiAnimation = XposedHelpers.getStaticObjectField(featureFlag_class, "ENABLE_ZUI_ANIMATION");
//                booleanFlag_value.set(enableZuiAnimation, Boolean.FALSE);
//            }
//        });

        final var launcherTaskbarUIController_class = XposedHelpers.findClass("com.zui.launcher.taskbar.LauncherTaskbarUIController", lpparam.classLoader);
        launcherTaskbarUIController_onStashedInAppChanged = XposedHelpers.findMethodExact(launcherTaskbarUIController_class, "onStashedInAppChanged");
        XposedHelpers.findAndHookMethod("com.zui.launcher.taskbar.LauncherTaskbarUIController", lpparam.classLoader, "init", "com.zui.launcher.taskbar.TaskbarControllers", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                taskbarUIController = param.thisObject;
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
                var disableTaskbarPreference = new android.preference.SwitchPreference(screen.getContext());
                disableTaskbarPreference.setTitle(modulectx.getString(R.string.disable_taskbar_title));
                disableTaskbarPreference.setChecked(disableTaskBar);
                disableTaskbarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        disableTaskBar = Boolean.TRUE.equals(newValue);
                        var editor = sp.edit();
                        editor.putBoolean(PERF_KEY_DISABLE_TASKBAR, disableTaskBar);
                        editor.apply();
                        onDisableTaskbarChanged();
                        return true;
                    }
                });
                if (isThreeButtonNav()) {
                    disableTaskbarPreference.setSummary(modulectx.getString(R.string.disable_taskbar_summary_3button));
                }
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
                screen.addPreference(disableTaskbarPreference);
                screen.addPreference(disableForceStopPreference);

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
