package xyz.cirno.unfuckzui.hooks;

import de.robv.android.xposed.XC_MethodHook;

public final class ReturnNullHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        param.setResult(null);
    }
}
