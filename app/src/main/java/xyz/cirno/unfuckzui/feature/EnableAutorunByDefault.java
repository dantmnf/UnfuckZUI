package xyz.cirno.unfuckzui.feature;

import java.lang.invoke.MethodHandles;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.FeatureRegistry;

public class EnableAutorunByDefault {
    public static final String FEATURE_NAME = "default_enable_autorun";

    private static final int ATTR_WHITELIST = 0x20000000;
    private static final int ATTR_RELATIVE_WHITELIST = 0x40000000;

    public static final FeatureRegistry.Feature FEATURE = new FeatureRegistry.Feature(FEATURE_NAME, new String[] {"com.zui.safecenter"}, EnableAutorunByDefault::handleLoadPackage);
    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.zui.safecenter".equals(lpparam.packageName)) {
            var cls = XposedHelpers.findClass("com.lenovo.performance.autorun.beans.AutoRunDbItem", lpparam.classLoader);
            var fld = XposedHelpers.findField(cls, "mAttrs");
            fld.setAccessible(true);
            var getter = MethodHandles.lookup().unreflectGetter(fld);
            var setter = MethodHandles.lookup().unreflectSetter(fld);
            XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var attrs = (int)getter.invoke(param.thisObject);
                    attrs |= ATTR_WHITELIST | ATTR_RELATIVE_WHITELIST;
                    setter.invoke(param.thisObject, attrs);
                }
            });
        }
    }
}
