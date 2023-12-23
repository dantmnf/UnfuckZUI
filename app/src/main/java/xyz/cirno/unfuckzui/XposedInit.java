package xyz.cirno.unfuckzui;

import android.annotation.SuppressLint;
import android.os.SystemProperties;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
public class XposedInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("".equals(SystemProperties.get("ro.com.zui.version", ""))) {
            XposedBridge.log("Not ZUI ROM");
            return;
        }
        if ("com.zui.launcher".equals(lpparam.packageName)) {
            new LauncherHook().handleLoadPackage(lpparam);
        } else if ("com.android.packageinstaller".equals(lpparam.packageName)) {
            PackageInstallerHook.handleZuiPackageInstaller(lpparam);
        } else if ("com.android.systemui".equals(lpparam.packageName)) {
            new SystemUIHook().handleLoadSystemUi(lpparam);
        } else if ("com.android.permissioncontroller".equals(lpparam.packageName)) {
            PermissionControllerHook.handleLoadPackage(lpparam);
        } else if ("com.zui.safecenter".equals(lpparam.packageName)) {
            SafeCenterHook.handleLoadPackage(lpparam);
        } else if ("com.android.settings".equals(lpparam.packageName)) {
            SettingsHook.handleLoadPackage(lpparam);
        }
    }

//    @Override
//    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
//        if ("com.android.systemui".equals(resparam.packageName)) {
//            SystemUIHook.handleLoadResource(resparam);
//        }
//    }
}
