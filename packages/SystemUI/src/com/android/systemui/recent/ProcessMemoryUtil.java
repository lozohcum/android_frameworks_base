
package com.android.systemui.recent;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.ProcessErrorStateInfo;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.TaskThumbnails;
import android.app.ActivityManagerNative;
import android.app.ApplicationErrorReport.CrashInfo;
import android.app.IActivityController;
import android.app.IApplicationThread;
import android.app.IInstrumentationWatcher;
import android.app.IProcessObserver;
import android.app.IServiceConnection;
import android.app.IThumbnailReceiver;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.StrictMode.ViolationInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProcessMemoryUtil {
    public Context context;
    private ActivityManagerNative am;

    final int INDEX_FIRST = -1;
    final int INDEX_USER = INDEX_FIRST + 1;
    final int INDEX_PID = INDEX_FIRST + 2;
    final int INDEX_PPID = INDEX_FIRST + 3;
    final int INDEX_VSIZE = INDEX_FIRST + 4;
    final int INDEX_RSS = INDEX_FIRST + 5;
    final int INDEX_WCHAN = INDEX_FIRST + 6;
    final int INDEX_PC = INDEX_FIRST + 7;
    final int INDEX_STATUS = INDEX_FIRST + 8;
    final int INDEX_NAME = INDEX_FIRST + 9;
    final int Length_ProcStat = 9;

    private static String[] defaultIgnoreList;
    static {
        String[] arrayString = new String[16];
        arrayString[0] = "com.android.mms";
        arrayString[1] = "com.android.bluetooth";
        arrayString[2] = "com.android.phone";
        arrayString[3] = "com.android.launcher";
        arrayString[4] = "com.android.systemui";
        arrayString[5] = "com.android.providers.im";
        arrayString[6] = "com.android.htcdialer";
        arrayString[7] = "com.android.alarmclock";
        arrayString[8] = "com.nuance.android.vsuite.vsuiteapp";
        arrayString[9] = "com.spritemobile.backup.semc2";
        arrayString[10] = "com.motorola.usb";
        arrayString[11] = "android.process.acore";
        arrayString[12] = "com.android.deskclock";
        arrayString[13] = "com.shendu.launcher";
        arrayString[14] = "com.andrew.apollo";
        arrayString[15] = "com.shendu.weather";
        defaultIgnoreList = arrayString;
    }

    private List<String[]> PMUList = null;

    // ----------------------- Methods for memory -----------------------

    public long getTotalMemory() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            str2 = localBufferedReader.readLine();// Get the frist line of
                                                  // meminfo, "Total Memory"
            arrayOfString = str2.split("\\s+")[1].split("\\s");
            initial_memory = Integer.valueOf(arrayOfString[0]).intValue() * 1024;
            localBufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return initial_memory;

    }

    public long getAvailMemory(Context context) {
        ActivityManager activityManger = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo minfo = new ActivityManager.MemoryInfo();
        activityManger.getMemoryInfo(minfo);
        return minfo.availMem;
    }

    // --------------------- Methods for memory end ---------------------

    // ------------------- Methods for process information -------------------

    public void initPMUtil() {
        PMUList = new ArrayList<String[]>();
        String resultString = getProcessRunningInfo();
        parseProcessRunningInfo(resultString);
    }

    public ProcessMemoryUtil(Context context) {
        this.context = context;
        initPMUtil();
        am = new ActivityManagerNative() {

            @Override
            public boolean willActivityBeVisible(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void addToWhiteList(String packageName) throws RemoteException {

            }

            @Override
            public void wakingUp() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void updatePersistentConfiguration(Configuration values)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void updateConfiguration(Configuration values) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void unstableProviderDied(IBinder connection) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void unregisterReceiver(IIntentReceiver receiver) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void unregisterProcessObserver(IProcessObserver observer)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void unhandledBack() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void unbroadcastIntent(IApplicationThread caller, Intent intent,
                    int userId)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean unbindService(IServiceConnection connection)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void unbindFinished(IBinder token, Intent service, boolean doRebind)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void unbindBackupAgent(ApplicationInfo appInfo) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean testIsSystemReady() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean targetTaskAffinityMatchesActivity(IBinder token,
                    String destAffinity)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean switchUser(int userid) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean stopServiceToken(ComponentName className, IBinder token,
                    int startId)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public int stopService(IApplicationThread caller, Intent service,
                    String resolvedType)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void stopAppSwitches() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public ComponentName startService(IApplicationThread caller, Intent service,
                    String resolvedType)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void startRunning(String pkg, String cls, String action, String data)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean startNextMatchingActivity(IBinder callingActivity,
                    Intent intent, Bundle options)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean startInstrumentation(ComponentName className,
                    String profileFile, int flags,
                    Bundle arguments, IInstrumentationWatcher watcher)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public int startActivityWithConfig(IApplicationThread caller, Intent intent,
                    String resolvedType, IBinder resultTo, String resultWho,
                    int requestCode,
                    int startFlags, Configuration newConfig, Bundle options)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int startActivityIntentSender(IApplicationThread caller,
                    IntentSender intent,
                    Intent fillInIntent, String resolvedType, IBinder resultTo,
                    String resultWho,
                    int requestCode, int flagsMask, int flagsValues, Bundle options)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int startActivityInPackage(int uid, Intent intent, String resolvedType,
                    IBinder resultTo, String resultWho, int requestCode, int startFlags,
                    Bundle options)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public WaitResult startActivityAndWait(IApplicationThread caller,
                    Intent intent,
                    String resolvedType, IBinder resultTo, String resultWho,
                    int requestCode, int flags,
                    String profileFile, ParcelFileDescriptor profileFd, Bundle options)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int startActivity(IApplicationThread caller, Intent intent,
                    String resolvedType,
                    IBinder resultTo, String resultWho, int requestCode, int flags,
                    String profileFile,
                    ParcelFileDescriptor profileFd, Bundle options) throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int startActivitiesInPackage(int uid, Intent[] intents,
                    String[] resolvedTypes,
                    IBinder resultTo, Bundle options) throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int startActivities(IApplicationThread caller, Intent[] intents,
                    String[] resolvedTypes,
                    IBinder resultTo, Bundle options) throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void signalPersistentProcesses(int signal) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean shutdown(int timeout) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void showWaitingForDebugger(IApplicationThread who, boolean waiting)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void showBootMessage(CharSequence msg, boolean always)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setServiceForeground(ComponentName className, IBinder token,
                    int id,
                    Notification notification, boolean keepNotification)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setRequestedOrientation(IBinder token, int requestedOrientation)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setProcessLimit(int max) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setProcessForeground(IBinder token, int pid, boolean isForeground)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setPackageScreenCompatMode(String packageName, int mode)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setPackageAskScreenCompat(String packageName, boolean ask)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setLockScreenShown(boolean shown) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setImmersive(IBinder token, boolean immersive)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setFrontActivityScreenCompatMode(int mode) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setDebugApp(String packageName, boolean waitForDebugger,
                    boolean persistent)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setAlwaysFinish(boolean enabled) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setActivityController(IActivityController watcher)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void serviceDoneExecuting(IBinder token, int type, int startId, int res)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void revokeUriPermissionFromOwner(IBinder owner, Uri uri, int mode)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void revokeUriPermission(IApplicationThread caller, Uri uri, int mode)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void resumeAppSwitches() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void reportThumbnail(IBinder token, Bitmap thumbnail,
                    CharSequence description)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean removeTask(int taskId, int flags) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean removeSubTask(int taskId, int subTaskIndex)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void removeContentProviderExternal(String name, IBinder token)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void removeContentProvider(IBinder connection, boolean stable)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public Intent registerReceiver(IApplicationThread caller, String callerPackage,
                    IIntentReceiver receiver, IntentFilter filter, String requiredPermission)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void registerProcessObserver(IProcessObserver observer)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean refContentProvider(IBinder connection, int stableDelta,
                    int unstableDelta)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void publishService(IBinder token, Intent intent, IBinder service)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void publishContentProviders(IApplicationThread caller,
                    List<ContentProviderHolder> providers) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean profileControl(String process, boolean start, String path,
                    ParcelFileDescriptor fd, int profileType) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public IBinder peekService(Intent service, String resolvedType)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void overridePendingTransition(IBinder token, String packageName,
                    int enterAnim,
                    int exitAnim) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public ParcelFileDescriptor openContentUri(Uri uri) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void noteWakeupAlarm(IIntentSender sender) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public IBinder newUriPermissionOwner(String name) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean navigateUpTo(IBinder token, Intent target, int resultCode,
                    Intent resultData)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void moveTaskToFront(int task, int flags, Bundle options)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void moveTaskToBack(int task) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void moveTaskBackwards(int task) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean killProcessesBelowForeground(String reason)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean killPids(int[] pids, String reason, boolean secure)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void killBackgroundProcesses(String packageName) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void killApplicationWithUid(String pkg, int uid) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void killApplicationProcess(String processName, int uid)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void killAllBackgroundProcesses() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isUserAMonkey() throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isTopActivityImmersive() throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isIntentSenderTargetedToPackage(IIntentSender sender)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isIntentSenderAnActivity(IIntentSender sender)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isImmersive(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean handleApplicationWtf(IBinder app, String tag, CrashInfo crashInfo)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void handleApplicationStrictModeViolation(IBinder app,
                    int violationMask,
                    ViolationInfo crashInfo) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void handleApplicationCrash(IBinder app, CrashInfo crashInfo)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void grantUriPermissionFromOwner(IBinder owner, int fromUid,
                    String targetPkg, Uri uri,
                    int mode) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void grantUriPermission(IApplicationThread caller, String targetPkg,
                    Uri uri, int mode)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void goingToSleep() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public int getUidForIntentSender(IIntentSender sender) throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public List getTasks(int maxNum, int flags, IThumbnailReceiver receiver)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public TaskThumbnails getTaskThumbnails(int taskId) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getTaskForActivity(IBinder token, boolean onlyRoot)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public List getServices(int maxNum, int flags) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public PendingIntent getRunningServiceControlPanel(ComponentName service)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<ApplicationInfo> getRunningExternalApplications()
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<RunningAppProcessInfo> getRunningAppProcesses()
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getRequestedOrientation(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public List<RecentTaskInfo> getRecentTasks(int maxNum, int flags)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getProviderMimeType(Uri uri) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<ProcessErrorStateInfo> getProcessesInErrorState()
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long[] getProcessPss(int[] pids) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.os.Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getProcessLimit() throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getPackageScreenCompatMode(String packageName)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getPackageForToken(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getPackageForIntentSender(IIntentSender sender)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean getPackageAskScreenCompat(String packageName)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void getMyMemoryState(RunningAppProcessInfo outInfo)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void getMemoryInfo(MemoryInfo outInfo) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public int getLaunchedFromUid(IBinder activityToken) throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public IIntentSender getIntentSender(int type, String packageName,
                    IBinder token,
                    String resultWho, int requestCode, Intent[] intents,
                    String[] resolvedTypes, int flags,
                    Bundle options) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getFrontActivityScreenCompatMode() throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public ConfigurationInfo getDeviceConfigurationInfo() throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public UserInfo getCurrentUser() throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ContentProviderHolder getContentProviderExternal(String name,
                    IBinder token)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ContentProviderHolder getContentProvider(IApplicationThread caller,
                    String name,
                    boolean stable) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Configuration getConfiguration() throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getCallingPackage(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ComponentName getCallingActivity(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ComponentName getActivityClassForToken(IBinder token)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void forceStopPackage(String packageName) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void finishSubActivity(IBinder token, String resultWho, int requestCode)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void finishReceiver(IBinder who, int resultCode, String resultData,
                    Bundle map,
                    boolean abortBroadcast) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void finishInstrumentation(IApplicationThread target, int resultCode,
                    Bundle results)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void finishHeavyWeightApp() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean finishActivityAffinity(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean finishActivity(IBinder token, int code, Intent data)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void enterSafeMode() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean dumpHeap(String process, boolean managed, String path,
                    ParcelFileDescriptor fd)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void dismissKeyguardOnNextActivity() throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void crashApplication(int uid, int initialPid, String packageName,
                    String message)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void closeSystemDialogs(String reason) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean clearApplicationUserData(String packageName,
                    IPackageDataObserver observer,
                    int userId) throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public int checkUriPermission(Uri uri, int pid, int uid, int mode)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int checkPermission(String permission, int pid, int uid)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int checkGrantUriPermission(int callingUid, String targetPkg, Uri uri,
                    int modeFlags)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void cancelIntentSender(IIntentSender sender) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public int broadcastIntent(IApplicationThread caller, Intent intent,
                    String resolvedType,
                    IIntentReceiver resultTo, int resultCode, String resultData,
                    Bundle map,
                    String requiredPermission, boolean serialized, boolean sticky,
                    int userId)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int bindService(IApplicationThread caller, IBinder token,
                    Intent service,
                    String resolvedType, IServiceConnection connection, int flags,
                    int userId)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean bindBackupAgent(ApplicationInfo appInfo, int backupRestoreMode)
                    throws RemoteException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void backupAgentCreated(String packageName, IBinder agent)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void attachApplication(IApplicationThread app) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void activityStopped(IBinder token, Bundle state, Bitmap thumbnail,
                    CharSequence description) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void activitySlept(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void activityPaused(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void activityIdle(IBinder token, Configuration config,
                    boolean stopProfiling)
                    throws RemoteException {
                // TODO Auto-generated method stub

            }

            @Override
            public void activityDestroyed(IBinder token) throws RemoteException {
                // TODO Auto-generated method stub

            }
        };
    }

    private String getProcessRunningInfo() {
        String result = null;
        CMDExecute cmdexe = new CMDExecute();
        try {
            String[] args = {
                    "/system/bin/ps"
            };
            result = cmdexe.run(args, "/system/bin/");
        } catch (IOException ex) {
            Log.i("fetch_process_info", "ex=" + ex.toString());
        }
        return result;
    }

    private int parseProcessRunningInfo(String infoString) {
        String tempString = "";
        boolean bIsProcInfo = false;

        String[] rows = null;
        String[] columns = null;
        rows = infoString.split("[\n]+");

        for (int i = 0; i < rows.length; i++) {
            tempString = rows[i];
            if (tempString.indexOf("PID") == -1) {
                if (bIsProcInfo == true) {
                    tempString = tempString.trim();
                    columns = tempString.split("[ ]+");
                    if (columns.length == Length_ProcStat) {
                        PMUList.add(columns);
                    }
                }
            } else {
                bIsProcInfo = true;
            }
        }

        return PMUList.size();
    }

    public String getMemInfoByPid(int pid) {
        if (pid == -1) {
            return "-";
        }

        ActivityManager mActivityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        int[] myMempid = new int[] {
                pid
        };
        Debug.MemoryInfo[] memoryInfo = mActivityManager
                .getProcessMemoryInfo(myMempid);
        int memSize = memoryInfo[0].getTotalPss();

        return String.valueOf(memSize);
    }

    public int getPidByName(String procName) {
        int pid = -1;

        String tempString = "";
        for (Iterator<String[]> iterator = PMUList.iterator(); iterator.hasNext();) {
            String[] item = (String[]) iterator.next();
            tempString = item[INDEX_NAME];
            if (tempString != null &&
                    (tempString.equals(procName) || tempString.startsWith(procName))) {
                pid = Integer.valueOf(item[INDEX_PID]);
                break;
            }
        }

        return pid;
    }

    public int getProcessesCount() {
        ActivityManager activityManager;

        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> procList = activityManager.getRunningAppProcesses();

        return procList.size();
    }

    // ----------------- Methods for process information end -----------------

    // -------------------- Methods for killing processes --------------------
    public synchronized boolean killProcessByPkg(String packageName) {
        Class<?> c;
        boolean flag = true;
        // The default ignore list, system process, will not kill
        if (!isDefaultIgnoreList(packageName)) {
            try {
                am.forceStopPackage(packageName);
            } catch (Exception e) {
                e.printStackTrace();
                flag = false;
            }
        } else {
            flag = false;
        }

        return flag;
    }

    public void cleanCache() {
        ActivityManager activityManager;

        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> procList = activityManager.getRunningAppProcesses();

        for (Iterator<RunningAppProcessInfo> iterator = procList.iterator(); iterator.hasNext();) {
            try {
                RunningAppProcessInfo procInfo = iterator.next();

                String strProcName = procInfo.processName;
                if (strProcName.indexOf(":") > 0)
                    strProcName = strProcName.split(":")[0];

                if (!strProcName.equals("system")
                        && !isDefaultIgnoreList(strProcName)
                        && procInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                    killProcessByPkg(strProcName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void killAllProcess() {
        ActivityManager activityManager;

        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> procList = activityManager.getRunningAppProcesses();

        for (Iterator<RunningAppProcessInfo> iterator = procList.iterator(); iterator.hasNext();) {
            try {
                RunningAppProcessInfo procInfo = iterator.next();

                String strProcName = procInfo.processName;
                if (strProcName.indexOf(":") > 0)
                    strProcName = strProcName.split(":")[0];

                if (!strProcName.equals("system")
                        && !isDefaultIgnoreList(strProcName)
                        && procInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                    killProcessByPkg(strProcName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isDefaultIgnoreList(String packageName) {
        boolean flag = false;
        for (int i = 0; i < defaultIgnoreList.length; i++) {
            if (defaultIgnoreList[i].equals(packageName)) {
                flag = true;
            }
        }
        return flag;
    }

    // ------------------ Methods for killing processes end ------------------

    public static class CMDExecute {

        public synchronized String run(String[] cmd, String workdirectory) throws IOException {
            String result = "";
            try {
                ProcessBuilder builder = new ProcessBuilder(cmd);
                InputStream in = null;
                if (workdirectory != null) {
                    builder.directory(new File(workdirectory));
                    builder.redirectErrorStream(true);
                    Process process = builder.start();
                    in = process.getInputStream();
                    byte[] re = new byte[1024];
                    while (in.read(re) != -1)
                        result = result + new String(re);
                }
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return result;
        }
    }
}
