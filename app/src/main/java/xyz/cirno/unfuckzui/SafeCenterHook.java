package xyz.cirno.unfuckzui;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.hooks.ReturnNullHook;

public class SafeCenterHook {
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("tmsdk.fg.creator.ManagerCreatorF", lpparam.classLoader, "getManager", java.lang.Class.class, new ReturnNullHook());
        XposedHelpers.findAndHookMethod("com.lenovo.safecenter.antivirus.external.AntiVirusInterface", lpparam.classLoader, "initTMSApplication", android.content.Context.class, boolean.class, new ReturnNullHook());
    }
}
