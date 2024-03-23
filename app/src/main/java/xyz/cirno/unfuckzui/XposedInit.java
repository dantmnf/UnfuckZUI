package xyz.cirno.unfuckzui;

import android.annotation.SuppressLint;
import android.os.SystemProperties;

import java.util.Arrays;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.feature.AllowDisableDolbyAtmos;
import xyz.cirno.unfuckzui.feature.DisableForceStop;
import xyz.cirno.unfuckzui.feature.DisableTaskbar;
import xyz.cirno.unfuckzui.feature.PackageInstallerHook;
import xyz.cirno.unfuckzui.feature.PermissionControllerHook;
import xyz.cirno.unfuckzui.feature.SafeCenterHook;
import xyz.cirno.unfuckzui.feature.UnfuckNotificationIcon;

@SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
public class XposedInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("".equals(SystemProperties.get("ro.com.zui.version", ""))) {
            XposedBridge.log("Not ZUI ROM");
            return;
        }
        for (var feature : FeatureRegistry.FEATURES) {
            for (var scope : feature.hook_scope) {
                if (Objects.equals(scope, lpparam.packageName)) {
                    // read XSharedPreferences as late as possible
                    if ((feature instanceof FeatureRegistry.DynamicFeature) || FeatureControl.getInstance().isFeatureEnabled(feature.key)) {
                        feature.handleLoadPackage(lpparam);
                    }
                }
            }
        }
    }

}
