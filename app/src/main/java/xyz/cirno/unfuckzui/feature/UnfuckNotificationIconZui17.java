package xyz.cirno.unfuckzui.feature;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class UnfuckNotificationIconZui17 {
    public static final String FEATURE_NAME = "honor_notification_smallicon";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.systemui"}, UnfuckNotificationIconZui17::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT != 35) {
            return;
        }
        new UnfuckNotificationIconZui17().handleLoadSystemUi(lpparam);
    }

    private final ThreadLocal<Boolean> isCtsMode = ThreadLocal.withInitial(() -> null);

    public void handleLoadSystemUi(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
                XposedHelpers.findAndHookMethod("com.android.systemui.util.XSystemUtil", lpparam.classLoader, "isCTSGTSTest", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var mode = isCtsMode.get();
                if (mode != null) {
                    param.setResult(mode);
                }
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationShelf", lpparam.classLoader, "updateResources$5", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                isCtsMode.set(true);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                isCtsMode.set(false);
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationListener", lpparam.classLoader, "replaceTheSmallIcon", StatusBarNotification.class, XC_MethodReplacement.returnConstant(null));


        final var notificationHeaderViewWrapper_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper", lpparam.classLoader);
        final var notificationHeaderViewWrapper_mIcon = XposedHelpers.findField(notificationHeaderViewWrapper_class, "mIcon");
        final var expandableNotificationRow_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader);
//        final var notificationRow_mEntry = XposedHelpers.findField(expandableNotificationRow_class, "mEntry");
//        final var notificationRow_getEntry = MethodHandles.lookup().unreflectGetter(notificationRow_mEntry);
//        final var notificationEntry_getSbn = XposedHelpers.findMethodExact(notificationRow_mEntry.getType(), "getSbn");
        XposedHelpers.findAndHookMethod(notificationHeaderViewWrapper_class, "onContentUpdated", expandableNotificationRow_class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var row = param.args[0];

                var iconview = (ImageView) notificationHeaderViewWrapper_mIcon.get(param.thisObject);
                final int KEY_SIZE_UNFUCKED = 1145141919;
                final float scale = 24.0f / 34.0f;
                if (!Objects.equals(iconview.getTag(KEY_SIZE_UNFUCKED), Boolean.TRUE)) {
                    var lp = iconview.getLayoutParams();
                    lp.width = Math.round(lp.width * scale);
                    lp.height = Math.round(lp.height * scale);
                    if (lp instanceof ViewGroup.MarginLayoutParams mlp) {
                        mlp.setMarginStart(mlp.getMarginStart() + Math.round(lp.width * ((1.0f - scale) * 0.75f)));
                    }
                    iconview.requestLayout();
                    iconview.setTag(KEY_SIZE_UNFUCKED, Boolean.TRUE);
                }
            }
        });

        XposedHelpers.findAndHookConstructor("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader, "android.content.Context", "android.util.AttributeSet", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "mOverrideIconColor", false);
            }
        });

    }
}
