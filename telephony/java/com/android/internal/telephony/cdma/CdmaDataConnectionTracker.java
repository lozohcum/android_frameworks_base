/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.cdma;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.net.LinkProperties.CompareResult;
import android.net.NetworkConfig;
import android.net.NetworkUtils;
import android.net.ProxyProperties;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import com.android.internal.telephony.ApnContext;
import com.android.internal.telephony.ApnSetting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.DataCallState;
import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.DataConnection.FailCause;
import com.android.internal.telephony.DataConnection.UpdateLinkPropertyResult;
import com.android.internal.telephony.DataConnectionAc;
import com.android.internal.telephony.DataConnectionTracker;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.util.AsyncChannel;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RetryManager;
import com.android.internal.util.AsyncChannel;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@hide}
 */
public final class CdmaDataConnectionTracker extends DataConnectionTracker {
    protected final String LOG_TAG = "CDMA";

    private CDMAPhone mCdmaPhone;

    /**
     * Handles changes to the APN db.
     */
    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver () {
            super(mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean selfChange) {
            sendMessage(obtainMessage(EVENT_APN_CHANGED));
        }
    }
    private CdmaSubscriptionSourceManager mCdmaSSM;

    /** The DataConnection being setup */
    private CdmaDataConnection mPendingDataConnection;
    private ContentResolver mResolver;
    private ArrayList<ApnSetting> waitingApns;
	private int waitingApnsPermanentFailureCountDown = 0;
    private static final String PROPERTY_OPERATOR_NUMERIC = "ro.cdma.home.operator.numeric";

    private boolean mPendingRestartRadio = false;
    private static final int TIME_DELAYED_TO_RESTART_RADIO =
            SystemProperties.getInt("ro.cdma.timetoradiorestart", 60000);

    /**
     * Pool size of CdmaDataConnection objects.
     */
    private static final int DATA_CONNECTION_POOL_SIZE = 1;

    private static final String INTENT_RECONNECT_ALARM =
        "com.android.internal.telephony.cdma-reconnect";

    private static final String INTENT_DATA_STALL_ALARM =
        "com.android.internal.telephony.cdma-data-stall";

    static final Uri PREFERAPN_NO_UPDATE_URI =
                        Uri.parse("content://telephony/carriers/preferapn_no_update");
    static final String APN_ID = "apn_id";
    private boolean canSetPreferApn = false;

    /**
     * Constants for the data connection activity:
     * physical link down/up
     */
     private static final int DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE = 0;
     private static final int DATA_CONNECTION_ACTIVE_PH_LINK_DOWN = 1;
     private static final int DATA_CONNECTION_ACTIVE_PH_LINK_UP = 2;

    private static final String[] mSupportedApnTypes = {
            Phone.APN_TYPE_DEFAULT,
            Phone.APN_TYPE_MMS,
            Phone.APN_TYPE_DUN,
            Phone.APN_TYPE_HIPRI };

    private static final String[] mDefaultApnTypes = {
            Phone.APN_TYPE_DEFAULT,
            Phone.APN_TYPE_MMS,
            Phone.APN_TYPE_HIPRI };

    private String[] mDunApnTypes = {
            Phone.APN_TYPE_DUN };

    private static final int mDefaultApnId = DataConnectionTracker.APN_DEFAULT_ID;

    /** Watches for changes to the APN db. */
    private ApnChangeObserver mApnObserver;

    /* Constructor */

    CdmaDataConnectionTracker(CDMAPhone p) {
        super(p);
        mCdmaPhone = p;

        p.mCM.registerForAvailable (this, EVENT_RADIO_AVAILABLE, null);
        p.mCM.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        p.mIccRecords.registerForRecordsLoaded(this, EVENT_RECORDS_LOADED, null);
        p.mCM.registerForDataNetworkStateChanged (this, EVENT_DATA_STATE_CHANGED, null);
        p.mCT.registerForVoiceCallEnded (this, EVENT_VOICE_CALL_ENDED, null);
        p.mCT.registerForVoiceCallStarted (this, EVENT_VOICE_CALL_STARTED, null);
        p.mSST.registerForDataConnectionAttached(this, EVENT_TRY_SETUP_DATA, null);
        p.mSST.registerForDataConnectionDetached(this, EVENT_CDMA_DATA_DETACHED, null);
        p.mSST.registerForRoamingOn(this, EVENT_ROAMING_ON, null);
        p.mSST.registerForRoamingOff(this, EVENT_ROAMING_OFF, null);
        p.mCM.registerForCdmaOtaProvision(this, EVENT_CDMA_OTA_PROVISION, null);
        mCdmaSSM = CdmaSubscriptionSourceManager.getInstance (p.getContext(), p.mCM, this,
                EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);

        mDataConnectionTracker = this;
        mResolver = mPhone.getContext().getContentResolver();

        mApnObserver = new ApnChangeObserver();
        p.getContext().getContentResolver().registerContentObserver(
                Telephony.Carriers.CONTENT_URI, true, mApnObserver);

        mApnContexts = new ConcurrentHashMap<String, ApnContext>();

        createAllDataConnectionList();
        broadcastMessenger();

        Context c = mCdmaPhone.getContext();
        String[] t = c.getResources().getStringArray(
                com.android.internal.R.array.config_cdma_dun_supported_types);
        if (t != null && t.length > 0) {
            ArrayList<String> temp = new ArrayList<String>();
            for(int i=0; i< t.length; i++) {
                if (!Phone.APN_TYPE_DUN.equalsIgnoreCase(t[i])) {
                    temp.add(t[i]);
                }
            }
            temp.add(0, Phone.APN_TYPE_DUN);
            mDunApnTypes = temp.toArray(t);
        }

    }

