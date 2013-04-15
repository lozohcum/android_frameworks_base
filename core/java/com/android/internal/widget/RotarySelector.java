/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.internal.widget;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import static android.view.animation.AnimationUtils.currentAnimationTimeMillis;
import com.android.internal.R;


/**
 * Custom view that presents up to two items that are selectable by rotating a semi-circle from
 * left to right, or right to left.  Used by incoming call screen, and the lock screen when no
 * security pattern is set.
 */
public class RotarySelector extends View {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private static final String LOG_TAG = "RotarySelector";
    private static final boolean DBG = false;
    private static final boolean VISUAL_DEBUG = false;

    // Listener for onDialTrigger() callbacks.
    private OnDialTriggerListener mOnDialTriggerListener;

    private float mDensity;
    private int mDownPart=1;
    
    // Stores a scale factor for user modified density via build.prop
    private float mDensityScaleFactor=1;

    // UI elements
    private Bitmap mBackground;
    private Bitmap mBackground_Click;
    private Bitmap mDimple;
    private Bitmap mDimpleDim;
    private Bitmap mCallBitmap;
    private Bitmap mMmsBitmap;
    private Bitmap mNumBitmap;
    private Bitmap mClickBitmap;
    private Bitmap mAnimation_0;
    private Bitmap mAnimation_1;
    private Bitmap mAnimation_2;
    private Bitmap mAnimation_3;
    private Bitmap mAnimation_4;
    private Bitmap[] mAnimationBitmaps;
    private Bitmap mLeftHandleIcon;
    private Bitmap mRightHandleIcon;
    private Bitmap mMidHandleIcon;
    private boolean mIsShowAnimation=true;
    private boolean mIsDown=false;
    private boolean mUnLock=false;

    private Bitmap mArrowShortLeftAndRight;
    private Bitmap mArrowLongLeft;  // Long arrow starting on the left, pointing clockwise
    private Bitmap mArrowLongRight;  // Long arrow starting on the right, pointing CCW
    private Bitmap mArrowDown;  // Down arrow for middle handle

    // positions of the left and right handle
    private int mLeftHandleX;
    private int mRightHandleX;
    private int mMidHandleX;

    // current offset of rotary widget along the x axis
    private int mRotaryOffsetX = 0;
    // current offset of rotary widget along the y axis - added to pull middle dimple down
    private int mRotaryOffsetY = 0;
    // saves the initial Y value on ACTION_DOWN
    private int mEventStartY;
    // controls display of custom app dimple
    private boolean mCustomAppDimple=false;
    // size of the status bar for resizing the background
    private int mStatusBarSize=0;
    // backgrond Scale for landscape mode with status bar in our way
    private float mStatusBarScale=1;
    // controls hiding of directional arrows
    private boolean mHideArrows=false;
    // are we in lense mode?
    private boolean mLenseMode=false;
    // are we in rotary revamped mode?
    private boolean mRevampedMode=false;

    // state of the animation used to bring the handle back to its start position when
    // the user lets go before triggering an action
    private boolean mAnimating = false;
    private boolean mAnimatingUp = false;
    private long mAnimationStartTime;
    private long mAnimationDuration;
    private int mAnimatingDeltaXStart;   // the animation will interpolate from this delta to zero
    private int mAnimatingDeltaXEnd;
    private int mAnimatingDeltaYStart;
    private int mAnimatingDeltaYEnd;

    private DecelerateInterpolator mInterpolator;

    private Paint mPaint = new Paint();
    private Paint mLensePaint = new Paint ();

    // used to rotate the background and arrow assets depending on orientation
    final Matrix mBgMatrix = new Matrix();
    final Matrix mArrowMatrix = new Matrix();
    final Matrix drawMatrix = new Matrix();

    /**
     * If the user is currently dragging something.
     */
    private int mGrabbedState = NOTHING_GRABBED;
    public static final int NOTHING_GRABBED = 0;
    public static final int LEFT_HANDLE_GRABBED = 1;
    public static final int MID_HANDLE_GRABBED = 2;
    public static final int RIGHT_HANDLE_GRABBED = 3;

    /**
     * Static status bar sizes for ldpi/mdpi/hdpi
     */
    private static final int STATUS_BAR_HEIGHT_LDPI = 19;
    private static final int STATUS_BAR_HEIGHT_MDPI = 25;
    private static final int STATUS_BAR_HEIGHT_HDPI = 38;

    /**
     * Whether the user has triggered something (e.g dragging the left handle all the way over to
     * the right).
     */
    private boolean mTriggered = false;

    // Vibration (haptic feedback)
    private Vibrator mVibrator;

    /**
     * The drawable for the arrows need to be scrunched this many dips towards the rotary bg below
     * it.
     */
    private static final int ARROW_SCRUNCH_DIP = 6;

    /**
     * How far inset the left and right circles should be
     */
    private static final int EDGE_PADDING_DIP = 9;

    /**
     * How far from the edge of the screen the user must drag to trigger the event.
     */
    private static final int EDGE_TRIGGER_DIP = 100;

    /**
     * Dimensions of arc in background drawable.
     */
    static final int SNAP_BACK_ANIMATION_DURATION_MILLIS = 300;
    static final int SPIN_ANIMATION_DURATION_MILLIS = 800;
    static final int LENSE_DATE_SIZE_DIP = 16;
    static final int LENSE_TIME_SIZE_DIP = 60;
    static final int LENSE_DAY_SIZE_DIP = 64;

    private int mEdgeTriggerThresh;
    private int mDimpleWidth;
    private int mBackgroundWidth;
    private int mBackgroundHeight;
    private final int mRotaryOuterRadiusDIP;
    private final int mRotaryStrokeWidthDIP;
    private final int mOuterRadius;
    private final int mInnerRadius;
    private int mDimpleSpacing;

    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private long mMaxAnimationDuration;

    /**
     * The number of dimples we are flinging when we do the "spin" animation.  Used to know when to
     * wrap the icons back around so they "rotate back" onto the screen.
     * @see #updateAnimation()
     */
    private int mDimplesOfFling = 0;

    /**
     * Either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    private int mOrientation;

    /**
     * the deleted Margin from xml layout - fixes clipping
     */
    private int mMarginBottom=0;
    private int mMarginBottomAdd=0;

    private String mDateFormatString;
    
    private boolean showTipTx = true;//add show xiang xia hua dong wen ben

    private Bitmap weathIcon;
    private String condition;
    private String temp;
    private boolean weatherState;
    
    /**
     * lock srceen weather info
     * @param condition2
     * @param temp2
     * @param weathIcon2 
     */
    public void setWeatherInfo(String condition2, String temp2, Bitmap weathIcon2) {
        if(DBG)Log.d(LOG_TAG, "....RotarySelector.java...... setWeatherInfo().......");
        
        if(condition2 == null || temp2 == null || weathIcon2 == null){
            weatherState = false;
        }else{
            weatherState = true;
            weathIcon = weathIcon2;
            condition = condition2;
            temp = temp2;
        }
        mRotaryOffsetX = 0;
        mRotaryOffsetY = 0;
        setGrabbedState(NOTHING_GRABBED);
        showTipTx = true;
        invalidate();
    };
    
    public RotarySelector(Context context) {
        this(context, null);
    }

