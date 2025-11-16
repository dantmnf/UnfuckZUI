package xyz.cirno.unfuckzui.feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class UnlockCnGms {
    public static final String FEATURE_NAME = "unlock_cn_gms";

    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"android"}, UnlockCnGms::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.processName)) {
            XposedBridge.hookAllConstructors(XposedHelpers.findClass("com.android.server.SystemConfig", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var that = param.thisObject;
                    XposedHelpers.callMethod(that, "removeFeature", "cn.google.services");
                    XposedHelpers.callMethod(that, "removeFeature", "com.google.android.feature.services_updater");
                }
            });
        }
    }
}
