package xyz.cirno.unfuckzui.feature;

import java.lang.invoke.MethodHandles;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class EnableAutorunByDefault {
    public static final String FEATURE_NAME = "default_enable_autorun";

    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.safecenter"}, EnableAutorunByDefault::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.zui.safecenter".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.lenovo.performance.autorun.utils.AutoRunWhiteList", lpparam.classLoader, "checkType", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var result = (int) param.getResult();
                    if (result == 0) {
                        param.setResult(2);
                    }
                }
            });
        }
    }
}
