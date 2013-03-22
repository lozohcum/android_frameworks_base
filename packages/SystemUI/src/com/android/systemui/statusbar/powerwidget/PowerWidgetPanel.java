/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License atj
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.powerwidget;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wimax.WimaxHelper;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.view.ViewGroup;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PowerWidgetPanel extends FrameLayout {
    private static final String TAG = "PowerWidget";

    public static final String BUTTON_DELIMITER = "|";

    //the number of each line
    private int buttonCount = 4;
    private int screenWidth;

    private static final String BUTTONS_DEFAULT = PowerButton.BUTTON_WIFI
            + BUTTON_DELIMITER + PowerButton.BUTTON_GPS
            + BUTTON_DELIMITER + PowerButton.BUTTON_BLUETOOTH
            + BUTTON_DELIMITER + PowerButton.BUTTON_AIRPLANE
            + BUTTON_DELIMITER + PowerButton.BUTTON_AUTOROTATE
            + BUTTON_DELIMITER + PowerButton.BUTTON_SOUND
            + BUTTON_DELIMITER + PowerButton.BUTTON_BRIGHTNESS
            + BUTTON_DELIMITER + PowerButton.BUTTON_MOBILEDATA
            + BUTTON_DELIMITER + PowerButton.BUTTON_NETWORKMODE
            + BUTTON_DELIMITER + PowerButton.BUTTON_SHUT
            + BUTTON_DELIMITER + PowerButton.BUTTON_REST
            + BUTTON_DELIMITER + PowerButton.BUTTON_SYNC
            + BUTTON_DELIMITER + PowerButton.BUTTON_SCREENTIMEOUT
            + BUTTON_DELIMITER + PowerButton.BUTTON_WIFIAP
            + BUTTON_DELIMITER + PowerButton.BUTTON_SLEEP;

    private static final FrameLayout.LayoutParams WIDGET_LAYOUT_PARAMS = new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, // width = match_parent
                                        ViewGroup.LayoutParams.WRAP_CONTENT  // height = wrap_content
                                        );

    private static final LinearLayout.LayoutParams EMPTY_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            1.0f);

    private LayoutParams BOTTOM_LAYOUT_PARAMS;

    private LinearLayout.LayoutParams BUTTON_LAYOUT_PARAMS;

    private LinearLayout.LayoutParams LINE_LAYOUT_PARAMS ;
    private static final int LAYOUT_SCROLL_BUTTON_THRESHOLD = 6;

    // this is a list of all possible buttons and their corresponding classes
    private static final HashMap<String, Class<? extends PowerButton>> sPossibleButtons =
            new HashMap<String, Class<? extends PowerButton>>();

    static {
        sPossibleButtons.put(PowerButton.BUTTON_WIFI, WifiButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_GPS, GPSButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_BLUETOOTH, BluetoothButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_BRIGHTNESS, BrightnessButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_SOUND, SoundButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_SYNC, SyncButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_WIFIAP, WifiApButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_SCREENTIMEOUT, ScreenTimeoutButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_MOBILEDATA, MobileDataButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_LOCKSCREEN, LockScreenButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_NETWORKMODE, NetworkModeButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_AUTOROTATE, AutoRotateButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_AIRPLANE, AirplaneButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_FLASHLIGHT, FlashlightButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_SLEEP, SleepButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_MEDIA_PLAY_PAUSE, MediaPlayPauseButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_MEDIA_PREVIOUS, MediaPreviousButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_MEDIA_NEXT, MediaNextButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_WIMAX, WimaxButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_LTE, LTEButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_REST, RebootButton.class);
        sPossibleButtons.put(PowerButton.BUTTON_SHUT, ShutdownButton.class);
    }

    // this is a list of our currently loaded buttons
    private final HashMap<String, PowerButton> mButtons = new HashMap<String, PowerButton>();
    private final ArrayList<String> mButtonNames = new ArrayList<String>();

    private View.OnClickListener mAllButtonClickListener;
    private View.OnLongClickListener mAllButtonLongClickListener;

    private Context mContext;
    private Handler mHandler;
    private LayoutInflater mInflater;
    private WidgetBroadcastReceiver mBroadcastReceiver = null;
    private WidgetSettingsObserver mObserver = null;

    private long[] mShortPressVibePattern;
    private long[] mLongPressVibePattern;

    private LinearLayout mButtonLayout;
    private HorizontalScrollView mScrollView;

    private ArrayList<View> mLine;

    public PowerWidgetPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        final float scale = context.getResources().getDisplayMetrics().density;
        final int lineHeight = (int) (78 * scale + 0.5f);
        LINE_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // width = wrap_content
                lineHeight// height = match_parent
                );
        
        LINE_LAYOUT_PARAMS.setMargins(0,
                getResources().getDimensionPixelSize(R.dimen.powerwidget_margin),
                0,
                0);

        mLine = new ArrayList<View>();
        LinearLayout newline = new LinearLayout(context);
        newline.setLayoutParams(LINE_LAYOUT_PARAMS);
        mLine.add(newline);

        mHandler = new Handler();
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mShortPressVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_virtualKeyVibePattern);
        mLongPressVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_longPressVibePattern);

        // get an initial width
        updateButtonLayoutWidth();
        setupWidget();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateVisibility();
    }

    static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i=0; i < ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    public void destroyWidget() {
        Log.i(TAG, "Clearing any old widget stuffs");
        // remove all views from the layout
        removeAllViews();

        mLine = new ArrayList<View>();
        LinearLayout newline = new LinearLayout(mContext);
        newline.setLayoutParams(LINE_LAYOUT_PARAMS);
        mLine.add(newline);

        // unregister our content receiver
        if (mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }
        // unobserve our content
        if (mObserver != null) {
            mObserver.unobserve();
        }

        // clear the button instances
        unloadAllButtons();
    }

    public void setupWidget() {

        destroyWidget();

        Log.i(TAG, "Setting up widget");

        String buttons = Settings.System.getString(mContext.getContentResolver(), Settings.System.WIDGET_BUTTONS);
        if (buttons == null) {
            Log.i(TAG, "Default buttons being loaded");
            buttons = BUTTONS_DEFAULT;
            // Add the WiMAX button if it's supported
            if (WimaxHelper.isWimaxSupported(mContext)) {
                buttons += BUTTON_DELIMITER + PowerButton.BUTTON_WIMAX;
            }
        }
        Log.i(TAG, "Button list: " + buttons);

        boolean flag = true;
        for (String button : buttons.split("\\|")) {
            if (button.equals(PowerButton.BUTTON_DRIVER)) flag = false;
            if (flag) {
                if (loadButton(button)) {
                    mButtonNames.add(button);
                    } else {
                        Log.e(TAG, "Error setting up button: " + button);
                        }
            }
        }
        recreateButtonLayout();
        updateHapticFeedbackSetting();

        // set up a broadcast receiver for our intents, based off of what our power buttons have been loaded
        setupBroadcastReceiver();
        IntentFilter filter = getMergedBroadcastIntentFilter();
        // we add this so we can update views and such if the settings for our widget change
//        filter.addAction(Settings.SETTINGS_CHANGED);
        // we need to detect orientation changes and update the static button width value appropriately
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        // register the receiver
        mContext.registerReceiver(mBroadcastReceiver, filter);
        // register our observer
        mObserver = new WidgetSettingsObserver(mHandler);
        mObserver.observe();
    }

    private boolean loadButton(String key) {
        // first make sure we have a valid button
        if (!sPossibleButtons.containsKey(key)) {
            return false;
        }

        if (mButtons.containsKey(key)) {
            return true;
        }

        try {
            // we need to instantiate a new button and add it
            PowerButton pb = sPossibleButtons.get(key).newInstance();
            pb.setExternalClickListener(mAllButtonClickListener);
            pb.setExternalLongClickListener(mAllButtonLongClickListener);
            // save it
            mButtons.put(key, pb);
        } catch (Exception e) {
            Log.e(TAG, "Error loading button: " + key, e);
            return false;
        }

        return true;
    }

    private void unloadButton(String key) {
        // first make sure we have a valid button
        if (mButtons.containsKey(key)) {
            // wipe out the button view
            mButtons.get(key).setupButton(null);
            // remove the button from our list of loaded ones
            mButtons.remove(key);
        }
    }

    private void unloadAllButtons() {
        // cycle through setting the buttons to null
        for (PowerButton pb : mButtons.values()) {
            pb.setupButton(null);
        }

        // clear our list
        mButtons.clear();
        mButtonNames.clear();
    }

    public void reloadButonLayout() {
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            buttonCount = 8;
        } else {
            buttonCount = 4;
        }

        screenWidth = getScreenWidth();
        BUTTON_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                (screenWidth / buttonCount) , 
                ViewGroup.LayoutParams.MATCH_PARENT // height = match_parent
                );

        // create a linearlayout to hold our buttons
        mButtonLayout = new LinearLayout(mContext);
        mButtonLayout.setOrientation(LinearLayout.VERTICAL);
        mButtonLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        mLine = new ArrayList<View>();
        LinearLayout currentLine = new LinearLayout(mContext);
        currentLine.setLayoutParams(LINE_LAYOUT_PARAMS);
        mLine.add(currentLine);

        for (String button : mButtonNames) {
            PowerButton pb = mButtons.get(button);
            if (pb != null) {
                View buttonView = mInflater.inflate(R.layout.power_widget_button, null, false);
                pb.setupButton(buttonView);
                pb.setTextEnable(true);
                if (currentLine.getChildCount() == buttonCount) {
//                    mButtonLayout.addView(currentLine);
                    currentLine = new LinearLayout(mContext);
                    currentLine.setLayoutParams(LINE_LAYOUT_PARAMS);
                    mLine.add(currentLine);
                } 
                buttonView.setLayoutParams(BUTTON_LAYOUT_PARAMS);
                currentLine.addView(buttonView);
//                mButtonLayout.addView(buttonView, BUTTON_LAYOUT_PARAMS);
            }
        }

        if (currentLine.getChildCount() < buttonCount) {

             LinearLayout.LayoutParams LAST_LINE_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                        (screenWidth / buttonCount), // width = screenWidth / buttonCount
                        ViewGroup.LayoutParams.WRAP_CONTENT // height = match_parent
                        );
             LAST_LINE_LAYOUT_PARAMS.setMargins(0,
                     getResources().getDimensionPixelSize(R.dimen.powerwidget_margin),
                     0,
                     0);

             for (int i = 0; i < currentLine.getChildCount(); i++) {
                 currentLine.getChildAt(i).setLayoutParams(LAST_LINE_LAYOUT_PARAMS);
             }
        }

        for (int i = 0; i < mLine.toArray().length; i++) {
            if (i < (buttonCount == 4 ? 4 : 2)) {
                mButtonLayout.addView(mLine.get(i));
            }
        }
        final float scale = mContext.getResources().getDisplayMetrics().density;
        final int lineHeight = (int) (77 * scale + 0.5f);
        BOTTOM_LAYOUT_PARAMS = new LayoutParams(LayoutParams.MATCH_PARENT, lineHeight);

        View bottomSeek = mInflater.inflate(R.layout.power_widget_brightness, null);
        mButtonLayout.addView(new View(mContext), EMPTY_LAYOUT_PARAMS);
        mButtonLayout.addView(bottomSeek, BOTTOM_LAYOUT_PARAMS);
    }

    public void recreateButtonLayout() {
        removeAllViews();

        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            buttonCount = 8;
        } else {
            buttonCount = 4;
        }

        screenWidth = getScreenWidth();
        BUTTON_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                (screenWidth / buttonCount) , 
                ViewGroup.LayoutParams.MATCH_PARENT // height = match_parent
                );

        // create a linearlayout to hold our buttons
        mButtonLayout = new LinearLayout(mContext);
        mButtonLayout.setOrientation(LinearLayout.VERTICAL);
        mButtonLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        final float scale = mContext.getResources().getDisplayMetrics().density;
        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            mButtonLayout.addView(new View(mContext),
                new LayoutParams(LayoutParams.MATCH_PARENT, (int) (16 * scale + 0.5f)));
        }

        mLine = new ArrayList<View>();
        LinearLayout currentLine = new LinearLayout(mContext);
        currentLine.setLayoutParams(LINE_LAYOUT_PARAMS);
        mLine.add(currentLine);

        for (String button : mButtonNames) {
            PowerButton pb = mButtons.get(button);
            if (pb != null) {
                View buttonView = mInflater.inflate(R.layout.power_widget_button, null, false);
                pb.setupButton(buttonView);
                pb.setTextEnable(true);
                if (currentLine.getChildCount() == buttonCount) {
//                    mButtonLayout.addView(currentLine);
                    currentLine = new LinearLayout(mContext);
                    currentLine.setLayoutParams(LINE_LAYOUT_PARAMS);
                    mLine.add(currentLine);
                } 
                buttonView.setLayoutParams(BUTTON_LAYOUT_PARAMS);
                currentLine.addView(buttonView);
//                mButtonLayout.addView(buttonView, BUTTON_LAYOUT_PARAMS);
            }
        }

        // we determine if we're using a horizontal scroll view based on a threshold of button counts
