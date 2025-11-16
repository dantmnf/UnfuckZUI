package xyz.cirno.unfuckzui.feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class NoSettingsSplit {
    public static final String FEATURE_NAME = "adjust_settings_split";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.settings"}, NoSettingsSplit::handleLoadPackage);

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod("com.android.settings.activityembedding.ActivityEmbeddingUtils", lpparam.classLoader, "getMinCurrentScreenSplitWidthDp", "android.content.Context", XC_MethodReplacement.returnConstant(960));
    }
}
