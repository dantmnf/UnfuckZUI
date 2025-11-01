package xyz.cirno.unfuckzui.feature;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class SafeCenterHook {
    public static final String FEATURE_NAME = "disable_virus_scan";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.safecenter"}, SafeCenterHook::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("tmsdk.fg.creator.ManagerCreatorF", lpparam.classLoader, "getManager", java.lang.Class.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.lenovo.safecenter.antivirus.external.AntiVirusInterface", lpparam.classLoader, "initTMSApplication", android.content.Context.class, boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.lenovo.safecenter.antivirus.tmsdbupdate.UpdateTMSVDBReceiver", lpparam.classLoader, "onReceive", "android.content.Context", "android.content.Intent", XC_MethodReplacement.returnConstant(null));
    }
}
