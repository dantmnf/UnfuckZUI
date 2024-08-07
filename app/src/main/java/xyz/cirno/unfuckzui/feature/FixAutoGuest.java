package xyz.cirno.unfuckzui.feature;

import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class FixAutoGuest {
    public static final String FEATURE_NAME = "fix_auto_guest";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.systemui"}, FixAutoGuest::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.android.systemui".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.android.systemui.user.domain.interactor.GuestUserInteractor", lpparam.classLoader, "isDeviceAllowedToAddGuest", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    var context = (android.content.Context) XposedHelpers.getObjectField(param.thisObject, "applicationContext");
                    if (Settings.Global.getInt(context.getContentResolver(), "user_switcher_enabled", 0) == 0) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            });
        }
    }
}
