package xyz.cirno.unfuckzui;

import android.app.IActivityManager;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;


public class ShizukuUserService extends IShizukuUserService.Stub {
    public ShizukuUserService() {
    }

    @Override
    public void destroy() throws RemoteException {
        System.exit(0);
    }

    @Override
    public void killPackageProcess(String[] packages) throws RemoteException {
        // systemui can't be force-stopped
        var am = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
        var packagesSet = new HashSet<>(Arrays.asList(packages));
        packagesSet.remove("android");
        var snapshot = am.getRunningAppProcesses();
        var pids = new ArrayList<Integer>();

        if (snapshot != null) {
            for (var info : snapshot) {
                for (var p : info.pkgList) {
                    if (packagesSet.contains(p)) {
                        pids.add(info.pid);
                    }
                }
            }
            for (var pid : pids) {
                Process.killProcess(pid);
            }
        }
    }
}