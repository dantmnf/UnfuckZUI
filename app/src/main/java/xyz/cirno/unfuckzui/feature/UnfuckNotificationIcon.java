package xyz.cirno.unfuckzui.feature;

import android.animation.ArgbEvaluator;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.internal.util.ContrastColorUtil;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class UnfuckNotificationIcon {
    public static final String FEATURE_NAME = "honor_notification_smallicon";
    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.android.systemui"}, UnfuckNotificationIcon::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT == 35) {
            return;
        }
        new UnfuckNotificationIcon().handleLoadSystemUi(lpparam);
    }

    private Context systemUiContext;
    private PackageManager pm;
    private final ThreadLocal<Boolean> isCtsMode = ThreadLocal.withInitial(() -> null);

    private Context getSystemUiContext() {
        if (systemUiContext == null) {
            try {
                systemUiContext = AndroidAppHelper.currentApplication();
            } catch (Throwable ignored) {}
        }
        return systemUiContext;
    }

    private boolean isDark() {
        return isDark(getSystemUiContext());
    }

    private boolean isDark(Context ctx) {
        var isDark = (ctx.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        return isDark;
    }

    private int getSystemAccentColor() {
        return getSystemUiContext().getColor(android.R.color.system_accent1_500);
    }


    public void handleLoadSystemUi(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {



        XposedHelpers.findAndHookMethod(android.app.Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                systemUiContext = (Context) param.thisObject;
                pm = systemUiContext.getPackageManager();
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.util.XSystemUtil", lpparam.classLoader, "isCTSGTSTest", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var mode = isCtsMode.get();
                if (mode != null) {
                    param.setResult(Boolean.TRUE.equals(mode));
                }
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader, "onDarkChanged", java.util.ArrayList.class, float.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                isCtsMode.set(Boolean.FALSE);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                isCtsMode.set(null);
            }
        });
        XposedHelpers.findAndHookConstructor("com.android.systemui.statusbar.phone.DarkIconDispatcherImpl", lpparam.classLoader, android.content.Context.class, "com.android.systemui.statusbar.phone.LightBarTransitionsController$Factory", "com.android.systemui.dump.DumpManager", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setIntField(param.thisObject, "mDarkModeIconColorSingleTone", 0xdf000000);
                XposedHelpers.setIntField(param.thisObject, "mDarkModeIconColorSingleToneCts", 0xdf000000);
                XposedHelpers.setIntField(param.thisObject, "mLightModeIconColorSingleTone", 0xffffffff);
            }
        });
        var lookup = MethodHandles.lookup();
        final var clsStatusBarIconView = XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader);
        final var setDrawableColor = lookup.unreflectSetter(XposedHelpers.findField(clsStatusBarIconView, "mDrawableColor"));
        final var setIconColor = lookup.unreflectSetter(XposedHelpers.findField(clsStatusBarIconView, "mIconColor"));
        var method_setColorInternal = clsStatusBarIconView.getDeclaredMethod("setColorInternal", int.class);
        method_setColorInternal.setAccessible(true);
        final var mh_setColorInternal = lookup.unreflect(method_setColorInternal);
        var method_updateContrastedStaticColor = clsStatusBarIconView.getDeclaredMethod("updateContrastedStaticColor");
        method_updateContrastedStaticColor.setAccessible(true);
        final var mh_updateContrastedStaticColor = lookup.unreflect(method_updateContrastedStaticColor);
        var field_mDozer = XposedHelpers.findField(clsStatusBarIconView, "mDozer");
        final var getDozer = lookup.unreflectGetter(field_mDozer);
        var method_dozer_setColor = field_mDozer.getType().getDeclaredMethod("setColor", int.class);
        method_dozer_setColor.setAccessible(true);
        final var mh_dozer_setColor = lookup.unreflect(method_dozer_setColor);

        XposedHelpers.findAndHookMethod(clsStatusBarIconView, "setStaticDrawableColor", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                var color = (int) param.args[0];
                var thiz = param.thisObject;
                setDrawableColor.invoke(thiz, color);
                mh_setColorInternal.invoke(thiz, color);
                mh_updateContrastedStaticColor.invoke(thiz);
                setIconColor.invoke(thiz, color);
                var mDozer = (Object) getDozer.invoke(thiz);
                if (mDozer != null) {
                    mh_dozer_setColor.invoke(mDozer, color);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationListener", lpparam.classLoader, "replaceTheSmallIcon", android.service.notification.StatusBarNotification.class, XC_MethodReplacement.returnConstant(null));

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconAreaController", lpparam.classLoader, "generateIconLayoutParams", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var lp = (FrameLayout.LayoutParams) param.getResult();
                var ctx = getSystemUiContext();
                var m = ctx.getResources().getDisplayMetrics();
                var h = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, m);
                var p = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, m);
                lp.height = (int) h;
                lp.width += (int)(p*2);
            }
        });

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader, "clearStatusBarIcon", XC_MethodReplacement.returnConstant(null));
        } else if (Build.VERSION.SDK_INT == 34) {
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.ZuiCoreImpl", lpparam.classLoader, "clearStatusBarIcon", XC_MethodReplacement.returnConstant(null));
        }

        final var notificationHeaderViewWrapper_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper", lpparam.classLoader);
        final var notificationHeaderViewWrapper_mIcon = XposedHelpers.findField(notificationHeaderViewWrapper_class, "mIcon");
        final var expandableNotificationRow_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader);
        final var notificationRow_getEntry = XposedHelpers.findMethodExact(expandableNotificationRow_class, "getEntry");
        final var notificationEntry_getSbn = XposedHelpers.findMethodExact(notificationRow_getEntry.getReturnType(), "getSbn");
        final var cachingIconView_setOriginalIconColor = XposedHelpers.findMethodExact(Class.forName("com.android.internal.widget.CachingIconView"), "setOriginalIconColor", int.class);
        final var cachingIconView_setBackgroundColor = XposedHelpers.findMethodExact(Class.forName("com.android.internal.widget.CachingIconView"), "setBackgroundColor", int.class);
        XposedHelpers.findAndHookMethod(notificationHeaderViewWrapper_class, "onContentUpdated", expandableNotificationRow_class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var row = param.args[0];
                var entry = notificationRow_getEntry.invoke(row);
                var sbn = (StatusBarNotification) notificationEntry_getSbn.invoke(entry);
                var iconview = (ImageView) notificationHeaderViewWrapper_mIcon.get(param.thisObject);
                final int KEY_BACKGROUND_UNFUCKED = 1145141919;
                final float scale = 24.0f / 34.0f;
                if (!Objects.equals(iconview.getTag(KEY_BACKGROUND_UNFUCKED), Boolean.TRUE)) {
                    var d = new ShapeDrawable();
                    d.setShape(new OvalShape());
                    d.getPaint().setColor(0xFF333333);
                    iconview.setBackground(d);
                    var lp = iconview.getLayoutParams();
                    if (lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                        lp.width = Math.round(lp.width * scale);
                        lp.height = Math.round(lp.height * scale);
                        if (lp instanceof ViewGroup.MarginLayoutParams mlp) {
                            mlp.setMarginStart(mlp.getMarginStart() + Math.round(lp.width * ((1.0f - scale) * 0.75f)));
                        }
                    }
                    iconview.requestLayout();
                    iconview.setTag(KEY_BACKGROUND_UNFUCKED, Boolean.TRUE);
                }

                var isDark = isDark();
                var origcolor = sbn.getNotification().color;
                int bgcolor, fgcolor;
                if (origcolor == 0 || origcolor == 1) {
                    bgcolor = getSystemAccentColor();
                } else {
                    bgcolor = origcolor;
                }
                fgcolor = ContrastColorUtil.resolveContrastColor(getSystemUiContext(), 0, bgcolor, !isDark);
                cachingIconView_setBackgroundColor.invoke(iconview, fgcolor);
                cachingIconView_setOriginalIconColor.invoke(iconview, bgcolor);
//                Log.d("UnfuckZUI", String.format("NotificationHeaderViewWrapper onContentUpdated sbn.getNotification().color=%08X bgcolor=%08X fgcolor=%08X", origcolor, bgcolor, fgcolor));


            }
        });


        final var notificationShelf_class = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationShelf", lpparam.classLoader);
        final var notificationShelf_mShelfIcons = XposedHelpers.findField(notificationShelf_class, "mShelfIcons");
        final var notificationIconContainer_class = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader);
        final var notificationIconContainer_setInNotificationIconShelf = XposedHelpers.findMethodExact(notificationIconContainer_class, "setInNotificationIconShelf", boolean.class);
        final var notificationIconContainer_mContext = XposedHelpers.findField(notificationIconContainer_class, "mContext");
        final var mThemedTextColorPrimary = XposedHelpers.findField(notificationIconContainer_class, "mThemedTextColorPrimary");

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            XposedHelpers.findAndHookMethod(notificationShelf_class, "initDimens", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var mShelfIcons = notificationShelf_mShelfIcons.get(param.thisObject);
                    notificationIconContainer_setInNotificationIconShelf.invoke(mShelfIcons, true);
                }
            });
        }

        XposedHelpers.findAndHookMethod(notificationIconContainer_class, Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU ? "initDimens" : "initResources", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var context = (Context) notificationIconContainer_mContext.get(param.thisObject);
                final Context themedContext = new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_DayNight);
                try (var attrs = themedContext.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary})) {
                    var color = attrs.getColorStateList(0).getDefaultColor();
//                    Log.d("UnfuckZUI", String.format("notificationIconContainer initDimens mThemedTextColorPrimary=%08x", color));
                    mThemedTextColorPrimary.setInt(param.thisObject, color);
                }
            }
        });

        final var notificationInfo_class = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.NotificationInfo", lpparam.classLoader);
        final var notificationInfo_mPkgIcon = XposedHelpers.findField(notificationInfo_class, "mPkgIcon");
        final var notificationInfo_mPackageName = XposedHelpers.findField(notificationInfo_class, "mPackageName");
        final var com_android_systemui_r$id_pkg_icon = XposedHelpers.getStaticIntField(XposedHelpers.findClass("com.android.systemui.R$id", lpparam.classLoader), "pkg_icon");
        XposedHelpers.findAndHookMethod(notificationInfo_class, "bindHeader", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var mPackageName = (String) notificationInfo_mPackageName.get(param.thisObject);
                android.graphics.drawable.Drawable mPkgIcon = null;
                try {
                    var info = pm.getApplicationInfo(
                            mPackageName,
                            PackageManager.MATCH_UNINSTALLED_PACKAGES
                                    | PackageManager.MATCH_DISABLED_COMPONENTS
                                    | PackageManager.MATCH_DIRECT_BOOT_UNAWARE
                                    | PackageManager.MATCH_DIRECT_BOOT_AWARE);
                    if (info != null) {
                        mPkgIcon = pm.getApplicationIcon(info);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // app is gone, just show package name and generic icon
                    mPkgIcon = pm.getDefaultActivityIcon();
                }
                var view = (android.view.View) param.thisObject;
                var icon = (ImageView) view.findViewById(com_android_systemui_r$id_pkg_icon);
                icon.setImageDrawable(mPkgIcon);
                notificationInfo_mPkgIcon.set(param.thisObject, mPkgIcon);
            }
        });

        var argbEvaluator = new ArgbEvaluator();

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconAreaController", lpparam.classLoader, "onDarkChanged", java.util.ArrayList.class, float.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("NotificationIconAreaController", "onDarkChanged mIconTint=" + XposedHelpers.getIntField(param.thisObject, "mIconTint"));
                var darkIntensity = (float) param.args[1];
                var mIconTint = argbEvaluator.evaluate(darkIntensity, 0xffffffff, 0xb2000000);
                param.args[2] = mIconTint;
            }
        });
    }
}
