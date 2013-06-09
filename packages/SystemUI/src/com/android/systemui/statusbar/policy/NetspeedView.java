package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.provider.Settings.System;

public class NetspeedView extends TextView {

	private final String TAG = "NetSpeedView";
    private Context mContext;
	private boolean mNetworkStats = false;
	private boolean mMutexLock = false;
    private int mNetspeedSwitch = 0;
    private long rxspd = 0;
    
    private void registerObserver() {
        mContext = getContext();
        mNetspeedSwitch = System.getInt(mContext.getContentResolver(),
                System.STATUS_BAR_NET_SPD, 1);
        
        mContext.getContentResolver().registerContentObserver(System.getUriFor(
                System.STATUS_BAR_NET_SPD), false,
                new ContentObserver(new Handler()) {
                    public void onChange(boolean selfChange) {
                        mNetspeedSwitch = System.getInt(mContext.getContentResolver(),
                            System.STATUS_BAR_NET_SPD, 1);
                        if (mNetspeedSwitch == 1) {
                        	setVisibility(View.VISIBLE);
                            getNetworkSpeed();
                        } else if (mNetspeedSwitch == 0) {
                        	setVisibility(View.INVISIBLE);
                        }
                        else
                            Log.e(TAG, "Error get switch state");
                    }
                }
        );
    }
    
	public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
	
	private BroadcastReceiver NetworkStatsListner = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			if(isNetworkAvailable(getContext())) {
				mNetworkStats = true;
				if (mNetspeedSwitch == 1) {
					setVisibility(View.VISIBLE);
					getNetworkSpeed();
				}
			} else {
				mNetworkStats = false;
				setVisibility(View.INVISIBLE);
			}
		}
	};
	
	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		getContext().unregisterReceiver(NetworkStatsListner);
		super.onDetachedFromWindow();
	}

	public NetspeedView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		getNetworkSpeed();
		registerListener(context, NetworkStatsListner);
	}

	public NetspeedView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		getNetworkSpeed();
		registerListener(context, NetworkStatsListner);
	}

	public NetspeedView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		getNetworkSpeed();
		registerListener(context, NetworkStatsListner);
	}
	
	private void registerListener(Context context, BroadcastReceiver receiver) {
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(receiver, filter);
		registerObserver();
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message what) {
            switch(what.what) {
                case 0:
				    setVisibility(View.INVISIBLE);
                    break;
                case 1:
                	if (mNetspeedSwitch == 1) {
                		setText(rxspd+"kb/s");
                		setVisibility(View.VISIBLE);
                	}
                    break;
            }
		}
	};

	private void getNetworkSpeed() {
		Thread t = new Thread() {
			@Override
			public void run() {
				mMutexLock = true;
                int sNum = 0;
				while(mNetspeedSwitch == 1) {
					try {
						long rxspd_bf = TrafficStats.getTotalRxBytes() / 1024;
						Thread.sleep(1000);
						long rxspd_af = TrafficStats.getTotalRxBytes() / 1024;
						rxspd = rxspd_af - rxspd_bf;
                        if (rxspd < 0)
                            rxspd = 0;
                        if (rxspd == 0) {
                            sNum++;
                            if (sNum > 10)
                                sNum = 10;
                        } else {
                            sNum = 0;
                        }
                        //十秒内速度均为0,隐藏view
                        if (sNum == 10)
                            mHandler.sendEmptyMessage(0);
                        else
						    mHandler.sendEmptyMessage(1);
					} catch(Exception e) {
						Log.e(TAG, "Error when get net speed!");
					}
				}
				mMutexLock = false;
			}
		};
		if (!mMutexLock) t.start();
	}
	
}
