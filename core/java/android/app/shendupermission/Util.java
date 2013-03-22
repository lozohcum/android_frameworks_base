/*******************************************************************************
 * Copyright (c) 2011 Adam Shanks (ChainsDD)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package android.app.shendupermission;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

public class Util {
    private static final String TAG = "Su.Util";

    public static final int MALICIOUS_NOT = 0;
    public static final int MALICIOUS_UID = 1;
    public static final int MALICIOUS_RESPOND = 2;
    public static final int MALICIOUS_PROVIDER_WRITE = 3;

    public static String getAppName(Context c, int uid, boolean withUid) {
        PackageManager pm = c.getPackageManager();
        String appName = "Unknown";
        String[] packages = pm.getPackagesForUid(uid);

        if (packages != null) {
            try {
                if (packages.length == 1) {
                    appName = pm.getApplicationLabel(pm.getApplicationInfo(packages[0], 0))
                            .toString();
                } else if (packages.length > 1) {
                    appName = "";
                    for (int i = 0; i < packages.length; i++) {
                        appName += packages[i];
                        if (i < packages.length - 1) {
                            appName += ", ";
                        }
                    }
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package name not found", e);
            }
        } else {
            Log.e(TAG, "Package not found for uid " + uid);
        }

        if (withUid) {
            appName += " (" + uid + ")";
        }

        return appName;
    }

    public static String getAppPackage(Context c, int uid) {
        PackageManager pm = c.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        String appPackage = "unknown";

        if (packages != null) {
            if (packages.length == 1) {
                appPackage = packages[0];
            } else if (packages.length > 1) {
                appPackage = "";
                for (int i = 0; i < packages.length; i++) {
                    appPackage += packages[i];
                    if (i < packages.length - 1) {
                        appPackage += ", ";
                    }
                }
            }
        } else {
            Log.e(TAG, "Package not found");
        }

        return appPackage;
    }

    public static String formatDate(Context context, long date) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String format = prefs.getString("pref_date_format", "default");
        if (format.equals("default")) {
            return DateFormat.getDateFormat(context).format(date);
        } else {
            return (String) DateFormat.format(format, date);
        }
    }

    public static String formatTime(Context context, long time) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hour24 = prefs.getBoolean("pref_24_hour_format", true);
        boolean showSeconds = prefs.getBoolean("pref_show_seconds", false);
        String hour = "kk";
        String min = "mm";
        String sec = ":ss";
        String post = "";

        if (hour24) {
            hour = "kk";
        } else {
            hour = "hh";
            post = "aa";
        }

        if (showSeconds) {
            sec = ":ss";
        } else {
            sec = "";
        }

        String format = String.format("%s:%s%s%s", hour, min, sec, post);
        return (String) DateFormat.format(format, time);
    }

    public static String formatDateTime(Context context, long date) {
        return formatDate(context, date) + " " + formatTime(context, date);
    }

    public static List<String> findMaliciousPackages(Context context) {
        List<String> maliciousApps = new ArrayList<String>();
        List<PackageInfo> installedApps = context.getPackageManager()
                .getInstalledPackages(PackageManager.GET_PERMISSIONS);

        for (PackageInfo pkg : installedApps) {
            int result = isPackageMalicious(context, pkg);
            if (result != 0) {
                maliciousApps.add(pkg.packageName + ":" + result);
            }
        }
        return maliciousApps;
    }

    public static int isPackageMalicious(Context context, PackageInfo packageInfo) {
        // If the package being checked is this one, it's not malicious
        if (packageInfo.packageName.equals(context.getPackageName())) {
            return MALICIOUS_NOT;
        }

        // If the package being checked shares a UID with Superuser, it's
        // probably malicious
        if (packageInfo.applicationInfo.uid == context.getApplicationInfo().uid) {
            return MALICIOUS_UID;
        }

        // Finally we check for any permissions that other apps should not have.
        if (packageInfo.requestedPermissions != null) {
            String[] bannedPermissions = new String[] {
                    "com.noshufou.android.su.RESPOND",
                    "com.noshufou.android.su.provider.WRITE"
            };
            for (String s : packageInfo.requestedPermissions) {
                for (int i = 0; i < 2; i++) {
                    if (s.equals(bannedPermissions[i]) &&
                            context.getPackageManager().
                                    checkPermission(bannedPermissions[i], packageInfo.packageName)
                                == PackageManager.PERMISSION_GRANTED) {
                        return i + 2;
                    }
                }
            }
        }

        return MALICIOUS_NOT;
    }

    public static boolean isSystemApp(String name, Context context) {
        List<ApplicationInfo> packs = context.getPackageManager()
                .getInstalledApplications(0);
        ArrayList<String> appList = new ArrayList<String>();

        for (ApplicationInfo appInfo : packs) {
            boolean flag = false;
            if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flag = true;
            } else if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                flag = true;
            }
            if (flag) {
                appList.add(appInfo.packageName);
            }
        }
        if (appList.indexOf(name) == -1)
            return false;
        return true;
    }

}
