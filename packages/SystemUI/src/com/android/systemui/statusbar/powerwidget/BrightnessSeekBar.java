
package com.android.systemui.statusbar.powerwidget;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class BrightnessSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {

    private int mOldBrightness;
    private int mOldAutomatic;

    private boolean mAutomaticAvailable;

    public static int currentBrightness;

    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    private static final int MINIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_DIM + 10;
    private static final int MAXIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_ON;

    private static final Uri BRIGHTNESS_URI = Settings.System
            .getUriFor(Settings.System.SCREEN_BRIGHTNESS);
    private static final Uri BRIGHTNESS_MODE_URI = Settings.System
            .getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(BRIGHTNESS_URI);
        OBSERVED_URIS.add(BRIGHTNESS_MODE_URI);
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.LIGHT_SENSOR_CUSTOM));
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.LIGHT_SCREEN_DIM));
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.EXPANDED_BRIGHTNESS_MODE));
    }

    public BrightnessSeekBar(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public BrightnessSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();

        updateProgress();
        setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
        setBrightness(progress + MINIMUM_BACKLIGHT);
        currentBrightness = progress + MINIMUM_BACKLIGHT;

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        setValeToSystem();
    }

    private void setBrightness(int brightness) {
        try {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                power.setBacklightBrightness(brightness);
            }
        } catch (RemoteException doe) {

        }
    }

    private void setValeToSystem() {

        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, getProgress()
                        + MINIMUM_BACKLIGHT);
    }

    private void updateProgress() {
        setMax(MAXIMUM_BACKLIGHT - MINIMUM_BACKLIGHT);
        try {
            mOldBrightness = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException snfe) {
            mOldBrightness = MAXIMUM_BACKLIGHT;
        }
        setProgress(mOldBrightness - MINIMUM_BACKLIGHT);
        currentBrightness = getProgress();
        
        boolean mAutoBrightness = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        
        this.setEnabled(!mAutoBrightness);
    }

    private class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < OBSERVED_URIS.size(); i++) {
                resolver.registerContentObserver(OBSERVED_URIS.get(i), false, this);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub
            updateProgress();
        }

    }
}
