package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;

/** Uses only framework classes so it can run independently of the instrumented application's dex. */
public class UnprivilegedClientService extends Service {
    private static final String PLUGIN = "io.github.supermonster003.autojs6.plugin.three.shell.terminal";
    private final Binder control = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code != FIRST_CALL_TRANSACTION || reply == null) return false;
            ServiceConnection connection = new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service) { }
                public void onServiceDisconnected(ComponentName name) { }
            };
            boolean denied = false;
            try {
                Intent intent = new Intent().setComponent(new ComponentName(PLUGIN, PLUGIN + ".ThreeShellTerminalPluginService"));
                boolean bound = bindService(intent, connection, Context.BIND_AUTO_CREATE);
            } catch (SecurityException expected) {
                denied = true;
            } finally {
                // Android may allocate its ServiceDispatcher before the permission check fails.
                try { unbindService(connection); } catch (IllegalArgumentException ignored) { }
            }
            reply.writeInt(Process.myUid());
            reply.writeInt(checkSelfPermission("org.autojs.permission.PLUGIN") == PackageManager.PERMISSION_DENIED ? 1 : 0);
            reply.writeInt(denied ? 1 : 0);
            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) { return control; }
}