//        if (mButtonLayout.getChildCount() > LAYOUT_SCROLL_BUTTON_THRESHOLD) {
//            // we need our horizontal scroll view to wrap the linear layout
//            mScrollView = new HorizontalScrollView(mContext);
//            // make the fading edge the size of a button (makes it more noticible that we can scroll
//            mScrollView.setFadingEdgeLength(mContext.getResources().getDisplayMetrics().widthPixels / LAYOUT_SCROLL_BUTTON_THRESHOLD);
//            mScrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
//            mScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
//            mScrollView.addView(mButtonLayout, WIDGET_LAYOUT_PARAMS);
//            updateScrollbar();
//            addView(mScrollView, WIDGET_LAYOUT_PARAMS);
//        } else {
            // not needed, just add the linear layout

        if (currentLine.getChildCount() < buttonCount) {

             LinearLayout.LayoutParams LAST_LINE_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                        (screenWidth / buttonCount), // width = screenWidth / buttonCount
                        ViewGroup.LayoutParams.WRAP_CONTENT // height = match_parent
                        );

             for (int i = 0; i < currentLine.getChildCount(); i++) {
                 currentLine.getChildAt(i).setLayoutParams(LAST_LINE_LAYOUT_PARAMS);
             }
        }

        for (int i = 0; i < mLine.toArray().length; i++) {
            if (i < (buttonCount == 4 ? 4 : 2)) {
                mButtonLayout.addView(mLine.get(i));
            }
        }

        final int lineHeight = (int) (66 * scale + 0.5f);
        BOTTOM_LAYOUT_PARAMS = new LayoutParams(LayoutParams.MATCH_PARENT, lineHeight);

        View bottomSeek = mInflater.inflate(R.layout.power_widget_brightness, null);
        mButtonLayout.addView(new View(mContext), EMPTY_LAYOUT_PARAMS);
        mButtonLayout.addView(bottomSeek, BOTTOM_LAYOUT_PARAMS);

            addView(mButtonLayout, WIDGET_LAYOUT_PARAMS);

            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,
                    getResources().getDimensionPixelSize(R.dimen.button_layout_margin),
                    0,
                    0);
            mButtonLayout.setLayoutParams(lp);
