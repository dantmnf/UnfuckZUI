package xyz.cirno.unfuckzui.feature;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
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
                isCtsMode.remove();
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationListener", lpparam.classLoader, "replaceTheSmallIcon", StatusBarNotification.class, XC_MethodReplacement.returnConstant(null));

        final var notificationHeaderViewWrapper_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper", lpparam.classLoader);
        final var notificationHeaderViewWrapper_mIcon = XposedHelpers.findField(notificationHeaderViewWrapper_class, "mIcon");
        final var getIcon = MethodHandles.lookup().unreflectGetter(notificationHeaderViewWrapper_mIcon);
        final var expandableNotificationRow_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader);
        final var notificationRowIconView_class = Class.forName("com.android.internal.widget.NotificationRowIconView");
        XposedHelpers.findAndHookMethod(notificationHeaderViewWrapper_class, "onContentUpdated", expandableNotificationRow_class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var iconview = (ImageView) getIcon.invoke(param.thisObject);
                final int KEY_SIZE_UNFUCKED = 1145141919;
                if (!Objects.equals(iconview.getTag(KEY_SIZE_UNFUCKED), Boolean.TRUE)) {
                    var lp = iconview.getLayoutParams();
                    var dm = iconview.getContext().getResources().getDisplayMetrics();
                    // AOSP notification_icon_circle_size: 24dp
                    var diameter = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
                    lp.width = Math.round(diameter);
                    lp.height = Math.round(diameter);
                    if (lp instanceof ViewGroup.MarginLayoutParams mlp) {
                        mlp.setMarginStart(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, dm)));
                    }
                    if (iconview.getClass() != notificationRowIconView_class) {
                        // notifications with no circle template
                        // apply android:padding="@dimen/notification_icon_circle_padding"
                        var padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm));
                        iconview.setPadding(padding, padding, padding, padding);
                        // apply android:background="@drawable/notification_icon_circle"
                        var d = new ShapeDrawable();
                        d.setShape(new OvalShape());
                        d.getPaint().setColor(0xFF333333);
                        iconview.setBackground(d);
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

        XposedHelpers.findAndHookMethod("com.android.systemui.notificationlist.view.NotificationHeaderView", lpparam.classLoader, "shouldShowIconBackground", XC_MethodReplacement.returnConstant(true));
    }
}
