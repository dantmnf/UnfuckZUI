package xyz.cirno.unfuckzui.feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class ChangeTaskTimeout {
    public static final String FEATURE_NAME = "change_task_timeout";

    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"android"}, ChangeTaskTimeout::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.processName)) {
            XposedHelpers.findAndHookMethod("com.android.server.wm.RecentTasks", lpparam.classLoader, "loadParametersFromResources", "android.content.res.Resources", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var that = param.thisObject;
                    // XposedBridge.log("original mMaxNumVisibleTasks: " + XposedHelpers.getIntField(that, "mMaxNumVisibleTasks"));
                    // XposedBridge.log("original mActiveTasksSessionDurationMs: " + XposedHelpers.getLongField(that, "mActiveTasksSessionDurationMs"));
                    XposedHelpers.setIntField(that, "mMaxNumVisibleTasks", -1);
                    XposedHelpers.setLongField(that, "mActiveTasksSessionDurationMs", 7*24*60*60*1000L); // 7 days
                }
            });
        }
    }
}