//        }
    }

    public void updateAllButtons() {
        // cycle through our buttons and update them
        for (PowerButton pb : mButtons.values()) {
            pb.update(mContext);
        }
    }

    private IntentFilter getMergedBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();

        for (PowerButton button : mButtons.values()) {
            IntentFilter tmp = button.getBroadcastIntentFilter();

            // cycle through these actions, and see if we need them
            int num = tmp.countActions();
            for (int i = 0; i < num; i++) {
                String action = tmp.getAction(i);
                if(!filter.hasAction(action)) {
                    filter.addAction(action);
                }
            }
        }

        // return our merged filter
        return filter;
    }

    private List<Uri> getAllObservedUris() {
        List<Uri> uris = new ArrayList<Uri>();

        for (PowerButton button : mButtons.values()) {
            List<Uri> tmp = button.getObservedUris();

            for (Uri uri : tmp) {
                if (!uris.contains(uri)) {
                    uris.add(uri);
                }
            }
        }

        return uris;
    }

    public void setGlobalButtonOnClickListener(View.OnClickListener listener) {
        mAllButtonClickListener = listener;
        for (PowerButton pb : mButtons.values()) {
            pb.setExternalClickListener(listener);
        }
    }

    public void setGlobalButtonOnLongClickListener(View.OnLongClickListener listener) {
        mAllButtonLongClickListener = listener;
        for (PowerButton pb : mButtons.values()) {
            pb.setExternalLongClickListener(listener);
        }
    }

    private void setupBroadcastReceiver() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new WidgetBroadcastReceiver();
        }
    }

    private void updateButtonLayoutWidth() {
        // use our context to set a valid button width
//        BUTTON_LAYOUT_PARAMS.width = mContext.getResources().getDisplayMetrics().widthPixels / LAYOUT_SCROLL_BUTTON_THRESHOLD;
    }

    public void updateVisibility() {
        // now check if we need to display the widget still
        boolean displayPowerWidget = Settings.System.getInt(mContext.getContentResolver(),
                   Settings.System.EXPANDED_VIEW_WIDGET, 1) == 1;
        if(!displayPowerWidget) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }

    private void updateScrollbar() {
        if (mScrollView == null) return;
        boolean hideScrollBar = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.EXPANDED_HIDE_SCROLLBAR, 0) == 1;
        mScrollView.setHorizontalScrollBarEnabled(!hideScrollBar);

        // set the padding on the linear layout to the size of our scrollbar,
        // so we don't have them overlap
        // need to be here for make use of EXPANDED_HIDE_SCROLLBAR, expanding or collapsing
        // the space used by the scrollbar
        if (mButtonLayout != null) {
            mButtonLayout.setPadding(0, 0, 0,
                    !hideScrollBar ? mScrollView.getVerticalScrollbarWidth() : 0);
        }
    }

    private void updateHapticFeedbackSetting() {
        ContentResolver cr = mContext.getContentResolver();
        int expandedHapticFeedback = Settings.System.getInt(cr,
                Settings.System.EXPANDED_HAPTIC_FEEDBACK, 2);
        long[] clickPattern = null, longClickPattern = null;
        boolean hapticFeedback;

        if (expandedHapticFeedback == 2) {
             hapticFeedback = Settings.System.getInt(cr,
                     Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
        } else {
            hapticFeedback = (expandedHapticFeedback == 1);
        }

        if (hapticFeedback) {
            clickPattern = mShortPressVibePattern;
            longClickPattern = mLongPressVibePattern;
        }

        for (PowerButton button : mButtons.values()) {
            button.setHapticFeedback(hapticFeedback, clickPattern, longClickPattern);
        }
    }

    // our own broadcast receiver :D
    private class WidgetBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                updateButtonLayoutWidth();
                recreateButtonLayout();
            } else {
                // handle the intent through our power buttons
                for (PowerButton button : mButtons.values()) {
                    // call "onReceive" on those that matter
                    if (button.getBroadcastIntentFilter().hasAction(action)) {
                        button.onReceive(context, intent);
                    }
                }
            }

            // update our widget
            updateAllButtons();
        }
    };

    // our own settings observer :D
    private class WidgetSettingsObserver extends ContentObserver {
        public WidgetSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            // watch for display widget
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.EXPANDED_VIEW_WIDGET),
                            false, this);

            // watch for scrollbar hiding
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.EXPANDED_HIDE_SCROLLBAR),
                            false, this);

            // watch for haptic feedback
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.EXPANDED_HAPTIC_FEEDBACK),
                            false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED),
                            false, this);

            // watch for changes in buttons
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.WIDGET_BUTTONS),
                            false, this);

