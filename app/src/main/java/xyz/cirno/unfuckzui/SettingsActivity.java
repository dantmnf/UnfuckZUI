package xyz.cirno.unfuckzui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rikka.shizuku.Shizuku;

/**
 * @noinspection deprecation
 */
@SuppressLint("WorldReadableFiles")
public class SettingsActivity extends Activity {
    private static boolean moduleEnabled = false;

    private final Set<String> featuresSnapshot = new HashSet<>();
    private final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, ShizukuUserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);
    private SharedPreferences sp;
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    private static String escapeShellArg(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            reloadSettings();
        } else {
            showNoPermissionDialog();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        try {
            sp = getSharedPreferences("feature_config", MODE_WORLD_READABLE);
            snapshotFeatures();
            moduleEnabled = true;
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to open shared preferences", e);
            Toast.makeText(this, R.string.message_module_not_enabled, Toast.LENGTH_SHORT).show();
        }

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        var item = menu.add(0, 114514, 0, R.string.action_reload_settings);
        item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 114514) {
            reloadSettings();
        }
        return super.onOptionsItemSelected(item);
    }

    private void snapshotFeatures() {
        featuresSnapshot.clear();
        for (var feature : FeatureRegistry.FEATURES) {
            if (sp.getBoolean(feature.key, false)) {
                featuresSnapshot.add(feature.key);
            }
        }
    }

    private void showNoPermissionDialog() {
        var dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.no_permission_reload_title)
                .setMessage(R.string.no_permission_reload_message)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                .setCancelable(true)
                .create();
        dlg.show();
    }

    private ShizukuStatus checkShizuku() {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            return ShizukuStatus.Unavailable;
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            return ShizukuStatus.Available;
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Users choose "Deny and don't ask again"
            return ShizukuStatus.Unavailable;
        } else {
            // Request the permission
            Shizuku.requestPermission(0);
            return ShizukuStatus.Pending;
        }
    }

    private void reloadSettings() {
        var packagesToReload = new ArrayList<String>();
        if (sp == null) {
            Toast.makeText(this, R.string.message_module_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        sp.edit().commit();
        for (var feature : FeatureRegistry.FEATURES) {
            var oldState = featuresSnapshot.contains(feature.key);
            var newState = sp.getBoolean(feature.key, false);
            if (oldState == newState) {
                continue;
            }
            if (feature instanceof FeatureRegistry.DynamicFeature df) {
                if (df.static_reload_scope != null) {
                    packagesToReload.addAll(Arrays.asList(df.static_reload_scope));
                }
            } else {
                packagesToReload.addAll(Arrays.asList(feature.hook_scope));
            }
        }

        if (packagesToReload.isEmpty()) {
            Toast.makeText(this, R.string.message_no_apps_to_reload, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = stopPackagesWithSu(packagesToReload);

        if (!success) {
            // su unavailable
            var shizukuStatus = checkShizuku();
            if (shizukuStatus == ShizukuStatus.Available) {
                success = stopPackagesWithShizuku(packagesToReload);
            } else if (shizukuStatus == ShizukuStatus.Pending) {
                // handle in callback
                return;
            }
        }

        if (success) {
            snapshotFeatures();
        } else {
            showNoPermissionDialog();
        }
    }

    private boolean stopPackagesWithSu(List<String> packages) {
        try {
            var proc = Runtime.getRuntime().exec(new String[]{"sh", "-c", "command -v su"});
            var exit = proc.waitFor();
            if (exit != 0) {
                return false;
            }
            proc = Runtime.getRuntime().exec("su");
            var stdin = proc.getOutputStream();
            for (var pkg : packages) {
                if ("android".equals(pkg)) {
                    continue;
                } else {
                    stdin.write(("killall " + escapeShellArg(pkg) + "\n").getBytes());
                }
            }
            stdin.write("exit 0\n".getBytes());
            stdin.close();
            proc.waitFor();
        } catch (Exception e) {
            Toast.makeText(this, R.string.message_su_invocation_failed, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, R.string.message_manual_reload, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean stopPackagesWithShizuku(List<String> packagesToReload) {
        if (Shizuku.getUid() != 0) {
            return false;
        }
        Shizuku.bindUserService(userServiceArgs, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                if (binder != null && binder.pingBinder()) {
                    var service = IShizukuUserService.Stub.asInterface(binder);
                    try {
                        service.killPackageProcess(packagesToReload.toArray(new String[0]));
                    } catch (RemoteException ignore) {
                    }
                    Shizuku.unbindUserService(userServiceArgs, this, true);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        });
        return true;
    }

    private enum ShizukuStatus {Unavailable, Available, Pending}

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            var preferenceManager = getPreferenceManager();
            var activity = (SettingsActivity) getActivity();
            if (moduleEnabled) {
                preferenceManager.setSharedPreferencesName("feature_config");
                preferenceManager.setSharedPreferencesMode(MODE_WORLD_READABLE);
            } else {
                preferenceManager.setPreferenceDataStore(new DummyPreferenceStore());
            }

            addPreferencesFromResource(R.xml.root_preferences);

            if (!moduleEnabled) {
                findPreference("category_appearance").setEnabled(false);
                findPreference("category_behavior").setEnabled(false);
                findPreference("category_cn").setEnabled(false);
            }

            var launcherIconPreference = (SwitchPreference) findPreference("show_launcher_icon");
            launcherIconPreference.setChecked(getContext().getPackageManager().getComponentEnabledSetting(new ComponentName(getContext(), MainActivity.class)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            launcherIconPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                var enabled = (boolean) newValue;
                getContext().getPackageManager().setComponentEnabledSetting(new ComponentName(getContext(), MainActivity.class), enabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                return true;
            });

            findPreference("version").setSummary(BuildConfig.VERSION_NAME);
            findPreference("rom_region").setSummary(SystemProperties.get("ro.config.zui.region", "N/A"));
        }
    }
}