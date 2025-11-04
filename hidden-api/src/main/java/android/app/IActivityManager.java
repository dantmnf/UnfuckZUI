package android.app;

import android.content.pm.IPackageManager;
import android.os.IBinder;

import java.util.List;

public interface IActivityManager extends android.os.IInterface {
    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses();
    void forceStopPackage(String packageName, int userId);
    boolean killPids(int[] pids, String reason, boolean secure);

    public static abstract class Stub extends android.os.Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder x) {
            throw new RuntimeException("stub");
        }
    }
}