    @Override
    public void dispose() {
        cleanUpConnection(false, null, false);

        super.dispose();

        // Unregister from all events
        mPhone.mCM.unregisterForAvailable(this);
        mPhone.mCM.unregisterForOffOrNotAvailable(this);
        mCdmaPhone.mIccRecords.unregisterForRecordsLoaded(this);
        mPhone.mCM.unregisterForDataNetworkStateChanged(this);
        mCdmaPhone.mCT.unregisterForVoiceCallEnded(this);
        mCdmaPhone.mCT.unregisterForVoiceCallStarted(this);
        mCdmaPhone.mSST.unregisterForDataConnectionAttached(this);
        mCdmaPhone.mSST.unregisterForDataConnectionDetached(this);
        mCdmaPhone.mSST.unregisterForRoamingOn(this);
        mCdmaPhone.mSST.unregisterForRoamingOff(this);
        mCdmaSSM.dispose(this);
        mPhone.mCM.unregisterForCdmaOtaProvision(this);

        mPhone.getContext().getContentResolver().unregisterContentObserver(this.mApnObserver);

        destroyAllDataConnectionList();
    }

    @Override
    protected void finalize() {
        if(DBG) log("CdmaDataConnectionTracker finalized");
    }

    @Override
    protected String getActionIntentReconnectAlarm() {
        return INTENT_RECONNECT_ALARM;
    }

    @Override
    protected String getActionIntentDataStallAlarm() {
        return INTENT_DATA_STALL_ALARM;
    }

    @Override
    protected void restartDataStallAlarm() {}

    @Override
    protected void setState(State s) {
        if (DBG) log ("setState: " + s);
        if (mState != s) {
            EventLog.writeEvent(EventLogTags.CDMA_DATA_STATE_CHANGE,
                    mState.toString(), s.toString());
            mState = s;
        }
    }

    @Override
    public synchronized State getState(String apnType) {
        return mState;
    }

