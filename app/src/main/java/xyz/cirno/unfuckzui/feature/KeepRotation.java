package xyz.cirno.unfuckzui.feature;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class KeepRotation {
    public static final String FEATURE_NAME = "keep_rotation";

    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"android"}, KeepRotation::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.processName)) {
            XposedHelpers.findAndHookMethod("com.zui.server.wm.ZuiDisplayRotation", lpparam.classLoader, "isRotationCts", XC_MethodReplacement.returnConstant(Boolean.TRUE));
        }
    }
}
