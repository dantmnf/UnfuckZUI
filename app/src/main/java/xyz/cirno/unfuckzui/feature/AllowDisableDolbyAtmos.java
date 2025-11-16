package xyz.cirno.unfuckzui.feature;

import android.annotation.SuppressLint;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class AllowDisableDolbyAtmos {
    public static final String FEATURE_NAME = "allow_disable_dax";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.settings", "com.android.systemui", "com.zui.game.service"}, AllowDisableDolbyAtmos::handleLoadPackage);
    @SuppressLint("ObsoleteSdkInt")
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("com.android.settings".equals(lpparam.packageName)) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
                XposedHelpers.findAndHookMethod("com.android.settings.dolby.DolbyAtmosPreferenceFragment", lpparam.classLoader, "getheadsetStatus", XC_MethodReplacement.returnConstant(1));
            } else if (Build.VERSION.SDK_INT == 34) {
                XposedHelpers.findAndHookMethod("com.lenovo.settings.sound.dolby.DolbyAtmosFragment", lpparam.classLoader, "isHeadsetConnected", XC_MethodReplacement.returnConstant(Boolean.TRUE));
                XposedHelpers.findAndHookMethod("com.lenovo.settings.sound.dolby.DolbyAtmosFragment", lpparam.classLoader, "initView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        var pref = XposedHelpers.getObjectField(param.thisObject, "mDolbySwitchPreference");
                        XposedHelpers.callMethod(pref, "setSummary", (Object)null);
                    }
                });
            } else if (Build.VERSION.SDK_INT == 35) {
                XposedHelpers.findAndHookMethod("com.lenovo.settings.sound.dolby.DolbyAtmosUtils", lpparam.classLoader, "isHeadsetConnected", android.content.Context.class, XC_MethodReplacement.returnConstant(Boolean.TRUE));
                XposedHelpers.findAndHookMethod("com.lenovo.settings.sound.dolby.DolbySwitchPreferenceController", lpparam.classLoader, "updateState", "androidx.preference.Preference", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        var pref = param.args[0];
                        XposedHelpers.callMethod(pref, "setSummary", (Object)null);
                    }
                });
            }
        } else if ("com.android.systemui".equals(lpparam.packageName)) {
            if (Build.VERSION.SDK_INT <= 34) {
                XposedHelpers.findAndHookMethod("com.android.systemui.qs.tiles.QDolbyAtmosTile", lpparam.classLoader, "isHeadSetConnect", XC_MethodReplacement.returnConstant(Boolean.TRUE));
            } else {
                XposedHelpers.findAndHookMethod("com.android.systemui.qs.tiles.QDolbyAtmosTile", lpparam.classLoader, "isHeadSetConnect$2", XC_MethodReplacement.returnConstant(Boolean.TRUE));
            }
            XposedHelpers.findAndHookMethod("com.android.systemui.qs.tiles.QDolbyAtmosDetailView", lpparam.classLoader, "isHeadSetConnect", XC_MethodReplacement.returnConstant(Boolean.TRUE));
        } else if ("com.zui.game.service".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.zui.game.service.util.DolbyUtils", lpparam.classLoader, "handleDolbyGameSound", android.content.Context.class, int.class, XC_MethodReplacement.returnConstant(null));
//            XposedHelpers.findAndHookMethod("com.zui.game.service.ui.FloatingGameNoticController", lpparam.classLoader, "show", XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod("com.zui.util.SettingsValueUtilKt", lpparam.classLoader, "isHeadsetConnected", "android.content.Context", XC_MethodReplacement.returnConstant(true));
        }
    }
}
