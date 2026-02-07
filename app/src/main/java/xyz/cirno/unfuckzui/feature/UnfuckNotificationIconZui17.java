package xyz.cirno.unfuckzui.feature;

import android.content.Context;
import android.os.Build;
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

public class UnfuckNotificationIconZui17 {
    public static final String FEATURE_NAME = "honor_notification_smallicon";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.systemui"}, UnfuckNotificationIconZui17::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT < 35) {
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

        // use grayscale icons for notification shelf (collapsed notification icons)
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationShelf", lpparam.classLoader, "updateResources$5", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                isCtsMode.set(true);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                isCtsMode.remove();
            }
        });

        // don't replace the small icon with app icon
        if (Build.VERSION.SDK_INT == 35) {
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationListener", lpparam.classLoader, "replaceTheSmallIcon", StatusBarNotification.class, XC_MethodReplacement.returnConstant(null));
        } else if (Build.VERSION.SDK_INT == 36) {
            XposedHelpers.findAndHookMethod("com.android.systemui.util.QSUtil", lpparam.classLoader, "replaceTheSmallIcon", Context.class, StatusBarNotification.class, XC_MethodReplacement.returnConstant(null));
        }

        final var notificationHeaderViewWrapper_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper", lpparam.classLoader);
        final var notificationHeaderViewWrapper_mIcon = XposedHelpers.findField(notificationHeaderViewWrapper_class, "mIcon");
        final var getIcon = MethodHandles.lookup().unreflectGetter(notificationHeaderViewWrapper_mIcon);
        final var expandableNotificationRow_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(notificationHeaderViewWrapper_class, "onContentUpdated", expandableNotificationRow_class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var iconview = (ImageView) getIcon.invoke(param.thisObject);
                final int KEY_SIZE_UNFUCKED = 1145141919;
                if (Objects.equals(iconview.getTag(KEY_SIZE_UNFUCKED), Boolean.TRUE)) {
                    return;
                }
                var lp = iconview.getLayoutParams();
                if (lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                    // AOSP notification_icon_circle_size: 24dp
                    var dm = iconview.getContext().getResources().getDisplayMetrics();
                    var diameter = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
                    lp.width = Math.round(diameter);
                    lp.height = Math.round(diameter);
                    if (lp instanceof ViewGroup.MarginLayoutParams mlp) {
                        mlp.setMarginStart(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, dm)));
                    }
                    iconview.requestLayout();
                }
                iconview.setTag(KEY_SIZE_UNFUCKED, Boolean.TRUE);
            }
        });

        if (Build.VERSION.SDK_INT == 35) {
            // always true for ROW
            XposedHelpers.findAndHookMethod("com.android.systemui.notificationlist.view.NotificationHeaderView", lpparam.classLoader, "shouldShowIconBackground", XC_MethodReplacement.returnConstant(true));
            // always use circle template for android.app.Notification$Builder#get*Resource()
            XposedHelpers.findAndHookMethod("android.app.Notification$Builder", lpparam.classLoader, "isCtsGtsTest", XC_MethodReplacement.returnConstant(true));
        }
    }
}
