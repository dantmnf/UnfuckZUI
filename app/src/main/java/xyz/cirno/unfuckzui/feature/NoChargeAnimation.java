package xyz.cirno.unfuckzui.feature;

import android.os.Build;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class NoChargeAnimation {
    public static final String FEATURE_NAME = "no_charge_animation";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.systemui"}, NoChargeAnimation::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        new NoChargeAnimation().handleLoadSystemUi(lpparam);
    }

    public void handleLoadSystemUi(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        var classLoader = lpparam.classLoader;
        var ChargingAnimationControllerClass = classLoader.loadClass("com.android.keyguard.lockscreen.charge.ChargingAnimationController");
        if (Build.VERSION.SDK_INT >= 35) {
            // handleShow inlined
            var handlerField = XposedHelpers.findField(ChargingAnimationControllerClass, "H");
            XposedHelpers.findAndHookMethod(handlerField.getType(), "handleMessage", Message.class, XC_MethodReplacement.DO_NOTHING);
        } else {
            XposedHelpers.findAndHookMethod(ChargingAnimationControllerClass, "handleShow", int.class, int.class, long.class, XC_MethodReplacement.DO_NOTHING);
        }
    }
}