//            // watch for changes in color
//            resolver.registerContentObserver(
//                    Settings.System.getUriFor(Settings.System.EXPANDED_VIEW_WIDGET_COLOR),
//                            false, this);
//
//            // watch for changes in indicator visibility
//            resolver.registerContentObserver(
//                    Settings.System.getUriFor(Settings.System.EXPANDED_HIDE_INDICATOR),
//                            false, this);

            // watch for power-button specifc stuff that has been loaded
            for(Uri uri : getAllObservedUris()) {
                resolver.registerContentObserver(uri, false, this);
            }
        }

        public void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = mContext.getContentResolver();
            Resources res = mContext.getResources();

            // first check if our widget buttons have changed
            if(uri.equals(Settings.System.getUriFor(Settings.System.WIDGET_BUTTONS))) {
                setupWidget();
            // now check if we change visibility
            } else if(uri.equals(Settings.System.getUriFor(Settings.System.EXPANDED_VIEW_WIDGET))) {
                updateVisibility();
            // now check for scrollbar hiding
            } else if(uri.equals(Settings.System.getUriFor(Settings.System.EXPANDED_HIDE_SCROLLBAR))) {
                // Needed to remove scrollview to gain the space of the scrollable area
                recreateButtonLayout();
            }

            if (uri.equals(Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED))
                    || uri.equals(Settings.System.getUriFor(Settings.System.EXPANDED_HAPTIC_FEEDBACK))) {
                updateHapticFeedbackSetting();
            }

            // do whatever the individual buttons must
            for (PowerButton button : mButtons.values()) {
                if (button.getObservedUris().contains(uri)) {
                    button.onChangeUri(resolver, uri);
                }
            }

            // something happened so update the widget
            updateAllButtons();
        }
    }

    private int getScreenWidth() {
        DisplayMetrics dm = mContext.getApplicationContext().getResources().getDisplayMetrics();
        return dm.widthPixels;
    }
}