    @Override
    protected boolean isApnTypeAvailable(String type) {
        for (String s : mSupportedApnTypes) {
            if (TextUtils.equals(type, s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isDataAllowed() {
        final boolean internalDataEnabled;
        synchronized (mDataEnabledLock) {
            internalDataEnabled = mInternalDataEnabled;
        }

        int psState = mCdmaPhone.mSST.getCurrentDataConnectionState();
        boolean roaming = (mPhone.getServiceState().getRoaming() && !getDataOnRoamingEnabled());
        boolean desiredPowerState = mCdmaPhone.mSST.getDesiredPowerState();
        boolean subscriptionFromNv = (mCdmaSSM.getCdmaSubscriptionSource()
                                       == CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_NV);

        boolean allowed =
                    (psState == ServiceState.STATE_IN_SERVICE ||
                            mAutoAttachOnCreation) &&
                    (subscriptionFromNv ||
                            mCdmaPhone.mIccRecords.getRecordsLoaded()) &&
                    (mCdmaPhone.mSST.isConcurrentVoiceAndDataAllowed() ||
                            mPhone.getState() == Phone.State.IDLE) &&
                    !roaming &&
                    internalDataEnabled &&
                    desiredPowerState &&
                    !mPendingRestartRadio &&
                    ((mPhone.getLteOnCdmaMode() == Phone.LTE_ON_CDMA_TRUE) ||
                            !mCdmaPhone.needsOtaServiceProvisioning());
        if (!allowed && DBG) {
            String reason = "";
            if (!((psState == ServiceState.STATE_IN_SERVICE) || mAutoAttachOnCreation)) {
                reason += " - psState= " + psState;
            }
            if (!subscriptionFromNv &&
                    !mCdmaPhone.mIccRecords.getRecordsLoaded()) {
                reason += " - RUIM not loaded";
            }
            if (!(mCdmaPhone.mSST.isConcurrentVoiceAndDataAllowed() ||
                    mPhone.getState() == Phone.State.IDLE)) {
                reason += " - concurrentVoiceAndData not allowed and state= " + mPhone.getState();
            }
            if (roaming) reason += " - Roaming";
            if (!internalDataEnabled) reason += " - mInternalDataEnabled= false";
            if (!desiredPowerState) reason += " - desiredPowerState= false";
            if (mPendingRestartRadio) reason += " - mPendingRestartRadio= true";
            if (mCdmaPhone.needsOtaServiceProvisioning()) reason += " - needs Provisioning";
            log("Data not allowed due to" + reason);
        }
        return allowed;
    }

    @Override
    protected boolean isDataPossible(String apnType) {
        boolean possible = isDataAllowed() && !(getAnyDataEnabled() &&
                (mState == State.FAILED || mState == State.IDLE));
        if (!possible && DBG && isDataAllowed()) {
            log("Data not possible.  No coverage: dataState = " + mState);
        }
        return possible;
    }

    private boolean trySetupData(String reason) {
        if (DBG) log("***trySetupData due to " + (reason == null ? "(unspecified)" : reason));

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            setState(State.CONNECTED);
            notifyDataConnection(reason);
            notifyOffApnsOfAvailability(reason);

            log("(fix?) We're on the simulator; assuming data is connected");
            return true;
        }

        int psState = mCdmaPhone.mSST.getCurrentDataConnectionState();
        boolean roaming = mPhone.getServiceState().getRoaming();
        boolean desiredPowerState = mCdmaPhone.mSST.getDesiredPowerState();

        if ((mState == State.IDLE || mState == State.SCANNING) &&
                isDataAllowed() && getAnyDataEnabled() && !isEmergency()) {
            if (mState == State.IDLE) {
                waitingApns = buildWaitingApns();
                waitingApnsPermanentFailureCountDown = waitingApns.size();
                if (waitingApns.isEmpty()) {
                    if (DBG) log("trySetupData: No APN found");
                    notifyNoData(CdmaDataConnection.FailCause.MISSING_UNKNOWN_APN);
                    notifyOffApnsOfAvailability(reason);
                    return false;
                } else {
//                    apnContext.setWaitingApns(waitingApns);
                    if (DBG) {
                        log ("trySetupData: Create from mAllApns : " + apnListToString(mAllApns));
                    }
                }
            }

            boolean retValue = setupData(reason);
            notifyOffApnsOfAvailability(reason);
            return retValue;
        } else {
            notifyOffApnsOfAvailability(reason);
            return false;
        }
    }

    /**
     * Cleanup the CDMA data connection (only one is supported)
     *
     * @param tearDown true if the underlying DataConnection should be disconnected.
     * @param reason for the clean up.
     */
    private void cleanUpConnection(boolean tearDown, String reason, boolean doAll) {
        if (DBG) log("cleanUpConnection: reason: " + reason);

        // Clear the reconnect alarm, if set.
        if (mReconnectIntent != null) {
            AlarmManager am =
                (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
            am.cancel(mReconnectIntent);
            mReconnectIntent = null;
        }

        setState(State.DISCONNECTING);
        notifyOffApnsOfAvailability(reason);

        boolean notificationDeferred = false;
        for (DataConnection conn : mDataConnections.values()) {
            if(conn != null) {
                DataConnectionAc dcac =
                    mDataConnectionAsyncChannels.get(conn.getDataConnectionId());
                if (tearDown) {
                    if (doAll) {
                        if (DBG) log("cleanUpConnection: teardown, conn.tearDownAll");
                        conn.tearDownAll(reason, obtainMessage(EVENT_DISCONNECT_DONE,
                                conn.getDataConnectionId(), 0, reason));
                    } else {
                        if (DBG) log("cleanUpConnection: teardown, conn.tearDown");
                        conn.tearDown(reason, obtainMessage(EVENT_DISCONNECT_DONE,
                                conn.getDataConnectionId(), 0, reason));
                    }
                    notificationDeferred = true;
                } else {
                    if (DBG) log("cleanUpConnection: !tearDown, call conn.resetSynchronously");
                    if (dcac != null) {
                        dcac.resetSync();
                    }
                    notificationDeferred = false;
                }
            }
        }

        stopNetStatPoll();

        if (!notificationDeferred) {
            if (DBG) log("cleanupConnection: !notificationDeferred");
            gotoIdleAndNotifyDataConnection(reason);
        }
    }

    /**
     * @param types comma delimited list of APN types
     * @return array of APN types
     */
    private String[] parseTypes(String types) {
        String[] result;
        // If unset, set to DEFAULT.
        if (types == null || types.equals("")) {
            result = new String[1];
            result[0] = Phone.APN_TYPE_ALL;
        } else {
            result = types.split(",");
        }
        return result;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        ArrayList<ApnSetting> result = new ArrayList<ApnSetting>();
        if (cursor.moveToFirst()) {
            do {
                String[] types = parseTypes(
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                ApnSetting apn = new ApnSetting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                        types,
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.ROAMING_PROTOCOL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.CARRIER_ENABLED)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)));
                result.add(apn);
            } while (cursor.moveToNext());
        }
        if (DBG) log("createApnList: X result=" + result);
        return result;
    }

    private CdmaDataConnection findFreeDataConnection() {
        for (DataConnectionAc dcac : mDataConnectionAsyncChannels.values()) {
            if (dcac.isInactiveSync()) {
                log("found free GsmDataConnection");
                return (CdmaDataConnection) dcac.dataConnection;
            }
        }
        log("NO free CdmaDataConnection");
        return null;
    }

   /**
    * Get next apn in waitingApns
    * @return the first apn found in waitingApns, null if none
    */
   private ApnSetting getNextApn() {
       ArrayList<ApnSetting> list = waitingApns;
       ApnSetting apn = null;

       if (list != null) {
           if (!list.isEmpty()) {
               apn = list.get(0);
           }
       }
       return apn;
   }

    private boolean setupData(String reason) {
        CdmaDataConnection conn = findFreeDataConnection();
        ApnSetting apn = getNextApn();
        if (conn == null) {
            if (DBG) log("setupData: No free CdmaDataConnection found!");
            return false;
        }

        /** TODO: We probably want the connection being setup to a parameter passed around */
        mPendingDataConnection = conn;
        String[] types;
        int apnId;
        mActiveApn = apn;
        if (mRequestedApnType.equals(Phone.APN_TYPE_DUN)) {
            types = mDunApnTypes;
            apnId = DataConnectionTracker.APN_DUN_ID;
        } else {
            types = mDefaultApnTypes;
            apnId = mDefaultApnId;
        }
//        mActiveApn = new ApnSetting(apnId, "", "", "", "", "", "", "", "", "",
//                                    "", 0, types, "IP", "IP", true, 0);
        if (DBG) log("call conn.bringUp mActiveApn=" + mActiveApn);

        Message msg = obtainMessage();
        msg.what = EVENT_DATA_SETUP_COMPLETE;
        msg.obj = reason;
        conn.bringUp(msg, apn);

        setState(State.INITING);
        notifyDataConnection(reason);
        return true;
    }

    /**
     * Handles changes to the APN database.
     */
    private void onApnChanged() {
        boolean isConnected;

        isConnected = (mState != State.IDLE && mState != State.FAILED);
        
        String operator = SystemProperties.get(PROPERTY_OPERATOR_NUMERIC);
        
        mCdmaPhone.updateCurrentCarrierInProvider(operator);
        
        // TODO: It'd be nice to only do this if the changed entrie(s)
        // match the current operator.
        createAllApnList();
        if (mState != State.DISCONNECTING) {
            cleanUpConnection(isConnected, Phone.REASON_APN_CHANGED,false);
            if (!isConnected) {
                // reset reconnect timer
                mDataConnections.get(0).resetRetryCount();
                trySetupData(Phone.REASON_APN_CHANGED);
            }
        }
    }

    private void notifyDefaultData(String reason) {
        setState(State.CONNECTED);
        notifyDataConnection(reason);
        startNetStatPoll();
        mDataConnections.get(0).resetRetryCount();
    }

    private void resetPollStats() {
        mTxPkts = -1;
        mRxPkts = -1;
        mSentSinceLastRecv = 0;
        mNetStatPollPeriod = POLL_NETSTAT_MILLIS;
        mNoRecvPollCount = 0;
    }

    @Override
    protected void startNetStatPoll() {
        if (mState == State.CONNECTED && mNetStatPollEnabled == false) {
            log("[DataConnection] Start poll NetStat");
            resetPollStats();
            mNetStatPollEnabled = true;
            mPollNetStat.run();
        }
    }

    @Override
    protected void stopNetStatPoll() {
        mNetStatPollEnabled = false;
        removeCallbacks(mPollNetStat);
        log("[DataConnection] Stop poll NetStat");
    }

    @Override
    protected void restartRadio() {
        if (DBG) log("Cleanup connection and wait " +
                (TIME_DELAYED_TO_RESTART_RADIO / 1000) + "s to restart radio");
        cleanUpAllConnections(null);
        sendEmptyMessageDelayed(EVENT_RESTART_RADIO, TIME_DELAYED_TO_RESTART_RADIO);
        mPendingRestartRadio = true;
    }

    private Runnable mPollNetStat = new Runnable() {

        public void run() {
            long sent, received;
            long preTxPkts = -1, preRxPkts = -1;

            Activity newActivity;

            preTxPkts = mTxPkts;
            preRxPkts = mRxPkts;

            mTxPkts = TrafficStats.getMobileTxPackets();
            mRxPkts = TrafficStats.getMobileRxPackets();

            //log("rx " + String.valueOf(rxPkts) + " tx " + String.valueOf(txPkts));

            if (mNetStatPollEnabled && (preTxPkts > 0 || preRxPkts > 0)) {
                sent = mTxPkts - preTxPkts;
                received = mRxPkts - preRxPkts;

                if ( sent > 0 && received > 0 ) {
                    mSentSinceLastRecv = 0;
                    newActivity = Activity.DATAINANDOUT;
                } else if (sent > 0 && received == 0) {
                    if (mPhone.getState()  == Phone.State.IDLE) {
                        mSentSinceLastRecv += sent;
                    } else {
                        mSentSinceLastRecv = 0;
                    }
                    newActivity = Activity.DATAOUT;
                } else if (sent == 0 && received > 0) {
                    mSentSinceLastRecv = 0;
                    newActivity = Activity.DATAIN;
                } else if (sent == 0 && received == 0) {
                    newActivity = (mActivity == Activity.DORMANT) ? mActivity : Activity.NONE;
                } else {
                    mSentSinceLastRecv = 0;
                    newActivity = (mActivity == Activity.DORMANT) ? mActivity : Activity.NONE;
                }

                if (mActivity != newActivity && mIsScreenOn) {
                    mActivity = newActivity;
                    mPhone.notifyDataActivity();
                }
            }

            if (mSentSinceLastRecv >= NUMBER_SENT_PACKETS_OF_HANG) {
                // Packets sent without ack exceeded threshold.

                if (mNoRecvPollCount == 0) {
                    EventLog.writeEvent(
                            EventLogTags.PDP_RADIO_RESET_COUNTDOWN_TRIGGERED,
                            mSentSinceLastRecv);
                }

                if (mNoRecvPollCount < NO_RECV_POLL_LIMIT) {
                    mNoRecvPollCount++;
                    // Slow down the poll interval to let things happen
                    mNetStatPollPeriod = POLL_NETSTAT_SLOW_MILLIS;
                } else {
                    if (DBG) log("Sent " + String.valueOf(mSentSinceLastRecv) +
                                        " pkts since last received");
                    // We've exceeded the threshold.  Restart the radio.
                    mNetStatPollEnabled = false;
                    stopNetStatPoll();
                    restartRadio();
                    EventLog.writeEvent(EventLogTags.PDP_RADIO_RESET, NO_RECV_POLL_LIMIT);
                }
            } else {
                mNoRecvPollCount = 0;
                mNetStatPollPeriod = POLL_NETSTAT_MILLIS;
            }

            if (mNetStatPollEnabled) {
                mDataConnectionTracker.postDelayed(this, mNetStatPollPeriod);
            }
        }
    };

    /**
     * Returns true if the last fail cause is something that
     * seems like it deserves an error notification.
     * Transient errors are ignored
     */
    private boolean
    shouldPostNotification(FailCause cause) {
        return (cause != FailCause.UNKNOWN);
    }

    /**
     * Return true if data connection need to be setup after disconnected due to
     * reason.
     *
     * @param reason the reason why data is disconnected
     * @return true if try setup data connection is need for this reason
     */
    private boolean retryAfterDisconnected(String reason) {
        boolean retry = true;

        if ( Phone.REASON_RADIO_TURNED_OFF.equals(reason) ) {
            retry = false;
        }
        return retry;
    }

    private void reconnectAfterFail(FailCause lastFailCauseCode, String reason, int retryOverride) {
        if (mState == State.FAILED) {
            /**
             * For now With CDMA we never try to reconnect on
             * error and instead just continue to retry
             * at the last time until the state is changed.
             * TODO: Make this configurable?
             */
            int nextReconnectDelay = retryOverride;
            if (nextReconnectDelay < 0) {
                nextReconnectDelay = mDataConnections.get(0).getRetryTimer();
                mDataConnections.get(0).increaseRetryCount();
            }
            startAlarmForReconnect(nextReconnectDelay, reason);

            if (!shouldPostNotification(lastFailCauseCode)) {
                log("NOT Posting Data Connection Unavailable notification "
                                + "-- likely transient error");
            } else {
                notifyNoData(lastFailCauseCode);
            }
        }
    }

    private void startAlarmForReconnect(int delay, String reason) {

        log("Data Connection activate failed. Scheduling next attempt for "
                + (delay / 1000) + "s");

        AlarmManager am =
            (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(INTENT_RECONNECT_ALARM);
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON, reason);
        mReconnectIntent = PendingIntent.getBroadcast(
                mPhone.getContext(), 0, intent, 0);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, mReconnectIntent);

    }

