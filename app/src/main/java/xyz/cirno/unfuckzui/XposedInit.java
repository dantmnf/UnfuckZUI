package xyz.cirno.unfuckzui;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.os.SystemProperties;
import android.provider.Settings;

import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

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
                        try {
                            feature.handleLoadPackage(lpparam);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                }
            }
        }
    }
}
