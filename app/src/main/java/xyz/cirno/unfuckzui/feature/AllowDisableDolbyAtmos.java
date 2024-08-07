package xyz.cirno.unfuckzui.feature;

import android.os.Build;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class AllowDisableDolbyAtmos {
    public static final String FEATURE_NAME = "allow_disable_dax";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.settings", "com.android.systemui"}, AllowDisableDolbyAtmos::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("com.android.settings".equals(lpparam.packageName)) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
                XposedHelpers.findAndHookMethod("com.android.settings.dolby.DolbyAtmosPreferenceFragment", lpparam.classLoader, "getheadsetStatus", XC_MethodReplacement.returnConstant(1));
            } else if (Build.VERSION.SDK_INT == 34) {
                XposedHelpers.findAndHookMethod("com.lenovo.settings.sound.dolby.DolbyAtmosFragment", lpparam.classLoader, "isHeadsetConnected", XC_MethodReplacement.returnConstant(Boolean.TRUE));
            }
        } else if ("com.android.systemui".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.android.systemui.qs.tiles.QDolbyAtmosTile", lpparam.classLoader, "isHeadSetConnect", XC_MethodReplacement.returnConstant(Boolean.TRUE));
            XposedHelpers.findAndHookMethod("com.android.systemui.qs.tiles.QDolbyAtmosDetailView", lpparam.classLoader, "isHeadSetConnect", XC_MethodReplacement.returnConstant(Boolean.TRUE));
        }
    }
}
