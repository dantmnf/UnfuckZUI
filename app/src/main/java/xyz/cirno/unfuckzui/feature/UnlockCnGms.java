package xyz.cirno.unfuckzui.feature;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

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
            var lookup = MethodHandles.lookup();
            var cfgCls = XposedHelpers.findClass("com.android.server.SystemConfig", lpparam.classLoader);
            var cfgGetInstance = XposedHelpers.findMethodExact(cfgCls, "getInstance");
            var cfgRemoveFeature = XposedHelpers.findMethodExact(cfgCls, "removeFeature", String.class);
            final var getSystemConfig = lookup.unreflect(cfgGetInstance);
            final var removeFeature = lookup.unreflect(cfgRemoveFeature);
            var pmsClass = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpparam.classLoader);
            var pmsMain = Arrays.stream(pmsClass.getDeclaredMethods()).filter(x -> x.getName().equals("main")).findFirst().get();
            XposedBridge.hookMethod(pmsMain, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    XposedBridge.log("after PackageManagerService.main");
                    var cfg = (Object)getSystemConfig.invoke();
                    removeFeature.invoke(cfg, "cn.google.services");
                    removeFeature.invoke(cfg, "com.google.android.feature.services_updater");
                }
            });
        }
    }
}
