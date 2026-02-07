package xyz.cirno.unfuckzui.feature;

import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class DisableForceStop {
    public static final String FEATURE_NAME = "disable_force_stop";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.launcher"}, DisableForceStop::handleLoadPackage);

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT <= 35) {
            var amwclass = XposedHelpers.findClass("com.android.systemui.shared.system.ActivityManagerWrapper", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(amwclass, "removeAllRunningAppProcesses", android.content.Context.class, java.util.ArrayList.class, XC_MethodReplacement.returnConstant(null));
            XposedHelpers.findAndHookMethod(amwclass, "removeAppProcess", android.content.Context.class, int.class, String.class, int.class, XC_MethodReplacement.returnConstant(null));
        } else {
            var cls = XposedHelpers.findClass("com.zui.launcher.util.OverviewUtilities", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(cls, "removeAllRunningAppProcesses", android.content.Context.class, java.util.ArrayList.class, boolean.class, XC_MethodReplacement.returnConstant(null));
            XposedHelpers.findAndHookMethod(cls, "removeAppProcess", android.content.Context.class, int.class, String.class, int.class, XC_MethodReplacement.returnConstant(null));
        }

    }
}
