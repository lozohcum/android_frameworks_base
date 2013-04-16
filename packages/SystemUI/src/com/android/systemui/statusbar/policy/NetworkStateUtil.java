
package com.android.systemui.statusbar.policy;

import com.android.systemui.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class NetworkStateUtil {
    private ConnectivityManager conMan;
    private Context mContext;
    private State mobileState, wifiState;

    public NetworkStateUtil(Context context) {
        mContext = context;
        conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mobileState = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        wifiState = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
    }

    public Boolean isAvail() {
        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        return manager.isWifiEnabled();
    }

    public int getSignalLevel() {
        if (wifiState.equals(NetworkInfo.State.CONNECTED)) {
            WifiManager wifiMan = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            String currentSSID = wifiMan.getConnectionInfo().getSSID();
            wifiMan.startScan();
            for (ScanResult result : wifiMan.getScanResults()) {
                if (result.SSID.equals(currentSSID)) {
                    return WifiManager.calculateSignalLevel(result.level, 4);
                }
            }
        }
        return 4;
    }
    
    @Override
    public String toString() {
        String result = mContext.getResources().getString(R.string.click_to_connect_wifi);

        if (wifiState.equals(NetworkInfo.State.CONNECTED)) {
            WifiManager wifiMan = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            result = wifiMan.getConnectionInfo().getSSID();
        } else if (mobileState.equals(NetworkInfo.State.CONNECTED)) {
            // result = "Mobile Data";
        } else {
            // result = "No network";
        }
        if (result == null || result.isEmpty()) {
            result = mContext.getResources().getString(R.string.click_to_connect_wifi);
        }
        return result;
    }

}
