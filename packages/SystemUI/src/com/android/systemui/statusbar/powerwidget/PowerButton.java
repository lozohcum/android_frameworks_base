package com.android.systemui.statusbar.powerwidget;

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;

public abstract class PowerButton {
    public static final String TAG = "PowerButton";

    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_TURNING_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_INTERMEDIATE = 5;
    public static final int STATE_UNKNOWN = 6;

    public static final String BUTTON_WIFI = "toggleWifi";
    public static final String BUTTON_GPS = "toggleGPS";
    public static final String BUTTON_BLUETOOTH = "toggleBluetooth";
    public static final String BUTTON_BRIGHTNESS = "toggleBrightness";
    public static final String BUTTON_SOUND = "toggleSound";
    public static final String BUTTON_SYNC = "toggleSync";
    public static final String BUTTON_WIFIAP = "toggleWifiAp";
    public static final String BUTTON_SCREENTIMEOUT = "toggleScreenTimeout";
    public static final String BUTTON_MOBILEDATA = "toggleMobileData";
    public static final String BUTTON_LOCKSCREEN = "toggleLockScreen";
    public static final String BUTTON_NETWORKMODE = "toggleNetworkMode";
    public static final String BUTTON_AUTOROTATE = "toggleAutoRotate";
    public static final String BUTTON_AIRPLANE = "toggleAirplane";
    public static final String BUTTON_FLASHLIGHT = "toggleFlashlight";
    public static final String BUTTON_SLEEP = "toggleSleepMode";
    public static final String BUTTON_MEDIA_PLAY_PAUSE = "toggleMediaPlayPause";
    public static final String BUTTON_MEDIA_PREVIOUS = "toggleMediaPrevious";
    public static final String BUTTON_MEDIA_NEXT = "toggleMediaNext";
    public static final String BUTTON_LTE = "toggleLte";
    public static final String BUTTON_WIMAX = "toggleWimax";
    public static final String BUTTON_DRIVER = "toggleDriver";
    public static final String BUTTON_REST = "toggleReboot";
    public static final String BUTTON_SHUT = "toggleShutdown";
    public static final String BUTTON_UNKNOWN = "unknown";
    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";
    
    //for text color
    public static final int TEXT_COLOR_ENABLE = R.color.power_widget_panel_text_enable_color;
    public static final int TEXT_COLOR_DISABLE = R.color.power_widget_panel_text_disable_color;

    private static final Mode MASK_MODE = Mode.SCREEN;

    protected int mIcon;
    protected int mState;
    protected View mView;
    protected String mType = BUTTON_UNKNOWN;

    private ImageView mIconView;
    private ImageView mIndicatorView;
    private TextView mTextView;

    private View.OnClickListener mExternalClickListener;
    private View.OnLongClickListener mExternalLongClickListener;

    protected boolean mHapticFeedback;
    protected Vibrator mVibrator;
    private long[] mClickPattern;
    private long[] mLongClickPattern;

