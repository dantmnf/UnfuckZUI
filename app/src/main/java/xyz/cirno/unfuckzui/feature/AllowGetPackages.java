package xyz.cirno.unfuckzui.feature;

import android.provider.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class AllowGetPackages {
    public static final String FEATURE_NAME = "allow_get_packages";

    private static final int OP_GET_INSTALLED_APP = 214;

    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"android"}, AllowGetPackages::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.processName)) {
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "opToDefaultMode", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    int op = (int) param.args[0];
                    if (op == OP_GET_INSTALLED_APP) {
                        param.setResult(0);
                    }
                }
            });

            XposedHelpers.findAndHookMethod("com.android.server.appop.AppOpsService", lpparam.classLoader, "checkOperationRawZui", int.class, int.class, "java.lang.String", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    int op = (int) param.args[0];
                    if (op == OP_GET_INSTALLED_APP) {
                        param.setResult(0);
                    }
                }
            });
        }
    }
}
