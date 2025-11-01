package xyz.cirno.unfuckzui.feature;

import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class DisableTaskbar {
    public static final String FEATURE_NAME = "disable_taskbar";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.launcher", "com.android.systemui"}, DisableTaskbar::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT >= 35) {
            // setting available since middle ZUI 16
            return;
        }
        if ("com.zui.launcher".equals(lpparam.packageName)) {
            // check if taskbar is configurable in this build
            var TaskbarManagerClass = XposedHelpers.findClassIfExists("com.zui.launcher.taskbar.TaskbarManager", lpparam.classLoader);
            var field = TaskbarManagerClass != null ? XposedHelpers.findFieldIfExists(TaskbarManagerClass, "ZUI_TASKBAR_FEATURE_ENABLE") : null;
            if (field == null) {
                // only hook if taskbar is not configurable
                XposedHelpers.findAndHookMethod("com.zui.launcher.DeviceProfile$Builder", lpparam.classLoader, "build", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // not in hot path
                        if (param.hasThrowable()) return;
                        var profile = param.getResult();
                        if (XposedHelpers.getIntField(profile, "displayId") == 0) {
                            XposedHelpers.setBooleanField(profile, "isTaskbarPresent", false);
                        }
                    }
                });
            }
        } else if ("com.android.systemui".equals(lpparam.packageName)) {
            new SystemUIHook().handleLoadSystemUi(lpparam);
        }
    }

    static class SystemUIHook {
        private final ThreadLocal<Boolean> isPad = ThreadLocal.withInitial(() -> null);

        public void handleLoadSystemUi(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
            // context.getResources().getDimension(android.content.res.Resources.getSystem().getIdentifier("navigation_bar_height", "dimen", "android"))


            var NavigationBarControllerClass = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationBarController", lpparam.classLoader);

            if (NavigationBarControllerClass != null && XposedHelpers.findFieldIfExists(NavigationBarControllerClass, "SETTINGS_SYSTEM_ZUI_TASKBAR_FEATURE_ENABLED") != null) {
                // taskbar is configurable in this build
                return;
            }

            try {
                XposedHelpers.findAndHookMethod("com.android.systemui.shared.recents.utilities.Utilities", lpparam.classLoader, "isTablet", android.content.Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // not in hot path
                        final var stack = Thread.currentThread().getStackTrace();
                        for (final var line: stack) {
                            if (line.getClassName().contains("NavigationBarController")) {
                                param.setResult(false);
                                return;
                            }
                        }
                    }
                });
            } catch (Throwable ignore) {
            }

            final var isPadMethod = Build.VERSION.SDK_INT == 33 ? "isPad" : "isDevicePad";
            XposedHelpers.findAndHookMethod("com.android.systemui.util.XSystemUtil", lpparam.classLoader, isPadMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    var hookedValue = isPad.get();
                    if (hookedValue != null) {
                        param.setResult(Boolean.TRUE.equals(hookedValue));
                    }
                }
            });

            XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader, "onLikelyDefaultLayoutChange", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    isPad.set(Boolean.FALSE);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    isPad.remove();
                }
            });

        }
    }
}
