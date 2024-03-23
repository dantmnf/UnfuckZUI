package xyz.cirno.unfuckzui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.unfuckzui.feature.AllowDisableDolbyAtmos;
import xyz.cirno.unfuckzui.feature.DisableForceStop;
import xyz.cirno.unfuckzui.feature.DisableTaskbar;
import xyz.cirno.unfuckzui.feature.PackageInstallerHook;
import xyz.cirno.unfuckzui.feature.PermissionControllerHook;
import xyz.cirno.unfuckzui.feature.SafeCenterHook;
import xyz.cirno.unfuckzui.feature.UnfuckNotificationIcon;

public class FeatureRegistry {
    public static class Feature {
        public final String key;
        public final String[] hook_scope;
        private final IHandleLoadPackage loader;

        public interface IHandleLoadPackage {
            public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
        }
        public Feature(String key, String[] hook_scope, IHandleLoadPackage loadPackageMethod) {
            this.key = key;
            this.hook_scope = hook_scope;
            this.loader = loadPackageMethod;
        }

        public Feature(String key, String[] hook_scope, IHandleLoadPackage loadPackageMethod, String[] reload_scope) {
            this.key = key;
            this.hook_scope = hook_scope;
            this.loader = loadPackageMethod;
        }

        public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
            try {
                loader.handleLoadPackage(lpparam);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }
    }

    public static class DynamicFeature extends Feature {
        public final String[] static_reload_scope;

        public DynamicFeature(String key, String[] hook_scope, IHandleLoadPackage loadPackageMethod, String[] static_reload_scope) {
            super(key, hook_scope, loadPackageMethod);
            this.static_reload_scope = static_reload_scope;
        }

        public DynamicFeature(String key, String[] hook_scope, IHandleLoadPackage loadPackageMethod) {
            super(key, hook_scope, loadPackageMethod);
            this.static_reload_scope = null;
        }
    }

    public static final Feature[] FEATURES = new Feature[] {
            AllowDisableDolbyAtmos.FEATURE,
            DisableForceStop.FEATURE,
            DisableTaskbar.FEATURE,
            PackageInstallerHook.FEATURE,
            PermissionControllerHook.FEATURE,
            SafeCenterHook.FEATURE,
            UnfuckNotificationIcon.FEATURE,
    };

    private static final Map<String, Feature> featureMap;

    static {
        featureMap = new HashMap<>();
        for (var feature : FEATURES) {
            featureMap.put(feature.key, feature);
        }
    }

    public static Feature getFeature(String key) {
        return featureMap.get(key);
    }

}
