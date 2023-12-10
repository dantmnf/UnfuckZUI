package xyz.cirno.unfuckzui.hooks;

import de.robv.android.xposed.XC_MethodHook;

public final class ReturnTrueHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        param.setResult(Boolean.TRUE);
    }
}