    /**
     * Constructor used when this widget is created from a layout file.
     */
    public RotarySelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.RotarySelector);
        mOrientation = a.getInt(R.styleable.RotarySelector_orientation, HORIZONTAL);
        a.recycle();
        
        mContentResolver = context.getContentResolver();
        
        registerObserver();
        int temp=shenduGetMissCallCount();
        if (temp!=-1) {//err
        	  mCallNum=temp;
		}
        mMmsNum=shenduGetUnreadMMSCount();
        Resources r = getResources();
        mDensity = r.getDisplayMetrics().density;
        int densityDpi;
        densityDpi = r.getDisplayMetrics().densityDpi;

        /*
         * this hack assumes people change build.prop for increasing
         * the virtual size of their screen by decreasing dpi in
         * build.prop file. this is often done especially for hd
         * phones. keep in mind changing build.prop and density
         * isnt officially supported, but this should do for most cases
         */
        if(densityDpi < 240 && densityDpi >180)
            mDensityScaleFactor=(float)(240.0 / densityDpi);
        if(densityDpi < 160 && densityDpi >120)
            mDensityScaleFactor=(float)(160.0 / densityDpi);

        if (DBG) log("- Density: " + mDensity);

        // Assets (all are BitmapDrawables).
        mBackground = getBitmapFor(R.drawable.jog_dial_bg);
        mDimple = getBitmapFor(R.drawable.jog_dial_dimple);
        mDimpleDim = getBitmapFor(R.drawable.jog_dial_dimple_dim);
        //add by shendu liuchaun
        mBackground_Click = getBitmapFor(R.drawable.lock_bg_click);
        mCallBitmap = getBitmapFor(R.drawable.tel);
        mMmsBitmap = getBitmapFor(R.drawable.sms);
        mNumBitmap = getBitmapFor(R.drawable.num_bg);
        mClickBitmap = getBitmapFor(R.drawable.tel_sms_click_bg);
        mAnimation_0 = getBitmapFor(R.drawable.d0);
        mAnimation_1 = getBitmapFor(R.drawable.d1);
        mAnimation_2 = getBitmapFor(R.drawable.d2);
        mAnimation_3 = getBitmapFor(R.drawable.d3);
        mAnimation_4 = getBitmapFor(R.drawable.d4);
        mAnimationBitmaps=new Bitmap[]{mAnimation_0,mAnimation_1,mAnimation_2,mAnimation_3,mAnimation_4};
        
        mArrowLongLeft = getBitmapFor(R.drawable.jog_dial_arrow_long_left_green);
        mArrowLongRight = getBitmapFor(R.drawable.jog_dial_arrow_long_right_red);
        mArrowDown = getBitmapFor(R.drawable.jog_dial_arrow_short_down_green);
        mArrowShortLeftAndRight = getBitmapFor(R.drawable.jog_dial_arrow_short_left_and_right);

        mInterpolator = new DecelerateInterpolator(1f);

        mEdgeTriggerThresh = (int) (mDensity * EDGE_TRIGGER_DIP);

        mDimpleWidth = mDimple.getWidth();

        mBackgroundWidth = mBackground.getWidth();
        mBackgroundHeight = mBackground.getHeight();

        mRotaryOuterRadiusDIP = context.getResources().getInteger(R.integer.config_rotaryOuterRadiusDIP);
        mRotaryStrokeWidthDIP = context.getResources().getInteger(R.integer.config_rotaryStrokeWidthDIP);
        mOuterRadius = (int) (mDensity * mDensityScaleFactor * mRotaryOuterRadiusDIP);
        mInnerRadius = (int) ((mRotaryOuterRadiusDIP - mRotaryStrokeWidthDIP) * mDensity * mDensityScaleFactor);

        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mMaxAnimationDuration = 1000;

        int marginBottomDIP = context.getResources().getInteger(R.integer.config_rotaryMarginBottomDIP);
        mMarginBottom = (int)(marginBottomDIP * mDensity * mDensityScaleFactor);
        mMarginBottomAdd= (int)((marginBottomDIP-60) * mDensity * mDensityScaleFactor);
        mLensePaint.setColor(Color.WHITE);
        mLensePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLensePaint.setTextAlign(Paint.Align.LEFT);
        mLensePaint.setFlags(Typeface.BOLD);
        
        // get status bar size in every possible orientation
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);

        switch (displayMetrics.densityDpi) {
            case DisplayMetrics.DENSITY_HIGH:
                mStatusBarSize = STATUS_BAR_HEIGHT_HDPI;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                mStatusBarSize = STATUS_BAR_HEIGHT_MDPI;
                break;
            case DisplayMetrics.DENSITY_LOW:
                mStatusBarSize = STATUS_BAR_HEIGHT_LDPI;
                break;
            default:
                mStatusBarSize = STATUS_BAR_HEIGHT_HDPI;
        }

        // set up the scale in landscape mode
        if(!isHoriz()){
            mStatusBarScale = (float) ((mBackgroundWidth - mStatusBarSize) / (float)mBackgroundWidth);
        }
        
        mDateFormatString = context.getString(R.string.abbrev_wday_month_day_no_year1);
        mVibrator = (android.os.Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        showAnimation();
    }

    
    private void recycleBitmap(){
    	//add by shendu liuchuan  gai--
    	if(mBackground_Click!=null){
    		mBackground_Click.recycle();
    		mBackground_Click = null;
    	}
    	if(mCallBitmap!=null){
    		mCallBitmap.recycle();
    		mCallBitmap = null;
    	}
    	if(mMmsBitmap!=null){
    		mMmsBitmap.recycle();
    		mMmsBitmap = null;
    	}
    	if(mNumBitmap!=null){
    		mNumBitmap.recycle();
    		mNumBitmap = null;
    	}
    	if(mClickBitmap!=null){
    		mClickBitmap.recycle();
    		mClickBitmap = null;
    	}
    	if(mAnimation_0!=null){
    		mAnimation_0.recycle();
    		mAnimation_0 = null;
    	}
    	if(mAnimation_1!=null){
    		mAnimation_1.recycle();
    		mAnimation_1 = null;
    	}
    	if(mAnimation_2!=null){
    		mAnimation_2.recycle();
    		mAnimation_2 = null;
    	}
    	if(mAnimation_3!=null){
    		mAnimation_3.recycle();
    		mAnimation_3 = null;
    	}
    	if(mAnimation_4!=null){
    		mAnimation_4.recycle();
    		mAnimation_4 = null;
    	}
    	if(mAnimationBitmaps!=null){
    		mAnimationBitmaps = null;
    	}
    	
    	
    	if(mBackground!=null){
    		mBackground.recycle();
    		mBackground = null;
    	}
    	if(mDimple!=null){
    		mDimple.recycle();
    		mDimple = null;
    	}
    	if(mDimpleDim!=null){
    		mDimpleDim.recycle();
    		mDimpleDim = null;
    	}
    	if(mBackground_Click!=null){
    		mBackground_Click.recycle();
    		mBackground_Click = null;
    	}
    	if(mCallBitmap!=null){
    		mCallBitmap.recycle();
    		mCallBitmap = null;
    	}
    	if(mMmsBitmap!=null){
    		mMmsBitmap.recycle();
    		mMmsBitmap = null;
    	}
    	if(mNumBitmap!=null){
    		mNumBitmap.recycle();
    		mNumBitmap = null;
    	}
    	if(mClickBitmap!=null){
    		mClickBitmap.recycle();
    		mClickBitmap = null;
    	}
    	if(mClickBitmap!=null){
    		mClickBitmap.recycle();
    		mClickBitmap = null;
    	}
    	if(mAnimationBitmaps!=null){
	    	for(int i = 0;i < mAnimationBitmaps.length ; i++){
	    		mAnimationBitmaps[i].recycle();
	    		mAnimationBitmaps[i] = null;
	    	}	
	    	mAnimationBitmaps = null;
    	}
    	if(mAnimation_0!=null){
    		mAnimation_0.recycle();
    		mAnimation_0 = null;
    	}
    	if(mAnimation_1!=null){
    		mAnimation_1.recycle();
    		mAnimation_1 = null;
    	}
    	if(mAnimation_2!=null){
    		mAnimation_2.recycle();
    		mAnimation_2 = null;
    	}
    	if(mAnimation_3!=null){
    		mAnimation_3.recycle();
    		mAnimation_3 = null;
    	}
    	if(mAnimation_4!=null){
    		mAnimation_4.recycle();
    		mAnimation_4 = null;
    	}
    	
    	if(mArrowLongLeft!=null){
    		mArrowLongLeft.recycle();
    		mArrowLongLeft = null;
    	}
    	if(mArrowLongRight!=null){
    		mArrowLongRight.recycle();
    		mArrowLongRight = null;
    	}
    	if(mArrowDown!=null){
    		mArrowDown.recycle();
    		mArrowDown = null;
    	}
     	if(mArrowShortLeftAndRight!=null){
     		mArrowShortLeftAndRight.recycle();
     		mArrowShortLeftAndRight = null;
    	}
    	
    	System.gc();
    }
    
    private Bitmap getBitmapFor(int resId) {
        return BitmapFactory.decodeResource(getContext().getResources(), resId);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        final int edgePadding = (int) (EDGE_PADDING_DIP * mDensity);
        mLeftHandleX = edgePadding + mDimpleWidth / 2;
        final int length = isHoriz() ? w : h;
        mRightHandleX = length - edgePadding - mDimpleWidth / 2;
        mMidHandleX = length / 2;
        mDimpleSpacing = (length / 2) - mLeftHandleX;

        // bg matrix only needs to be calculated once
        mBgMatrix.setTranslate(0, 0);
        mBgMatrix.postScale(mDensityScaleFactor, mDensityScaleFactor);
        if (!isHoriz()) {
            // set up matrix for translating drawing of background and arrow assets
            final int left = w - mBackgroundHeight;
            mBgMatrix.preRotate(-90, 0, 0);
            if(mLenseMode){
                mBgMatrix.postTranslate(left, h + mStatusBarSize);
                mBgMatrix.postScale(1, mStatusBarScale);
            }else
                mBgMatrix.postTranslate(left, h);
        } else {
            mBgMatrix.postTranslate(0, h - mBackgroundHeight);
        }
    }

    private boolean isHoriz() {
        return mOrientation == HORIZONTAL;
    }

    /**
     * Sets the left handle icon to a given resource.
     *
     * The resource should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param resId the resource ID.
     */
    public void setLeftHandleResource(int resId) {
        if (resId != 0) {
            mLeftHandleIcon = getBitmapFor(resId);
        }
        invalidate();
    }

    /**
     * Sets the left handle icon to a given drawable.
     *
     * The argument should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param icon Bitmap object.
     */
    public void setLeftHandleResource(Bitmap icon) {
        mLeftHandleIcon=icon;
        invalidate();
    }

    /**
     * Sets the right handle icon to a given resource.
     *
     * The resource should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param resId the resource ID.
     */
    public void setRightHandleResource(int resId) {
        if (resId != 0) {
            mRightHandleIcon = getBitmapFor(resId);
        }
        invalidate();
    }

    /**
     * Sets the middle handle icon to a given resource.
     *
     * The resource should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param resId the resource ID.
     */
    public void setMidHandleResource(int resId) {
        if (resId != 0) {
            mMidHandleIcon = getBitmapFor(resId);
        }
        invalidate();
    }

    /**
     * Sets the middle handle icon to a given drawable.
     *
     * The argument should refer to a Drawable object, or use 0 to remove
     * the icon.
     *
     * @param icon Bitmap object.
     */
    public void setMidHandleResource(Bitmap icon) {
        mMidHandleIcon=icon;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int length = isHoriz() ?
                MeasureSpec.getSize(widthMeasureSpec) :
                MeasureSpec.getSize(heightMeasureSpec);
        final int arrowScrunch = (int) (ARROW_SCRUNCH_DIP * mDensity);
        final int arrowH = mArrowShortLeftAndRight.getHeight();

        // by making the height less than arrow + bg, arrow and bg will be scrunched together,
        // overlaying somewhat (though on transparent portions of the drawable).
        // this works because the arrows are drawn from the top, and the rotary bg is drawn
        // from the bottom.

        final int height = mBackgroundHeight + arrowH - arrowScrunch + mMarginBottom;

        if (isHoriz()) {
            setMeasuredDimension(length, height);
        } else {
            setMeasuredDimension(height, length);
        }
    }
 
    		
    //add by shendu liuchuan
    public static final int DOWNL=0;
    public static final int DOWNM=1;
    public static final int DOWNR=2;
    private int mCallNum=0;
    private int mMmsNum=0;
	private ContentResolver mContentResolver;
	private ContentObserver mCallInfoObserver = new ContentObserver(
			new Handler()) {
		public void onChange(boolean selfChange) {
			int temp=shenduGetMissCallCount();
			if (temp!=-1) {
				mCallNum=temp;
			}
		}
	};

	private ContentObserver mSMSObserver = new ContentObserver(
			new Handler()) {
		public void onChange(boolean selfChange) {
			mMmsNum=shenduGetUnreadMMSCount();
		}
	};
    public int shenduGetUnreadMMSCount() {
		int result;
		ContentResolver cr = getContext().getContentResolver();
		Cursor cursorUnreadSms = cr.query(Uri.parse("content://sms/inbox"),
				null, "read = 0", null, null);
		Cursor cursorUnreadMms = cr.query(Uri.parse("content://mms/inbox"),
				null, "read = 0", null, null);
		result = cursorUnreadSms.getCount() + cursorUnreadMms.getCount();
		cursorUnreadSms.close();
		cursorUnreadMms.close();
		return result;
	}

	public int shenduGetMissCallCount() {
		int result;
		ContentResolver phoneCR = getContext().getContentResolver();
		Cursor cursorMissed = phoneCR.query(Calls.CONTENT_URI, null, Calls.TYPE
				+ "=" + Calls.MISSED_TYPE + " and " + Calls.NEW + " = 1", null,
				null);
          if (cursorMissed==null) {
			return -1;//err
		 }
		 result = cursorMissed.getCount();
		 cursorMissed.close();
		 return result;
	}
	public void registerObserver() {
		// Register for changes to the call info
         
		mContentResolver.registerContentObserver(Calls.CONTENT_URI, true,
				mCallInfoObserver);

		// Register for changes to the sms and mms info
		mContentResolver.registerContentObserver(
				Uri.parse("content://mms-sms/conversations"), true,
				mSMSObserver);

	}

	public void unRegisterObserver() {

		mContentResolver.unregisterContentObserver(mCallInfoObserver);

		mContentResolver.unregisterContentObserver(mSMSObserver);

	}
    private int downSwitch(int x) {
    	int partWidth=mBackgroundWidth/5;
    	if (x<=partWidth) {
			return DOWNL;
		}else if (x>partWidth&&x<partWidth*4) {
			return DOWNM;
		}else {
			return DOWNR;
		}
	}
    int i=0;
    private Runnable mAnimationRunnable;
    private void  showAnimation() {
    	mAnimationRunnable=new Runnable() {
    		
    		@Override
    		public void run() {
    			// TODO 自动生成的方法存根
    			while (mIsShowAnimation) {
    				if (i==4) {
    					i=0;
    				}else {
    					i=i+1;
    				}
    				RotarySelector.this.postInvalidate();
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
				}
    		}
    	
    	};
    	new Thread(mAnimationRunnable).start();
	}
    

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(DBG)Log.d(LOG_TAG, "..............RotarySelector.java.........onDraw().......");
        mLensePaint.setTextAlign(Paint.Align.LEFT);//left dui qi
        
        final int width = getWidth();
        final int height = getHeight();
        final int bgHeight = mBackgroundHeight;
        final int bgTop = isHoriz() ?
                height - bgHeight:
                width - bgHeight;
        final int halfdimple = mDimpleWidth / 2;

        if (VISUAL_DEBUG) {
            // draw bounding box around widget
            mPaint.setColor(0xffff0000);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0, 0, width, getHeight(), mPaint);
        }

        // update animating state before we draw anything
        if (mAnimating || mAnimatingUp) {
            updateAnimation();
        }

        // Background:
        drawMatrix.set(mBgMatrix);
        if (isHoriz())
            drawMatrix.postTranslate(0, (float) mRotaryOffsetY - mMarginBottom);
        else
            drawMatrix.postTranslate((float) mRotaryOffsetY - mMarginBottom, 0);
        canvas.drawBitmap(mBackground, drawMatrix, mPaint);

        // for lense mode, we are done after time and date
        if(mLenseMode){
            if(isHoriz()){
                Date now = new Date();
                String timeString = DateFormat.getTimeFormat(mContext).format(now).toString();
                //String dateString = DateFormat.format(mDateFormatString, now).toString();
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                Date curDate = new Date(System.currentTimeMillis());// huo qu dang qian shi jian 
                timeString = formatter.format(curDate);
                
//                SimpleDateFormat formatter1 = new SimpleDateFormat("dd");
                
                String dateString = DateFormat.format(mDateFormatString, curDate).toString();
                Calendar c = Calendar.getInstance();
//                String dayString = String.valueOf(c.get(Calendar.DAY_OF_MONTH)).trim();
//                String dayString = formatter1.format(curDate);
                //Add offset specified in config file
                int customTimeOffset = getContext().getResources().getInteger(R.integer.config_lenseTimeLabelOffsetDIP);
                canvas.translate(0, 0);
               float dateSize= LENSE_DATE_SIZE_DIP * mDensity * mDensityScaleFactor;
                mLensePaint.setTextSize(dateSize/*LENSE_DATE_SIZE_DIP * mDensity * mDensityScaleFactor*/);
                mLensePaint.setTypeface(null);
                mLensePaint.setAlpha(160);
                canvas.drawText(dateString,  mBackgroundWidth / 2 * mDensityScaleFactor-dateSize*(dateString.length()-3)/2
                        /* mBackgroundWidth / 10 * mDensityScaleFactor */, mRotaryOffsetY + mMarginBottom-mMarginBottomAdd
                      + LENSE_DAY_SIZE_DIP * mDensity - 20 + (9 * mDensity * mDensityScaleFactor) + customTimeOffset, mLensePaint);
                //changed by shendu liuchuan
//                mLensePaint.setTextSize(LENSE_DAY_SIZE_DIP * mDensity * mDensityScaleFactor/*LENSE_DAY_SIZE_DIP * mDensity * mDensityScaleFactor * 2 + 8*/);
//                //setting zi ding yi zi ti yang shi 
//                try{//if this fonts is not exist,catch runtime Exception
//                    mLensePaint.setTypeface(Typeface.createFromFile("/system/fonts/HelveticaNeueLTPro-Lt.ttf"));
//                }catch (Exception e) {
//                   e.printStackTrace();
//                }
                
//                canvas.drawText(dayString, 8 * mDensity * mDensityScaleFactor
//                        /* mBackgroundWidth / 10 * mDensityScaleFactor */, mRotaryOffsetY + mMarginBottom
//                                + LENSE_DAY_SIZE_DIP * mDensity - 20 + (8 * mDensity * mDensityScaleFactor) + customTimeOffset, mLensePaint);
                //Log.e("############################################", "###=" + (LENSE_TIME_SIZE_DIP * mDensity * mDensityScaleFactor));
                float timeSize=LENSE_TIME_SIZE_DIP * mDensity * mDensityScaleFactor;
                mLensePaint.setTextSize(timeSize/*LENSE_TIME_SIZE_DIP * mDensity * mDensityScaleFactor*/);
                mLensePaint.setTextAlign(Align.LEFT);
                mLensePaint.setAlpha(255);
                canvas.drawText(timeString,/*
                                            * 10+LENSE_DAY_SIZE_DIP* mDensity *
                                            * mDensityScaleFactor
                                            */
                        mBackgroundWidth / 2 * mDensityScaleFactor -timeSize*timeString.length()/ 4, 
                        mRotaryOffsetY +mMarginBottom-mMarginBottomAdd-(3 * mDensity * mDensityScaleFactor)
                        + dateSize*2+ dateSize/2, mLensePaint);
                //add by shendu liuchuan
                Log.d("MyLog", "--------------"+dateSize);
                int mmsWidth=mMmsBitmap.getWidth();
                int callWidth=mCallBitmap.getWidth();
				switch (mDownPart) {
				case DOWNL:
	                canvas.drawBitmap(mClickBitmap, 0, mRotaryOffsetY +mMarginBottom-18*mDensity-mMarginBottomAdd
                  , mLensePaint);
					break;
				case DOWNM:
					if (mIsDown) {
						 canvas.drawBitmap(mBackground_Click, drawMatrix, mPaint);
					}
					break;
				case DOWNR:
	                canvas.drawBitmap(mClickBitmap, mBackgroundWidth-mmsWidth-15*mDensity, mRotaryOffsetY-mMarginBottomAdd +mMarginBottom-18*mDensity
                  , mLensePaint);
					break;
				default:
					break;
				}
                canvas.drawBitmap(mCallBitmap, 10*mDensity, mRotaryOffsetY +mMarginBottom-mMarginBottomAdd
                       , mLensePaint);
                canvas.drawBitmap(mMmsBitmap, mBackgroundWidth-mmsWidth-10*mDensity, mRotaryOffsetY-mMarginBottomAdd +mMarginBottom
                        , mLensePaint);
                float numSize=15 * mDensity * mDensityScaleFactor;
                mLensePaint.setTextSize(numSize);
                int numWidth=mNumBitmap.getWidth();
                if (mCallNum!=0) {
                	canvas.drawBitmap(mNumBitmap, 10*mDensity+mmsWidth/2, mRotaryOffsetY +mMarginBottom-mMarginBottomAdd
                            , mLensePaint);
                	 canvas.drawText(mCallNum+"",10*mDensity+mmsWidth/2+numWidth/3, mRotaryOffsetY +mMarginBottom-mMarginBottomAdd+numWidth*5/7, mLensePaint);
				}
                if (mMmsNum!=0) {
                    canvas.drawBitmap(mNumBitmap, mBackgroundWidth-mmsWidth-10*mDensity+mmsWidth/2, mRotaryOffsetY +mMarginBottom-mMarginBottomAdd
                            , mLensePaint);
                    canvas.drawText(mMmsNum+"",mBackgroundWidth-mmsWidth-10*mDensity+mmsWidth/2+numWidth/3, mRotaryOffsetY +mMarginBottom-mMarginBottomAdd+numWidth*5/7, mLensePaint);
				}

              //hui zhi tian qi xin xi 
                if(weatherState){
                    mLensePaint.setTextSize(LENSE_DATE_SIZE_DIP * mDensity * mDensityScaleFactor);
                    mLensePaint.setTextAlign(Align.RIGHT);
                    canvas.drawText(condition+ " " + temp /*+ getResources().getString(R.string.lockscreen_weather_temp)*/,
                            mBackgroundWidth*4 / 4  * mDensityScaleFactor - (8 * mDensity * mDensityScaleFactor), mRotaryOffsetY
                 + mMarginBottom + LENSE_DAY_SIZE_DIP * mDensity - 20 + (8 * mDensity * mDensityScaleFactor)
                 + customTimeOffset, mLensePaint);
                    
                    drawMatrix.postTranslate(360,1);
                    canvas.drawBitmap(weathIcon, drawMatrix , null);
                }
                //hui zhi xiang xia hua dong wen ben ti shi 
                if (showTipTx) {
                    mLensePaint.setTextSize(LENSE_DATE_SIZE_DIP * mDensity * mDensityScaleFactor);
                    mLensePaint.setTextAlign(Align.CENTER);
                    canvas.drawText(getResources().getString(R.string.lockscreen_down_tips),
                            mBackgroundWidth / 2 * mDensityScaleFactor, mRotaryOffsetY
                                    + mMarginBottom + (LENSE_DATE_SIZE_DIP + 144) * mDensity
                                    + customTimeOffset, mLensePaint);
                }  
                
              //  addliu
                if (mIsShowAnimation) {
                	  canvas.drawBitmap(mAnimationBitmaps[i], mBackgroundWidth/2-mAnimation_0.getWidth()/2, mMarginBottom+60 * mDensity * mDensityScaleFactor
                              , mLensePaint);
				}
              
            }
            
           
            return;
        }

        // Draw the correct arrow(s) depending on the current state:
        if (!mHideArrows) {
            mArrowMatrix.reset();
            switch (mGrabbedState) {
                case NOTHING_GRABBED:
                    //mArrowShortLeftAndRight;
                    break;
                case LEFT_HANDLE_GRABBED:
                    mArrowMatrix.setTranslate(0, 0);
                    mArrowMatrix.postScale(mDensityScaleFactor, mDensityScaleFactor);
                    if (!isHoriz()) {
                        mArrowMatrix.preRotate(-90, 0, 0);
                        mArrowMatrix.postTranslate(0, height);
                    }
                    canvas.drawBitmap(mArrowLongLeft, mArrowMatrix, mPaint);
                    break;
                case MID_HANDLE_GRABBED:
                    mArrowMatrix.setTranslate(0, 0);
                    mArrowMatrix.postScale(mDensityScaleFactor, mDensityScaleFactor);
                    if (!isHoriz()) {
                        mArrowMatrix.preRotate(-90, 0, 0);
                    }
                    // draw left down arrow
                    mArrowMatrix.postTranslate(halfdimple, 0);
                    canvas.drawBitmap(mArrowDown, mArrowMatrix, mPaint);
                    // draw right down arrow
                    mArrowMatrix.postTranslate(mRightHandleX-mLeftHandleX, 0);
                    canvas.drawBitmap(mArrowDown, mArrowMatrix, mPaint);
                    // draw mid down arrow
                    mArrowMatrix.postTranslate(mMidHandleX-mRightHandleX, -(mDimpleWidth/4));
                    canvas.drawBitmap(mArrowDown, mArrowMatrix, mPaint);
                    break;
                case RIGHT_HANDLE_GRABBED:
                    mArrowMatrix.setTranslate(0, 0);
                    mArrowMatrix.postScale(mDensityScaleFactor, mDensityScaleFactor);
                    if (!isHoriz()) {
                        mArrowMatrix.preRotate(-90, 0, 0);
                        // since bg width is > height of screen in landscape mode...
                        mArrowMatrix.postTranslate(0, height + (mBackgroundWidth - height));
                    }
                    canvas.drawBitmap(mArrowLongRight, mArrowMatrix, mPaint);
                    break;
                default:
                    throw new IllegalStateException("invalid mGrabbedState: " + mGrabbedState);
            }
        }

        if (VISUAL_DEBUG) {
            // draw circle bounding arc drawable: good sanity check we're doing the math correctly
            float or = mRotaryOuterRadiusDIP * mDensity;
            final int vOffset = mBackgroundWidth - height;
            final int midX = isHoriz() ? width / 2 : mBackgroundWidth / 2 - vOffset;
            if (isHoriz()) {
                canvas.drawCircle(midX, or + bgTop, or, mPaint);
            } else {
                canvas.drawCircle(or + bgTop, midX, or, mPaint);
            }
        }

        // left dimple / icon
        {
            int xOffset = mLeftHandleX + mRotaryOffsetX;
            if(!isHoriz()) xOffset = xOffset + mStatusBarSize/2;
            final int drawableY = getYOnArc(
                    mBackgroundWidth,
                    mInnerRadius,
                    mOuterRadius,
                    xOffset);
            final int x = isHoriz() ? xOffset : drawableY + bgTop;
            final int y = isHoriz() ? drawableY + bgTop : height - xOffset;
            if (mRevampedMode || (mGrabbedState != RIGHT_HANDLE_GRABBED
                    && mGrabbedState != MID_HANDLE_GRABBED)) {
                drawCentered(mDimple, canvas, x, y);
                drawCentered(mLeftHandleIcon, canvas, x, y);
            } else {
                drawCentered(mDimpleDim, canvas, x, y);
            }
        }

        // center dimple / icon
        {
            int xOffset = mMidHandleX + mRotaryOffsetX;
            if(!isHoriz()) xOffset = xOffset + mStatusBarSize/2;
            final int drawableY = getYOnArc(
                    mBackgroundWidth,
                    mInnerRadius,
                    mOuterRadius,
                    xOffset);
            final int x = isHoriz() ? xOffset : drawableY + bgTop;
            final int y = isHoriz() ? drawableY + bgTop : height - xOffset;
            if ((mRevampedMode || (mGrabbedState != LEFT_HANDLE_GRABBED
                    && mGrabbedState != RIGHT_HANDLE_GRABBED)) && mCustomAppDimple) {
                drawCentered(mDimple, canvas, x, y);
                drawCentered(mMidHandleIcon, canvas, x, y);
            } else {
                drawCentered(mDimpleDim, canvas, x, y);
            }
        }

        // right dimple / icon
        {
            int xOffset = mRightHandleX + mRotaryOffsetX;
            if(!isHoriz()) xOffset = xOffset + mStatusBarSize/2;
            final int drawableY = getYOnArc(
                    mBackgroundWidth,
                    mInnerRadius,
                    mOuterRadius,
                    xOffset);
            final int x = isHoriz() ? xOffset : drawableY + bgTop;
            final int y = isHoriz() ? drawableY + bgTop : height - xOffset;
            if (mRevampedMode || (mGrabbedState != LEFT_HANDLE_GRABBED
                    && mGrabbedState != MID_HANDLE_GRABBED)) {
                drawCentered(mDimple, canvas, x, y);
                drawCentered(mRightHandleIcon, canvas, x, y);
            } else {
                drawCentered(mDimpleDim, canvas, x, y);
            }
        }

        // draw extra left hand dimples
        int dimpleLeft = mRotaryOffsetX + mLeftHandleX - mDimpleSpacing;
        if(!isHoriz()) dimpleLeft = dimpleLeft + mStatusBarSize/2;
        while (dimpleLeft > -halfdimple) {
            final int drawableY = getYOnArc(
                    mBackgroundWidth,
                    mInnerRadius,
                    mOuterRadius,
                    dimpleLeft);

            if (isHoriz()) {
                drawCentered(mDimpleDim, canvas, dimpleLeft, drawableY + bgTop);
            } else {
                drawCentered(mDimpleDim, canvas, drawableY + bgTop, height - dimpleLeft);
            }
            dimpleLeft -= mDimpleSpacing;
        }

        // draw extra middle dimples
        int dimpleMid = mRotaryOffsetX + mMidHandleX + mDimpleSpacing;
        if(!isHoriz()) dimpleMid = dimpleMid + mStatusBarSize/2;
        final int midThresh = mMidHandleX + halfdimple;
        while (dimpleMid < midThresh) {
            final int drawableY = getYOnArc(
                    mBackgroundWidth,
                    mInnerRadius,
                    mOuterRadius,
                    dimpleMid);

            if (isHoriz()) {
                drawCentered(mDimpleDim, canvas, dimpleLeft, drawableY + bgTop);
            } else {
                drawCentered(mDimpleDim, canvas, drawableY + bgTop, height - dimpleMid);
            }
            dimpleMid += mDimpleSpacing;
        }

        // draw extra right hand dimples
        int dimpleRight = mRotaryOffsetX + mRightHandleX + mDimpleSpacing;
        if(!isHoriz()) dimpleRight = dimpleRight + mStatusBarSize/2;
        final int rightThresh = mRight + halfdimple;
        while (dimpleRight < rightThresh) {
            final int drawableY = getYOnArc(
                    mBackgroundWidth,
                    mInnerRadius,
                    mOuterRadius,
                    dimpleRight);

            if (isHoriz()) {
                drawCentered(mDimpleDim, canvas, dimpleRight, drawableY + bgTop);
            } else {
                drawCentered(mDimpleDim, canvas, drawableY + bgTop, height - dimpleRight);
            }
            dimpleRight += mDimpleSpacing;
        }
    }

    /**
     * Assuming bitmap is a bounding box around a piece of an arc drawn by two concentric circles
     * (as the background drawable for the rotary widget is), and given an x coordinate along the
     * drawable, return the y coordinate of a point on the arc that is between the two concentric
     * circles.  The resulting y combined with the incoming x is a point along the circle in
     * between the two concentric circles.
     *
     * @param backgroundWidth The width of the asset (the bottom of the box surrounding the arc).
     * @param innerRadius The radius of the circle that intersects the drawable at the bottom two
     *        corders of the drawable (top two corners in terms of drawing coordinates).
     * @param outerRadius The radius of the circle who's top most point is the top center of the
     *        drawable (bottom center in terms of drawing coordinates).
     * @param x The distance along the x axis of the desired point.    @return The y coordinate, in drawing coordinates, that will place (x, y) along the circle
     *        in between the two concentric circles.
     */
    private int getYOnArc(int backgroundWidth, int innerRadius, int outerRadius, int x) {

        // the hypotenuse
        final int halfWidth = (outerRadius - innerRadius) / 2;
        final int middleRadius = innerRadius + halfWidth;

        // the bottom leg of the triangle
        final int triangleBottom = (int) ((backgroundWidth / 2.0 * mDensityScaleFactor) - x);

        // "Our offense is like the pythagorean theorem: There is no answer!" - Shaquille O'Neal
        final int triangleY =
                (int) Math.sqrt(middleRadius * middleRadius - triangleBottom * triangleBottom);

        // convert to drawing coordinates:
        // middleRadius - triangleY =
        //   the vertical distance from the outer edge of the circle to the desired point
        // from there we add the distance from the top of the drawable to the middle circle
        return middleRadius - triangleY + halfWidth + mRotaryOffsetY - mMarginBottom;
    }

    /**
     * Handle touch screen events.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mAnimating) {
            return true;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int height = getHeight();
        final int width = getWidth();

        final int eventX = isHoriz() ?
                (int) event.getX():
                height - ((int) event.getY());
        final int eventY = isHoriz() ?
                (int) event.getY():
                width - ((int) event.getX());
        final int hitWindow = mDimpleWidth;
        final int downThresh = mDimpleWidth *5/2;

        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            	mIsDown=true;
            	 mDownPart=downSwitch(eventX);//add by shendu liuchuan
            	 mIsShowAnimation=false;
            	 switch (mDownPart) {
				case DOWNL:
					 mCallBitmap.recycle();
	            	 mCallBitmap=getBitmapFor(R.drawable.tel_click);
					break;
                 case DOWNR:
                	 mMmsBitmap.recycle();
                	 mMmsBitmap=getBitmapFor(R.drawable.sms_click);
					break;
				default:
					break;
				}
                if (DBG) log("touch-down");
                mTriggered = false;
                mEventStartY = eventY;
                if (mGrabbedState != NOTHING_GRABBED) {
                    reset();
                    invalidate();
                }
                if (mLenseMode){
                    setGrabbedState(MID_HANDLE_GRABBED);
                    invalidate();
                    vibrate();
                    break;
                }
                if (eventX < mLeftHandleX + hitWindow) {
                    mRotaryOffsetX = eventX - mLeftHandleX;
                    setGrabbedState(LEFT_HANDLE_GRABBED);
                    invalidate();
                    vibrate();
                } else if (eventX > mMidHandleX - hitWindow && eventX <= mRightHandleX - hitWindow && mCustomAppDimple) {
                    setGrabbedState(MID_HANDLE_GRABBED);
                    invalidate();
                    vibrate();
                } else if (eventX > mRightHandleX - hitWindow) {
                    mRotaryOffsetX = eventX - mRightHandleX;
                    setGrabbedState(RIGHT_HANDLE_GRABBED);
                    invalidate();
                    vibrate();
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (DBG) log("touch-move");
                if (mUnLock) {//add by shendu liuchuan Prevent sliding across the screen
					return true;
				}
                if (mGrabbedState == LEFT_HANDLE_GRABBED) {
                    mRotaryOffsetX = eventX - mLeftHandleX;
                    invalidate();
                    final int rightThresh = isHoriz() ? getRight() : height;
                    if (eventX >= rightThresh - mEdgeTriggerThresh && !mTriggered) {
                        mTriggered = true;
                        dispatchTriggerEvent(OnDialTriggerListener.LEFT_HANDLE);
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        final int rawVelocity = isHoriz() ?
                                (int) velocityTracker.getXVelocity():
                                -(int) velocityTracker.getYVelocity();
                        final int velocity = Math.max(mMinimumVelocity, rawVelocity);
                        mDimplesOfFling = Math.max(
                                8,
                                Math.abs(velocity / mDimpleSpacing));
                        startAnimationWithVelocity(
                                eventX - mLeftHandleX,
                                mDimplesOfFling * mDimpleSpacing,
                                velocity);
                    }
                } else if (mGrabbedState == MID_HANDLE_GRABBED && (mCustomAppDimple || mLenseMode)) {
                    mRotaryOffsetY = eventY - mEventStartY;
                    if (!isHoriz())
                        mRotaryOffsetY = mEventStartY - eventY;
                    if (mRotaryOffsetY < 0) {showTipTx = true;mRotaryOffsetY=0;}else{showTipTx = false;}
                    invalidate();

                    if ((Math.abs(mRotaryOffsetY)+80) >= downThresh && !mTriggered) {
                        mTriggered = true;
                        // lense mode is handled as "middle dimple" for up/down movement, yet we need to emit left handle for unlock
                        if(mLenseMode){
                            showTipTx = false;
                            mUnLock=true;
                        }
                        else{
                            dispatchTriggerEvent(OnDialTriggerListener.MID_HANDLE);
                        }
                        // set up "flow up" animation
//                        int delta = (isHoriz() ? eventY - mEventStartY : mEventStartY - eventY);
//                        showTipTx = true;
//                        startAnimationUp(delta, 0, SNAP_BACK_ANIMATION_DURATION_MILLIS);
                    }
                } else if (mGrabbedState == RIGHT_HANDLE_GRABBED) {
                    mRotaryOffsetX = eventX - mRightHandleX;
                    invalidate();
                    if (eventX <= mEdgeTriggerThresh && !mTriggered) {
                        mTriggered = true;
                        dispatchTriggerEvent(OnDialTriggerListener.RIGHT_HANDLE);
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        final int rawVelocity = isHoriz() ?
                                (int) velocityTracker.getXVelocity():
                                - (int) velocityTracker.getYVelocity();
                        final int velocity = Math.min(-mMinimumVelocity, rawVelocity);
                        mDimplesOfFling = Math.max(
                                8,
                                Math.abs(velocity / mDimpleSpacing));
                        startAnimationWithVelocity(
                                eventX - mRightHandleX,
                                -(mDimplesOfFling * mDimpleSpacing),
                                velocity);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mUnLock){
                    dispatchTriggerEvent(OnDialTriggerListener.LEFT_HANDLE);
                    return true;
                }
            	mIsDown=false;
                if (DBG) log("touch-up");
                mDownPart=DOWNM;
                 mCallBitmap.recycle();
	           	 mCallBitmap=getBitmapFor(R.drawable.tel);
	           	 mMmsBitmap.recycle();
	           	 mMmsBitmap=getBitmapFor(R.drawable.sms);
                // handle animating back to start if they didn't trigger
                if (mGrabbedState == LEFT_HANDLE_GRABBED
                        && Math.abs(eventX - mLeftHandleX) > 5) {
                    // set up "snap back" animation
                    startAnimation(eventX - mLeftHandleX, 0, SNAP_BACK_ANIMATION_DURATION_MILLIS);
                } else if (mGrabbedState == MID_HANDLE_GRABBED) {
                    // set up "flow up" animation
                    int delta = (isHoriz() ? eventY - mEventStartY : mEventStartY - eventY);
                    if (delta > 5)
                        startAnimationUp(delta, 0, SNAP_BACK_ANIMATION_DURATION_MILLIS);
                } else if (mGrabbedState == RIGHT_HANDLE_GRABBED
                        && Math.abs(eventX - mRightHandleX) > 5) {
                    // set up "snap back" animation
                    startAnimation(eventX - mRightHandleX, 0, SNAP_BACK_ANIMATION_DURATION_MILLIS);
                }
                mRotaryOffsetX = 0;
                mRotaryOffsetY = 0;
                setGrabbedState(NOTHING_GRABBED);
                showTipTx = true;
                invalidate();
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle(); // wishin' we had generational GC
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                showTipTx = true;
                if (DBG) log("touch-cancel");
                reset();
                invalidate();
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return true;
    }

    private void startAnimation(int startX, int endX, int duration) {
        mAnimating = true;
        mAnimationStartTime = currentAnimationTimeMillis();
        mAnimationDuration = duration;
        mAnimatingDeltaXStart = startX;
        mAnimatingDeltaXEnd = endX;
        setGrabbedState(NOTHING_GRABBED);
        mDimplesOfFling = 0;
        invalidate();
    }

    private void startAnimationWithVelocity(int startX, int endX, int pixelsPerSecond) {
        mAnimating = true;
        mAnimationStartTime = currentAnimationTimeMillis();
        mAnimationDuration = 1000 * (endX - startX) / pixelsPerSecond;
        mAnimationDuration = Math.min(mAnimationDuration, mMaxAnimationDuration);
        mAnimatingDeltaXStart = startX;
        mAnimatingDeltaXEnd = endX;
        setGrabbedState(NOTHING_GRABBED);
        invalidate();
    }

    private void startAnimationUp(int startY, int endY, int duration) {
        mAnimatingUp = true;
        mAnimationStartTime = currentAnimationTimeMillis();
        mAnimationDuration = duration;
        mAnimatingDeltaYStart = startY;
        mAnimatingDeltaYEnd = endY;
        setGrabbedState(NOTHING_GRABBED);
        invalidate();
    }

    private void updateAnimation() {
        final long millisSoFar = currentAnimationTimeMillis() - mAnimationStartTime;
        final long millisLeft = mAnimationDuration - millisSoFar;
        final int totalDeltaX = mAnimatingDeltaXStart - mAnimatingDeltaXEnd;
        final int totalDeltaY = mAnimatingDeltaYStart - mAnimatingDeltaYEnd;
        int delta;
        final boolean goingRight = totalDeltaX < 0;
        if (DBG) log("millisleft for animating: " + millisLeft);
        if (millisLeft <= 0) {
            mAnimating=false;
            mAnimatingUp=false;
            reset();
            return;
        }
        // from 0 to 1 as animation progresses
        float interpolation =
                mInterpolator.getInterpolation((float) millisSoFar / mAnimationDuration);
        if (mAnimating){
            delta = (int) (totalDeltaX * (1 - interpolation));
            mRotaryOffsetX = mAnimatingDeltaXEnd + delta;
        }
        if (mAnimatingUp){
            delta = (int) (totalDeltaY * (1 - interpolation));
            mRotaryOffsetY = mAnimatingDeltaYEnd + delta;
        }

        // once we have gone far enough to animate the current buttons off screen, we start
        // wrapping the offset back to the other side so that when the animation is finished,
        // the buttons will come back into their original places.
        if (mDimplesOfFling > 0 && mAnimatingUp == false) {
            if (!goingRight && mRotaryOffsetX < -3 * mDimpleSpacing) {
                // wrap around on fling left
                mRotaryOffsetX += mDimplesOfFling * mDimpleSpacing;
            } else if (goingRight && mRotaryOffsetX > 3 * mDimpleSpacing) {
                // wrap around on fling right
                mRotaryOffsetX -= mDimplesOfFling * mDimpleSpacing;
            }
        }
        invalidate();
    }

    public void reset() {
        mAnimating = false;
        mRotaryOffsetX = 0;
        mDimplesOfFling = 0;
        setGrabbedState(NOTHING_GRABBED);
        mTriggered = false;
    }

    /**
     * Triggers haptic feedback.
     */
    private synchronized void vibrate() {
        ContentResolver cr = mContext.getContentResolver();
        final boolean hapticsEnabled = Settings.System.getInt(cr, Settings.System.LOCKSCREEN_VIBRATE_ENABLED, 1) == 1;
        if (hapticsEnabled && mVibrator != null) {
//            long[] hapFeedback = new long[] { 0 };
//            		Settings.System.getLongArray(cr, Settings.System.HAPTIC_DOWN_ARRAY, new long[] { 0 });
            mVibrator.vibrate(20);
        }
    }

    /**
     * Draw the bitmap so that it's centered
     * on the point (x,y), then draws it using specified canvas.
     * TODO: is there already a utility method somewhere for this?
     */
    private void drawCentered(Bitmap d, Canvas c, int x, int y) {
        int w = d.getWidth();
        int h = d.getHeight();

        c.drawBitmap(d, x - (w / 2), y - (h / 2), mPaint);
    }


    /**
     * Registers a callback to be invoked when the dial
     * is "triggered" by rotating it one way or the other.
     *
     * @param l the OnDialTriggerListener to attach to this view
     */
    public void setOnDialTriggerListener(OnDialTriggerListener l) {
        mOnDialTriggerListener = l;
        if(l==null){
        	recycleBitmap();
        	unRegisterObserver();
        	mContentResolver = null;
        	mCallInfoObserver = null;
    		mSMSObserver = null;
    		mIsShowAnimation = false;
    		mAnimationRunnable = null;
    		System.gc();
        }
    }

    private final static String contactsIntent = "com.android.contacts.activities.DialtactsActivity";
    private final static String contactsPackageName = "com.android.contacts";
    private Intent triggerIntent = null;
    
    private final static String mmsIntent = "com.android.mms.ui.ConversationList";
    private final static String mmsPackageName = "com.android.mms";
    
    private ComponentName comp = null;
    /**
     * Dispatches a trigger event to our listener.
     */
    private void dispatchTriggerEvent(int whichHandle) {
        vibrate();
        if (mOnDialTriggerListener != null) {
            mOnDialTriggerListener.onDialTrigger(this, whichHandle);
        	switch (mDownPart) {
			case RotarySelector.DOWNL:
					triggerIntent = null;
					triggerIntent = new Intent();
					comp = null;
        			comp = new ComponentName(contactsPackageName,
        					contactsIntent);
        			triggerIntent.setComponent(comp);
        			triggerIntent.setAction("android.intent.action.VIEW");
        			triggerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        			getContext().startActivity(triggerIntent);
				break;
			case RotarySelector.DOWNM:

				break;
			case RotarySelector.DOWNR:
				triggerIntent = null;
				triggerIntent = new Intent();
				comp = null;
    			comp = new ComponentName(mmsPackageName,
    					mmsIntent);
    			triggerIntent.setComponent(comp);
    			triggerIntent.setAction("android.intent.action.VIEW");
    			triggerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			getContext().startActivity(triggerIntent);
				break;

			default:
				break;
			}
        }
    }

    /**
     * Sets the current grabbed state, and dispatches a grabbed state change
     * event to our listener.
     */
    private void setGrabbedState(int newState) {
        if (newState != mGrabbedState) {
            mGrabbedState = newState;
            if (mOnDialTriggerListener != null) {
                mOnDialTriggerListener.onGrabbedStateChange(this, mGrabbedState);
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when the dial
     * is "triggered" by rotating it one way or the other.
     */
    public interface OnDialTriggerListener {
        /**
         * The dial was triggered because the user grabbed the left handle,
         * and rotated the dial clockwise.
         */
        public static final int LEFT_HANDLE = 1;

        /**
         * The dial was triggered because the user grabbed the middle handle,
         * and moved the dial down.
         */
        public static final int MID_HANDLE = 2;

        /**
         * The dial was triggered because the user grabbed the right handle,
         * and rotated the dial counterclockwise.
         */
        public static final int RIGHT_HANDLE = 3;

        /**
         * Called when the dial is triggered.
         *
         * @param v The view that was triggered
         * @param whichHandle  Which "dial handle" the user grabbed,
         *        either {@link #LEFT_HANDLE}, {@link #RIGHT_HANDLE}.
         */
        void onDialTrigger(View v, int whichHandle);

        /**
         * Called when the "grabbed state" changes (i.e. when
         * the user either grabs or releases one of the handles.)
         *
         * @param v the view that was triggered
         * @param grabbedState the new state: either {@link #NOTHING_GRABBED},
         * {@link #LEFT_HANDLE_GRABBED}, or {@link #RIGHT_HANDLE_GRABBED}.
         */
        void onGrabbedStateChange(View v, int grabbedState);
    }

    /**
     * Sets weather or not to display the custom app dimple
     */
    public void enableCustomAppDimple(boolean newState){
        mCustomAppDimple=newState;
    }

    /**
     * Sets weather or not to display the directional arrows
     */
    public void hideArrows(boolean newState){
        mHideArrows=newState;
    }

    /**
     * Sets up the original rotary style - called from InCallTouchUi.java only
     */
    public void setRotary(boolean newState){
        if(newState){
            mBackground = getBitmapFor(R.drawable.jog_dial_bg);
            mDimple = getBitmapFor(R.drawable.jog_dial_dimple);
            mDimpleDim = getBitmapFor(R.drawable.jog_dial_dimple_dim);
        }
    }

    /**
     * Sets up the rotary revamped style - called from LockScreen.java and InCallTouchUi.java
     */
    public void setRevamped(boolean newState){
        if(newState){
            if(mCustomAppDimple)
                mBackground = getBitmapFor(R.drawable.jog_dial_bg_rev_down);
            else
                mBackground = getBitmapFor(R.drawable.jog_dial_bg_rev);
            mDimple = getBitmapFor(R.drawable.jog_dial_dimple_rev);
            mDimpleDim = getBitmapFor(R.drawable.jog_dial_dimple_dim_rev);
        }
        mRevampedMode=newState;
    }

    /**
     * Sets up the lense square style - called from LockScreen.java and InCallTouchUi.java
     */
    public void setLenseSquare(boolean newState){
        mLenseMode=false;
        if(newState){
            mLenseMode=true;
            //mBackground = getBitmapFor(R.drawable.lense_square_bg);
			mBackground = getBitmapFor(R.drawable.lense_bg);//edit bg drawable backgroud
			mBackgroundWidth = mBackground.getWidth();// add by shendu liuchuan 
        }
    }

    // Debugging / testing code

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
