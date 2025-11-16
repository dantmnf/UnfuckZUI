package xyz.cirno.unfuckzui.feature;

import android.annotation.SuppressLint;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class DisableGameHelperPopup {
    public static final String FEATURE_NAME = "disable_game_helper_popup";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.game.service"}, DisableGameHelperPopup::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
       if ("com.zui.game.service".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.zui.game.service.ui.FloatingGameNoticController", lpparam.classLoader, "show", XC_MethodReplacement.DO_NOTHING);
        }
    }
}
