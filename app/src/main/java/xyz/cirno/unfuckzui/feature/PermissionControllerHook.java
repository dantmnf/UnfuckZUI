package xyz.cirno.unfuckzui.feature;

import android.app.Activity;
import android.view.Window;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class PermissionControllerHook {
    public static final String FEATURE_NAME = "permission_controller_style";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.permissioncontroller"}, PermissionControllerHook::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("com.android.permissioncontroller.extra.ZuiUtils", lpparam.classLoader, "isCTSandGTS", java.lang.String.class, XC_MethodReplacement.returnConstant(Boolean.TRUE));
        XposedHelpers.findAndHookMethod("com.android.permissioncontroller.permission.ui.GrantPermissionsActivity", lpparam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var activity = (Activity) param.thisObject;
                activity.setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
                activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
                var rootView = activity.getWindow().getDecorView();
                rootView.setFilterTouchesWhenObscured(true);
                rootView.setPadding(0, 0, 0, 0);
            }
        });
    }
}
