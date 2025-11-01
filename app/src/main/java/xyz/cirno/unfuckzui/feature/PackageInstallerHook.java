package xyz.cirno.unfuckzui.feature;

import android.app.Activity;
import android.view.Window;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class PackageInstallerHook {
    public static final String FEATURE_NAME = "package_installer_style";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.packageinstaller"}, PackageInstallerHook::handleLoadPackage);

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.android.packageinstaller".equals(lpparam.packageName)) {
            handleZuiPackageInstaller(lpparam);
        }
    }

    static void handleZuiPackageInstaller(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        final var utilsClass = XposedHelpers.findClass("com.android.packageinstaller.extra.Utils", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(utilsClass, "isCTSandGTS", String.class, XC_MethodReplacement.returnConstant(Boolean.TRUE));
        XposedHelpers.findAndHookMethod(utilsClass, "isCTSandGTS", String.class, android.content.Intent.class, XC_MethodReplacement.returnConstant(Boolean.TRUE));

        final var rStyleCls = XposedHelpers.findClass("com.android.packageinstaller.R$style", lpparam.classLoader);
        final var Theme_AlertDialogActivity = XposedHelpers.getStaticIntField(rStyleCls, "Theme_AlertDialogActivity");

        // AOSP package installer doesn't set style on individual activities
        XposedHelpers.findAndHookMethod(android.app.Activity.class, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var activity = (Activity) param.thisObject;
                activity.setTheme(Theme_AlertDialogActivity);
                activity.setTranslucent(true);
                activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
                activity.getWindow().setWindowAnimations(0);
            }
        });
    }

    private static class ActivityStyleHook extends XC_MethodHook {
        private final int newStyleId;
        private final boolean overrideAnimation;

        public ActivityStyleHook(int style, boolean overrideAnimation) {
            newStyleId = style;
            this.overrideAnimation = overrideAnimation;
        }
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            var activity = (android.app.Activity) param.thisObject;
            activity.setTheme(newStyleId);
            activity.setTranslucent(true);
            if (overrideAnimation) {
                activity.overridePendingTransition(0, 0);
            }
        }
    }
}
