package xyz.cirno.unfuckzui;

import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemServerHook {
    private IPackageManager pm;
    private int zuiLauncherUid = 0;

    private static final String ZUI_LAUNCHER_PACKAGE = "com.zui.launcher";

    private IPackageManager getPackageManager() {
        if (pm == null) {
            pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return pm;
    }

    private boolean isZuiLauncherUid(int uid) {
        if (zuiLauncherUid == 0) {
            var packageName = getPackageManager().getNameForUid(uid);
            if (ZUI_LAUNCHER_PACKAGE.equals(packageName)) {
                zuiLauncherUid = uid;
            }
        }
        return zuiLauncherUid == uid;
    }

    private class IgnoreBinderCallFromZuiLauncherHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            var uid = Binder.getCallingUid();
            if (isZuiLauncherUid(uid)) {
                Log.d("UnfuckZUI", "Ignoring " + param.method.getName() + " call from " + ZUI_LAUNCHER_PACKAGE);
                param.setResult(null);
            }
        }
    }

    private static void checkSystemProperty(String key, String value) {
        if (!value.equals(SystemProperties.get(key, ""))) {
            SystemProperties.set(key, value);
        }
    }

    void handleSystemServer(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        checkSystemProperty("persist.sys.ctsmode", "true");
        checkSystemProperty("persist.sys.ctsmode.set", "2147483647000");
        checkSystemProperty("persist.sys.lenovo_setup_privacy", "true");
        checkSystemProperty("persist.sys.lenovo.is_test_mode", "true");
        final var amsClass = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(amsClass, "forceStopPackage", String.class, int.class, new IgnoreBinderCallFromZuiLauncherHook());
        XposedHelpers.findAndHookMethod(amsClass, "killBackgroundProcesses", String.class, int.class, new IgnoreBinderCallFromZuiLauncherHook());
    }
}
