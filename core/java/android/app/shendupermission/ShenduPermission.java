
package android.app.shendupermission;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import android.util.Slog;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.provider.ContactsContract;
import android.net.Uri;
import android.util.Log;
import android.content.pm.PackageManager;
import android.os.IBinder;
import com.android.internal.telephony.IPhoneSubInfo;
import android.os.Binder;
import android.os.Process;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.content.IIntentReceiver;
import android.content.pm.IPackageManager;
import android.content.pm.ProviderInfo;
import android.os.RemoteException;
import android.os.Bundle;

public class ShenduPermission {
    private Context mContext;
    private static String TAG = "ShenduPermission";
    private static boolean DEBUG = true;

    private static Map<String, PidAllow> mRecord = new HashMap<String, PidAllow>();

    private static class PidAllow {
        int mPid = 0;
        //boolean mAllow = false;
    }

    private static List<String> needCheckPermission = new ArrayList<String>();
    static {
        needCheckPermission.add("android.permission.READ_CONTACTS");
        needCheckPermission.add("android.permission.READ_SMS");
        needCheckPermission.add("android.permission.RECEIVE_SMS");
        needCheckPermission.add("android.permission.SEND_SMS");
        needCheckPermission.add("android.permission.WRITE_SMS");
        needCheckPermission.add("android.permission.RECEIVE_MMS");
        needCheckPermission.add("android.permission.READ_PHONE_STATE");
        // needCheckPermission.add("android.permission.ACCESS_COARSE_LOCATION");
        // needCheckPermission.add("android.permission.ACCESS_FINE_LOCATION");
        // needCheckPermission.add("android.permission.COARSE_LOCATION");
    };

    private static class IntentReceiver extends IIntentReceiver.Stub {
        private boolean mFinished = false;

        public synchronized void performReceive(
                Intent intent, int rc, String data, Bundle ext, boolean ord,
                boolean sticky) {
            String line = "Broadcast completed: result=" + rc;
            if (data != null)
                line = line + ", data=\"" + data + "\"";
            if (ext != null)
                line = line + ", extras: " + ext;
            System.out.println(line);
            mFinished = true;
            notifyAll();
        }

        public synchronized void waitForFinish() {
            try {
                while (!mFinished)
                    wait();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public ShenduPermission(Context con) {
        mContext = con;
    }

    public static boolean isNeedCheckPermission(String perm) {
        return needCheckPermission.contains(perm);
    }

    synchronized public static void sendMessagePerm(final String permission) {
        final int pid = Binder.getCallingPid();
        final int uid = Binder.getCallingUid();
        if (uid < 10000) {
            return;
        }
        if (!isNeedCheckPermission(permission)) {
            return;
        }

        // read app name from proc
        final String app_name;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
                    "/proc/" + pid + "/cmdline")));
            int ch;
            StringBuilder sb = new StringBuilder();
            while (Character.isLetter((char) (ch = br.read())) || (char) ch == '.') {
                sb.append((char) ch);
            }
            app_name = sb.toString();
            br.close();
        } catch (java.io.FileNotFoundException e) {
            return;
        } catch (java.io.IOException e) {
            return;
        }
        // verify if this is first or not
        if (mRecord.containsKey(app_name)) {
            PidAllow pa = mRecord.get(app_name);
            if (pa.mPid == pid) {
                return;
            } else {
                pa.mPid = pid;
            }
        } else {
            PidAllow pa = new PidAllow();
            pa.mPid = pid;
            mRecord.put(app_name, pa);
        }

        // send a broadcast to tell a permission being denyed
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (DEBUG) {
                        Slog.i(TAG, "broadcastIntent desired_perm " + permission);
                    }
                    Intent intent = new Intent();
                    intent.setAction("com.shendu.permission.PERMREQUEST");
                    intent.putExtra("app_name", app_name);
                    intent.putExtra("caller_uid", uid);
                    intent.putExtra("desired_perm", permission);
                    intent.putExtra("caller_pid", pid);
                    IActivityManager am = ActivityManagerNative.getDefault();
                    IntentReceiver receiver = new IntentReceiver();
                    am.broadcastIntent(null, intent, null, receiver, 0, null, null, null, true,
                            false,
                            0);
                    receiver.waitForFinish();
                } catch (android.os.RemoteException ex) {
                    // ignore it
                }
                return;
            }
        }).start();
    }
}