    // we use this to ensure we update our views on the UI thread
    private Handler mViewUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mIconView != null) {
                mIconView.setImageResource(mIcon);
            }
            if (mIndicatorView != null) {
                Context context = mIndicatorView.getContext();
                ContentResolver cr = context.getContentResolver();
                Resources res = context.getResources();

//                boolean visible = Settings.System.getInt(cr,
//                        Settings.System.EXPANDED_HIDE_INDICATOR, 0) != 1;
                int colorMask = res.getColor(android.R.color.holo_blue_light);

//                mIndicatorView.setVisibility(visible ? View.VISIBLE : View.GONE);

                switch (mState) {
                    case STATE_ENABLED:
                        colorMask = (colorMask & 0x00FFFFFF) | 0xA0000000;
                        break;
                    case STATE_DISABLED:
                        colorMask = (colorMask & 0x00FFFFFF) | 0x33000000;
                        break;
                    default:
                        colorMask = (colorMask & 0x00FFFFFF) | 0x60000000;
                        break;
                }

//                mIndicatorView.setImageDrawable(context.getResources().getDrawable(
//                            R.drawable.stat_bgon_custom, colorMask, MASK_MODE));
            }
        }
    };

    protected abstract void updateState(Context context);
    protected abstract void toggleState(Context context);
    protected abstract boolean handleLongClick(Context context);

    protected void update(Context context) {
        updateState(context);
        updateView();
    }

    public String[] parseStoredValue(CharSequence val) {
        if (TextUtils.isEmpty(val)) {
          return null;
        } else {
          return val.toString().split(SEPARATOR);
        }
    }

    protected void onReceive(Context context, Intent intent) {
        // do nothing as a standard, override this if the button needs to respond
        // to broadcast events from the StatusBarService broadcast receiver
    }

    protected void onChangeUri(ContentResolver resolver, Uri uri) {
        // do nothing as a standard, override this if the button needs to respond
        // to a changed setting
    }

    /* package */ void setHapticFeedback(boolean enabled,
            long[] clickPattern, long[] longClickPattern) {
        mHapticFeedback = enabled;
        mClickPattern = clickPattern;
        mLongClickPattern = longClickPattern;
    }

    protected IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }

    protected List<Uri> getObservedUris() {
        return new ArrayList<Uri>();
    }

    protected void setupButton(View view) {
        mView = view;
        if (mView != null) {
            mView.setTag(mType);
            mView.setOnClickListener(mClickListener);
            mView.setOnLongClickListener(mLongClickListener);

            mIconView = (ImageView) mView.findViewById(R.id.power_widget_button_image);
//            mIndicatorView = (ImageView) mView.findViewById(R.id.power_widget_button_indic);
            mTextView = (TextView) mView.findViewById(R.id.power_widget_text);
            android.util.Log.i("SystemUI", mTextView.toString());
            mVibrator = (Vibrator) mView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            setTextColor(TEXT_COLOR_DISABLE);
        } else {
//            mIconView = null;
//            mIndicatorView = null;
        }
    }

    protected void updateView() {
        mViewUpdateHandler.sendEmptyMessage(0);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mHapticFeedback && mClickPattern != null) {
                if (mClickPattern.length == 1) {
                    // One-shot vibration
                    mVibrator.vibrate(mClickPattern[0]);
                } else {
                    // Pattern vibration
                    mVibrator.vibrate(mClickPattern, -1);
                }
            }
            toggleState(v.getContext());
            update(v.getContext());

            if (mExternalClickListener != null) {
                mExternalClickListener.onClick(v);
            }
        }
    };
    
    public void setTextEnable(boolean enable) {
        mTextView.setVisibility(enable ? View.VISIBLE : View.GONE); 
    }

    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            boolean result = handleLongClick(v.getContext());

            if (result && mHapticFeedback && mLongClickPattern != null) {
                mVibrator.vibrate(mLongClickPattern, -1);
            }

            try {
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
            }

            if (result && mExternalLongClickListener != null) {
                mExternalLongClickListener.onLongClick(v);
            }
            //close stateBar
                final StatusBarManager statusBar = (StatusBarManager) mIconView.getContext().getSystemService(Context.STATUS_BAR_SERVICE);
                if (statusBar != null) {
                  statusBar.collapse();
                }

            return result;
        }
    };

    void setExternalClickListener(View.OnClickListener listener) {
        mExternalClickListener = listener;
    }

    void setExternalLongClickListener(View.OnLongClickListener listener) {
        mExternalLongClickListener = listener;
    }

    protected void setText(CharSequence text) {
    	if (mTextView != null) {
    	mTextView.setText(text);
    	setTextColor(mState == STATE_ENABLED ? TEXT_COLOR_ENABLE : TEXT_COLOR_DISABLE);
        }
 }
    
    protected void setText(CharSequence text, int color) {
    	if (mTextView != null) {
    	mTextView.setText(text);
    	mTextView.setTextColor(mTextView.getContext().getResources().getColor(color));
    	}
    }
    
    protected void setTextColor(int color) {
    	if (mTextView != null) {
    	mTextView.setTextColor(mTextView.getContext().getResources().getColor(color));
    	}
    }

    protected SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("PowerButton-" + mType, Context.MODE_PRIVATE);
    }
}
