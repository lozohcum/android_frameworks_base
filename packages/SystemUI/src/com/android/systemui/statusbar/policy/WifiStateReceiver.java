
package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class WifiStateReceiver {

    private final int wifi_connected_icon[] = {
            R.drawable.ic_wifi_signal_blue_1_dark,
            R.drawable.ic_wifi_signal_blue_2_dark,
            R.drawable.ic_wifi_signal_blue_3_dark,
            R.drawable.ic_wifi_signal_blue_4_dark,
            R.drawable.ic_wifi_signal_blue_5_dark
    };

    Context mContext;
    ViewGroup mViewGroup;
    TextView mTextView;
    ImageView mImageView;
    public PhoneStatusBar mService;

    public WifiStateReceiver(Context context, ViewGroup viewGroup) {
        mViewGroup = viewGroup;
        mTextView = (TextView) viewGroup.findViewById(R.id.network_text);
        mImageView = (ImageView) viewGroup.findViewById(R.id.network_img);
        mContext = context;

        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (manager != null) {
            if (manager.isWifiEnabled()) {
                mTextView.setText(new NetworkStateUtil(mContext).toString());
                mImageView.setVisibility(View.VISIBLE);
                mViewGroup.setVisibility(View.VISIBLE);
            } else {
                mViewGroup.setVisibility(View.GONE);
            }
        }
    }

    public void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mContext.registerReceiver(new StatusChangeReceiver(),
                filter);
    }

    public class StatusChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();

            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (mWifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        mTextView.setEnabled(true);
                        String SSID = info.getSSID();
                        if (SSID != null) {
                            // mTextView.setText(info.getSSID());
                        } else {
                            // mTextView.setText("Click to connect AP");
                        } // if SSID == null, wifi is enable but not connection
                        NetworkStateUtil util = new NetworkStateUtil(mContext);
                        mTextView.setText(util.toString());
                        mTextView.setVisibility(View.VISIBLE);
                        mImageView.setImageDrawable(mContext.getResources().getDrawable(
                                R.drawable.ic_wifi_signal_blue_5_dark));
                        mImageView.setVisibility(View.VISIBLE);
                        if (mService.getViewPager().getCurrentItem() == 0) {
                            mViewGroup.setVisibility(View.VISIBLE);
                        }
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        mTextView.setEnabled(true);
                        mTextView.setVisibility(View.GONE);
                        mImageView.setVisibility(View.GONE);
                        // mTextView.setText("No network connection");
                        mViewGroup.setVisibility(View.GONE);
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                    case WifiManager.WIFI_STATE_ENABLING:
                    default:
                        mTextView.setVisibility(View.GONE);
                        mImageView.setVisibility(View.GONE);
                        mTextView.setEnabled(false);
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkStateUtil util = new NetworkStateUtil(mContext);
                Boolean isAvail = util.isAvail();
                mTextView.setText(util.toString());
                mTextView.setEnabled(isAvail);
                mTextView.setVisibility(isAvail ? View.VISIBLE :
                        View.INVISIBLE);
                if (isAvail) {
                    mImageView.setImageDrawable(mContext.getResources().getDrawable(
                            wifi_connected_icon[util.getSignalLevel()]));
                }
                mImageView.setVisibility(isAvail ? View.VISIBLE :
                        View.INVISIBLE);
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                NetworkStateUtil util = new NetworkStateUtil(mContext);
                Boolean isAvail = util.isAvail();
                mTextView.setText(util.toString());
                mTextView.setEnabled(isAvail);
                mTextView.setVisibility(isAvail ? View.VISIBLE :
                        View.INVISIBLE);
                if (isAvail) {
                    int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 65535);
                    if (rssi < 65535) {
                        rssi = WifiManager.calculateSignalLevel(rssi, 4);
                        mImageView.setImageDrawable(
                                mContext.getResources().getDrawable(wifi_connected_icon[rssi]));
                    }
                }
                mImageView.setVisibility(isAvail ? View.VISIBLE :
                        View.INVISIBLE);
            }
        }
    }
}