    private void notifyNoData(FailCause lastFailCauseCode) {
        setState(State.FAILED);
        notifyOffApnsOfAvailability(null);
    }

    protected void gotoIdleAndNotifyDataConnection(String reason) {
        if (DBG) log("gotoIdleAndNotifyDataConnection: reason=" + reason);
        setState(State.IDLE);
        notifyDataConnection(reason);
        mActiveApn = null;
    }

    protected void onRecordsLoaded() {
		createAllApnList();
        if (mState == State.FAILED) {
            cleanUpAllConnections(null);
        }
        sendMessage(obtainMessage(EVENT_TRY_SETUP_DATA, Phone.REASON_SIM_LOADED));
    }

    protected void onNVReady() {
        if (mState == State.FAILED) {
            cleanUpAllConnections(null);
        }
        sendMessage(obtainMessage(EVENT_TRY_SETUP_DATA));
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onEnableNewApn() {
        // No mRequestedApnType check; only one connection is supported
        cleanUpConnection(true, Phone.REASON_APN_SWITCHED, false);
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected boolean onTrySetupData(String reason) {
        return trySetupData(reason);
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onRoamingOff() {
        if (mUserDataEnabled == false) return;

        if (getDataOnRoamingEnabled() == false) {
            notifyOffApnsOfAvailability(Phone.REASON_ROAMING_OFF);
            trySetupData(Phone.REASON_ROAMING_OFF);
        } else {
            notifyDataConnection(Phone.REASON_ROAMING_OFF);
        }
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onRoamingOn() {
        if (mUserDataEnabled == false) return;

        if (getDataOnRoamingEnabled()) {
            trySetupData(Phone.REASON_ROAMING_ON);
            notifyDataConnection(Phone.REASON_ROAMING_ON);
        } else {
            if (DBG) log("Tear down data connection on roaming.");
            cleanUpAllConnections(null);
            notifyOffApnsOfAvailability(Phone.REASON_ROAMING_ON);
        }
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onRadioAvailable() {
        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            setState(State.CONNECTED);
            notifyDataConnection(null);

            log("We're on the simulator; assuming data is connected");
        }

        notifyOffApnsOfAvailability(null);

        if (mState != State.IDLE) {
            cleanUpAllConnections(null);
        }
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onRadioOffOrNotAvailable() {
        mDataConnections.get(0).resetRetryCount();

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            log("We're on the simulator; assuming radio off is meaningless");
        } else {
            if (DBG) log("Radio is off and clean up all connection");
            cleanUpAllConnections(null);
        }
    }

     @Override
     public boolean isApnTypeActive(String type) {
         return mActiveApn != null && mActiveApn.canHandleType(type);
     }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onDataSetupComplete(AsyncResult ar) {
		String reason = null;
        DataConnection.FailCause cause = DataConnection.FailCause.UNKNOWN;
        boolean handleError = false;
        if (ar.userObj instanceof String) {
            reason = (String) ar.userObj;
        }

        if (isDataSetupCompleteOk(ar)) {
            // Everything is setup
            if(isApnTypeActive(Phone.APN_TYPE_DEFAULT)) {
                SystemProperties.set("gsm.defaultpdpcontext.active", "true");
                if (canSetPreferApn && mPreferredApn == null) {
                    if (DBG) log("onDataSetupComplete: PREFERED APN is null");
                    mPreferredApn = mActiveApn;
                    if (mPreferredApn != null) {
                        setPreferredApn(mPreferredApn.id);
                    }
                }
            } else {
                SystemProperties.set("gsm.defaultpdpcontext.active", "false");
            }
            notifyDefaultData(reason);
        } else {
            cause = (DataConnection.FailCause) (ar.result);
            if(DBG) log("Data Connection setup failed " + cause);

            waitingApnsPermanentFailureCountDown -= cause.isPermanentFail() ? 1 : 0;

            waitingApns.remove(0);
            if (DBG) {
                log(String.format("onDataSetupComplete: WaitingApns.size=%d" +
                        " WaitingApnsPermFailureCountDown=%d",
                        waitingApns.size(),
                        waitingApnsPermanentFailureCountDown));
            }
            handleError = true;
        }

        if (handleError) {
            // See if there are more APN's to try
            if (waitingApns.isEmpty()) {
                if (waitingApnsPermanentFailureCountDown == 0) {
                    if (DBG) {
                        log("onDataSetupComplete: All APN's had permanent failures, stop retrying");
                    }
                    setState(State.FAILED);
                    mPhone.notifyDataConnection(Phone.REASON_APN_FAILED, mRequestedApnType);

                } else {
                    if (DBG) log("onDataSetupComplete: Not all permanent failures, retry");
                    // check to see if retry should be overridden for this failure.
                    int retryOverride = -1;
                    if (ar.exception instanceof DataConnection.CallSetupException) {
                        retryOverride =
                            ((DataConnection.CallSetupException)ar.exception).getRetryOverride();
                    }
                    if (retryOverride == RILConstants.MAX_INT) {
                        if (DBG) log("No retry is suggested.");
                    } else {
                        startDelayedRetry(cause, reason, retryOverride);
                    }
                }
            } else {
                if (DBG) log("onDataSetupComplete: Try next APN");
                setState(State.SCANNING);
                // Wait a bit before trying the next APN, so that
                // we're not tying up the RIL command channel
                startAlarmForReconnect(APN_DELAY_MILLIS, reason);
            }
        }
    }

    /**
     * Called when EVENT_DISCONNECT_DONE is received.
     */
    @Override
    protected void onDisconnectDone(int connId, AsyncResult ar) {
        if(DBG) log("EVENT_DISCONNECT_DONE connId=" + connId);
        String reason = null;
        if (ar.userObj instanceof String) {
            reason = (String) ar.userObj;
        }
        setState(State.IDLE);

        // Since the pending request to turn off or restart radio will be processed here,
        // remove the pending event to restart radio from the message queue.
        if (mPendingRestartRadio) removeMessages(EVENT_RESTART_RADIO);

        // Process the pending request to turn off radio in ServiceStateTracker first.
        // If radio is turned off in ServiceStateTracker, ignore the pending event to restart radio.
        CdmaServiceStateTracker ssTracker = mCdmaPhone.mSST;
        if (ssTracker.processPendingRadioPowerOffAfterDataOff()) {
            mPendingRestartRadio = false;
        } else {
            onRestartRadio();
        }

        notifyDataConnection(reason);
        mActiveApn = null;
        if (retryAfterDisconnected(reason)) {
          // Wait a bit before trying, so we're not tying up RIL command channel.
          startAlarmForReconnect(APN_DELAY_MILLIS, reason);
      }
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onVoiceCallStarted() {
        if (mState == State.CONNECTED && !mCdmaPhone.mSST.isConcurrentVoiceAndDataAllowed()) {
            stopNetStatPoll();
            notifyDataConnection(Phone.REASON_VOICE_CALL_STARTED);
            notifyOffApnsOfAvailability(Phone.REASON_VOICE_CALL_STARTED);
        }
    }

    /**
     * @override com.android.internal.telephony.DataConnectionTracker
     */
    @Override
    protected void onVoiceCallEnded() {
        if (mState == State.CONNECTED) {
            if (!mCdmaPhone.mSST.isConcurrentVoiceAndDataAllowed()) {
                startNetStatPoll();
                notifyDataConnection(Phone.REASON_VOICE_CALL_ENDED);
            } else {
                // clean slate after call end.
                resetPollStats();
            }
            notifyOffApnsOfAvailability(Phone.REASON_VOICE_CALL_ENDED);
        } else {
            mDataConnections.get(0).resetRetryCount();
            // in case data setup was attempted when we were on a voice call
            trySetupData(Phone.REASON_VOICE_CALL_ENDED);
        }
    }

    @Override
    protected void onCleanUpConnection(boolean tearDown, int apnId, String reason) {
        // No apnId check; only one connection is supported
        cleanUpConnection(tearDown, reason, (apnId == APN_DUN_ID));
    }

    @Override
    protected void onCleanUpAllConnections(String cause) {
        // Only one CDMA connection is supported
        cleanUpConnection(true, cause, false);
    }

    private void createAllDataConnectionList() {
        CdmaDataConnection dataConn;

        String retryConfig = SystemProperties.get("ro.cdma.data_retry_config");
        for (int i = 0; i < DATA_CONNECTION_POOL_SIZE; i++) {
            RetryManager rm = new RetryManager();
            if (!rm.configure(retryConfig)) {
                if (!rm.configure(DEFAULT_DATA_RETRY_CONFIG)) {
                    // Should never happen, log an error and default to a simple linear sequence.
                    log("Could not configure using DEFAULT_DATA_RETRY_CONFIG="
                            + DEFAULT_DATA_RETRY_CONFIG);
                    rm.configure(20, 2000, 1000);
                }
            }

            int id = mUniqueIdGenerator.getAndIncrement();
            dataConn = CdmaDataConnection.makeDataConnection(mCdmaPhone, id, rm, this);
            mDataConnections.put(id, dataConn);
            DataConnectionAc dcac = new DataConnectionAc(dataConn, LOG_TAG);
            int status = dcac.fullyConnectSync(mPhone.getContext(), this, dataConn.getHandler());
            if (status == AsyncChannel.STATUS_SUCCESSFUL) {
                log("Fully connected");
                mDataConnectionAsyncChannels.put(dcac.dataConnection.getDataConnectionId(), dcac);
            } else {
                log("Could not connect to dcac.dataConnection=" + dcac.dataConnection +
                        " status=" + status);
            }

        }
    }

    /**
     * Based on the sim operator numeric, create a list for all possible
     * Data Connections and setup the preferredApn.
     */
    private void createAllApnList() {
        mAllApns = new ArrayList<ApnSetting>();
  
          String operator = SystemProperties.get(PROPERTY_OPERATOR_NUMERIC);
          if (operator != null) {
             String selection = "numeric = '" + operator + "'";
 
             Cursor cursor = mPhone.getContext().getContentResolver().query(
                     Telephony.Carriers.CONTENT_URI, null, selection, null, null);

             if (cursor != null) {
                 if (cursor.getCount() > 0) {
                     mAllApns = createApnList(cursor);
                 }
                 cursor.close();
             }
         }
 
         if (mAllApns.isEmpty()) {
             if (DBG) log("No APN found for carrier: " + operator);
             mPreferredApn = null;
         } else {
             mPreferredApn = getPreferredApn();
             Log.d(LOG_TAG, "Get PreferredAPN");
             if (mPreferredApn != null && !mPreferredApn.numeric.equals(operator)) {
                 mPreferredApn = null;
                 setPreferredApn(-1);
             }
         }

    }

    private void destroyAllDataConnectionList() {
        if(mDataConnections != null) {
            mDataConnections.clear();
        }
    }

    private void onCdmaDataDetached() {
        if (mState == State.CONNECTED) {
            startNetStatPoll();
            notifyDataConnection(Phone.REASON_CDMA_DATA_DETACHED);
        } else {
            if (mState == State.FAILED) {
                cleanUpConnection(false, Phone.REASON_CDMA_DATA_DETACHED, false);
                mDataConnections.get(0).resetRetryCount();

                CdmaCellLocation loc = (CdmaCellLocation)(mPhone.getCellLocation());
                EventLog.writeEvent(EventLogTags.CDMA_DATA_SETUP_FAILED,
                        loc != null ? loc.getBaseStationId() : -1,
                        TelephonyManager.getDefault().getNetworkType());
            }
            trySetupData(Phone.REASON_CDMA_DATA_DETACHED);
        }
    }

    private void onCdmaOtaProvision(AsyncResult ar) {
        if (ar.exception != null) {
            int [] otaPrivision = (int [])ar.result;
            if ((otaPrivision != null) && (otaPrivision.length > 1)) {
                switch (otaPrivision[0]) {
                case Phone.CDMA_OTA_PROVISION_STATUS_COMMITTED:
                case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED:
                    mDataConnections.get(0).resetRetryCount();
                    break;
                default:
                    break;
                }
            }
        }
    }

    private void onRestartRadio() {
        if (mPendingRestartRadio) {
            log("************TURN OFF RADIO**************");
            mPhone.mCM.setRadioPower(false, null);
            /* Note: no need to call setRadioPower(true).  Assuming the desired
             * radio power state is still ON (as tracked by ServiceStateTracker),
             * ServiceStateTracker will call setRadioPower when it receives the
             * RADIO_STATE_CHANGED notification for the power off.  And if the
             * desired power state has changed in the interim, we don't want to
             * override it with an unconditional power on.
             */
            mPendingRestartRadio = false;
        }
    }

    private void writeEventLogCdmaDataDrop() {
        CdmaCellLocation loc = (CdmaCellLocation)(mPhone.getCellLocation());
        EventLog.writeEvent(EventLogTags.CDMA_DATA_DROP,
                loc != null ? loc.getBaseStationId() : -1,
                TelephonyManager.getDefault().getNetworkType());
    }

    protected void onDataStateChanged(AsyncResult ar) {
        ArrayList<DataCallState> dataCallStates = (ArrayList<DataCallState>)(ar.result);

        if (ar.exception != null) {
            // This is probably "radio not available" or something
            // of that sort. If so, the whole connection is going
            // to come down soon anyway
            return;
        }

        if (mState == State.CONNECTED) {
            boolean isActiveOrDormantConnectionPresent = false;
            int connectionState = DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE;

            // Check for an active or dormant connection element in
            // the DATA_CALL_LIST array
            for (int index = 0; index < dataCallStates.size(); index++) {
                connectionState = dataCallStates.get(index).active;
                if (connectionState != DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE) {
                    isActiveOrDormantConnectionPresent = true;
                    break;
                }
            }

            if (!isActiveOrDormantConnectionPresent) {
                // No active or dormant connection
                log("onDataStateChanged: No active connection"
                        + "state is CONNECTED, disconnecting/cleanup");
                writeEventLogCdmaDataDrop();
                cleanUpConnection(true, null, false);
                return;
            }

            switch (connectionState) {
                case DATA_CONNECTION_ACTIVE_PH_LINK_UP:
                    log("onDataStateChanged: active=LINK_ACTIVE && CONNECTED, ignore");
                    mActivity = Activity.NONE;
                    mPhone.notifyDataActivity();
                    startNetStatPoll();
                    break;

                case DATA_CONNECTION_ACTIVE_PH_LINK_DOWN:
                    log("onDataStateChanged active=LINK_DOWN && CONNECTED, dormant");
                    mActivity = Activity.DORMANT;
                    mPhone.notifyDataActivity();
                    stopNetStatPoll();
                    break;

                default:
                    log("onDataStateChanged: IGNORE unexpected DataCallState.active="
                            + connectionState);
            }
        } else {
            // TODO: Do we need to do anything?
            log("onDataStateChanged: not connected, state=" + mState + " ignoring");
        }
    }

    /**
     * Build a list of APNs to be used to create PDP's.
     *
     * @param requestedApnType
     * @return waitingApns list to be used to create PDP
     *          error when waitingApns.isEmpty()
     */
    private ArrayList<ApnSetting> buildWaitingApns() {
        ArrayList<ApnSetting> apnList = new ArrayList<ApnSetting>();
 
        String operator = SystemProperties.get(PROPERTY_OPERATOR_NUMERIC);
        if (mRequestedApnType.equals(Phone.APN_TYPE_DEFAULT)) {
            if (canSetPreferApn && mPreferredApn != null) {
                Log.i(LOG_TAG, "Preferred APN:" + operator + ":"
                        + mPreferredApn.numeric + ":" + mPreferredApn);
                if (mPreferredApn.numeric.equals(operator)) {
                    Log.i(LOG_TAG, "Waiting APN set to preferred APN");
                    apnList.add(mPreferredApn);
                    return apnList;
                } else {
                    setPreferredApn(-1);
                    mPreferredApn = null;
                }
            }
        }

        if (mAllApns != null) {
            for (ApnSetting apn : mAllApns) {
                if (apn.canHandleType(mRequestedApnType)) {
                    apnList.add(apn);
                }
            }
        }
        return apnList;
    }

    private String apnListToString (ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, size = apns.size(); i < size; i++) {
            result.append('[')
                  .append(apns.get(i).toString())
                  .append(']');
        }
        return result.toString();
    }

    private void startDelayedRetry(FailCause cause, String reason, int retryOverride) {
        notifyNoData(cause);
        reconnectAfterFail(cause, reason, retryOverride);
    }

    private void setPreferredApn(int pos) {
        if (!canSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }

        log("setPreferredApn: delete");
        ContentResolver resolver = mPhone.getContext().getContentResolver();
        resolver.delete(PREFERAPN_NO_UPDATE_URI, null, null);

        if (pos >= 0) {
            log("setPreferredApn: insert");
            ContentValues values = new ContentValues();
            values.put(APN_ID, pos);
            resolver.insert(PREFERAPN_NO_UPDATE_URI, values);
        }
    }

    private ApnSetting getPreferredApn() {
        if (mAllApns.isEmpty()) {
            log("getPreferredApn: X not found mAllApns.isEmpty");
            return null;
        }
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                PREFERAPN_NO_UPDATE_URI, new String[] { "_id", "name", "apn" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            canSetPreferApn = true;
        } else {
            canSetPreferApn = false;
        }

        if (canSetPreferApn && cursor.getCount() > 0) {
            int pos;
            cursor.moveToFirst();
            pos = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
            for(ApnSetting p:mAllApns) {
                if (p.id == pos && p.canHandleType(mRequestedApnType)) {
                    log("getPreferredApn: X found apnSetting" + p);
                    cursor.close();
                    return p;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        log("getPreferredApn: X not found");
        return null;
    }

    @Override
    public void handleMessage (Message msg) {
        if (DBG) log("CdmaDCT handleMessage msg=" + msg);

        if (!mPhone.mIsTheCurrentActivePhone || mIsDisposed) {
            log("Ignore CDMA msgs since CDMA phone is inactive");
            return;
        }

        switch (msg.what) {
            case EVENT_RECORDS_LOADED:
                onRecordsLoaded();
                break;

            case EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                if(mCdmaSSM.getCdmaSubscriptionSource() ==
                       CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_NV) {
                    onNVReady();
                }
                break;

            case EVENT_CDMA_DATA_DETACHED:
                onCdmaDataDetached();
                break;

            case EVENT_DATA_STATE_CHANGED:
                onDataStateChanged((AsyncResult) msg.obj);
                break;

            case EVENT_CDMA_OTA_PROVISION:
                onCdmaOtaProvision((AsyncResult) msg.obj);
                break;

            case EVENT_APN_CHANGED:
                onApnChanged();
                break;

            case EVENT_RESTART_RADIO:
                if (DBG) log("EVENT_RESTART_RADIO");
                onRestartRadio();
                break;

            default:
                // handle the message in the super class DataConnectionTracker
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    public boolean isDisconnected() {
        return ((mState == State.IDLE) || (mState == State.FAILED));
    }

    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[CdmaDCT] " + s);
    }

    @Override
    protected void loge(String s) {
        Log.e(LOG_TAG, "[CdmaDCT] " + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CdmaDataConnectionTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mCdmaPhone=" + mCdmaPhone);
        pw.println(" mCdmaSSM=" + mCdmaSSM);
        pw.println(" mPendingDataConnection=" + mPendingDataConnection);
        pw.println(" mPendingRestartRadio=" + mPendingRestartRadio);
        pw.println(" mSupportedApnTypes=" + mSupportedApnTypes);
        pw.println(" mDefaultApnTypes=" + mDefaultApnTypes);
        pw.println(" mDunApnTypes=" + mDunApnTypes);
        pw.println(" mDefaultApnId=" + mDefaultApnId);
    }
}
