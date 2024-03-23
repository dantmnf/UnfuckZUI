package xyz.cirno.unfuckzui.feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class DisableTaskbar {
    public static final String FEATURE_NAME = "disable_taskbar";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.launcher", "com.android.systemui", "com.android.settings"}, DisableTaskbar::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.zui.launcher".equals(lpparam.packageName)) {
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
        } else if ("com.android.systemui".equals(lpparam.packageName)) {
            new SystemUIHook().handleLoadSystemUi(lpparam);
        } else if ("com.android.settings".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.android.settings.homepage.SettingsHomepageActivity", lpparam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    var activity = (android.app.Activity) param.thisObject;
                    var window = activity.getWindow();
                    window.setNavigationBarColor(0x00000000);
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                }
            });
        }
    }

    static class SystemUIHook {
        private final ThreadLocal<Boolean> isPad = ThreadLocal.withInitial(() -> null);

        public void handleLoadSystemUi(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

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


            XposedHelpers.findAndHookMethod("com.android.systemui.util.XSystemUtil", lpparam.classLoader, "isPad", new XC_MethodHook() {
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
                    isPad.set(null);
                }
            });

        }
    }
}
