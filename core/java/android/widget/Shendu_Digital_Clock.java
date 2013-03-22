package android.widget;

import java.util.Calendar;
import java.util.TimeZone;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

/*
 * 
 * @ time 2012-9-7 
 * @ author : shutao shutao@shendu.com
 * @ Project: ShenDu OS 2.0
 * @ Function : shendu digital clock view
 * @ hide
 */
@RemoteView
public class Shendu_Digital_Clock extends View {

	private Drawable mDial;
	
	private Drawable mShendu_Time_Cap;

	private Time mCalendar;

	private Calendar mCaleandar;

	private boolean mAttached;

	private String mTime_Hour = "";

	private String mTime_Minutes = "";
	
	private String mDate = "";

	private String mAM_PM = "";
	
	private String mWeek = "";

	private final static String M24 = "kk:mm";

	private final static String M12 = "h:mm";
	
	private Typeface mShendu_time_Typeface;
	
	private boolean mIsStart = false;

    private Context mContext;	
	/**shutao 2012-9-27*/
	private boolean mIsViewFocusChanged = true;

	public boolean isViewFocusChanged() {
		return mIsViewFocusChanged;
	}

	private final Handler mHandler = new Handler();
	
	public boolean isStart() {
		return mIsStart;
	}
	
	public String getmDate() {
		return mDate;
	}

	public String getmAM_PM() {
		return mAM_PM;
	}

	public String getmWeek() {
		return mWeek;
	}

	public String getmTime_Hour() {
		return mTime_Hour;
	}

	public String getmTime_Minutes() {
		return mTime_Minutes;
	}
	
	public Shendu_Digital_Clock(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public Shendu_Digital_Clock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}

	public Shendu_Digital_Clock(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mDial = context.getResources().getDrawable(
				com.android.internal.R.drawable.shendu_digital_clock2_bg);
		mShendu_Time_Cap = context.getResources().getDrawable(com.android.internal.R.drawable.shendu_time_cap);
		mCalendar = new Time();
		mCaleandar = Calendar.getInstance();
		try{
			mShendu_time_Typeface = Typeface.createFromFile("system/fonts/kalinga.ttf");
		}catch(Exception e){
			mShendu_time_Typeface = Typeface.DEFAULT;
		}
	}

	

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		final Drawable dial = mDial;
		int mWidth = this.getWidth();
		Paint paint = new Paint();
		paint.setColor(mContext.getResources().getColor(com.android.internal.R.color.shendu_clock_widget_color));
		paint.setAntiAlias(true);
		paint.setDither(true);
		int w = dial.getIntrinsicWidth();
		int h = dial.getIntrinsicHeight();
		int mDialOffset = (mWidth - w) / 2;
		int x = 0;
		int top_Off = h/2-40;
		int y = 0;
		int pmx = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_mp_offset_l);
		int pmy = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_mp_offset_r);
		int date_r = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_date_r);
       int date_l = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_date_l);
       int week_r = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_week_r);
       int week_l = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_week_l);


		dial.setBounds(mDialOffset, y+top_Off, w+mDialOffset, h+top_Off);
		MyLog("mWidth = " + mDialOffset + "mHeight = " +h);
		dial.draw(canvas);
		DrawTime(canvas, mDialOffset,top_Off);
		paint.setTextSize(mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_mp_size));
		canvas.drawText(mAM_PM, mDialOffset+pmx, pmy+top_Off, paint);
		paint.setTextSize(mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_date_size));
		canvas.drawText(mDate, mDialOffset+date_r, date_l+top_Off, paint);
		paint.setTextAlign(Align.RIGHT);
		canvas.drawText(mWeek, mDialOffset+week_r, week_l+top_Off, paint);
		canvas.save();
	}
	
	private void DrawTime(Canvas canvas , int mDialOffset , int top_Off ){
		final Drawable cap = mShendu_Time_Cap;
		int cap_W = cap.getIntrinsicWidth();
		int cap_H = cap.getIntrinsicHeight();
		int cap_offset = mDialOffset + (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_cap_offset_l);

		Paint paint = new Paint();
		paint.setColor(mContext.getResources().getColor(com.android.internal.R.color.shendu_clock_widget_color));
		paint.setAntiAlias(true);
		paint.setDither(true);    	
		paint.setTextSize(mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_mtext_size));
		paint.setTypeface(mShendu_time_Typeface);
		
		int timel = (int) mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_time_offset_r);
		
		int hour_l = (int) mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_time_Hour_r);
		
		int minutes_r = (int) mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_time_Minutes_r);
		
		int cap_off = (int) mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_cap_off);

		canvas.drawText(mTime_Hour, mDialOffset+hour_l, timel+top_Off, paint);
		
		canvas.drawText(mTime_Minutes, mDialOffset+minutes_r, timel+top_Off, paint);
		
		cap.setBounds(cap_offset, cap_off+top_Off, cap_W+cap_offset,cap_H+cap_off+top_Off);
		
		cap.draw(canvas);
		canvas.save();
	}

	
	
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		// TODO Auto-generated method stub
		MyLog("onWindowFocusChanged"+hasWindowFocus);
		mIsViewFocusChanged = hasWindowFocus;
		if(hasWindowFocus){
			onTimeChanged();
			invalidate();
		}
		super.onWindowFocusChanged(hasWindowFocus);
	}

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		if (!mAttached) {
			mAttached = true;
			IntentFilter filter = new IntentFilter();

			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIME_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

			getContext().registerReceiver(mIntentReceiver, filter, null,
					mHandler);
		}

		// NOTE: It's safe to do these after registering the receiver since the
		// receiver always runs
		// in the main thread, therefore the receiver can't run before this
		// method returns.

		// The time zone may have changed while the receiver wasn't registered,
		// so update the Time
		mCalendar = new Time();

		MyLog("onAttachedToWindow");
		// Make sure we update to the current time
		onTimeChanged();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			getContext().unregisterReceiver(mIntentReceiver);
			mAttached = false;
		}
	}

	public BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
			}
			MyLog("isViewFocusChanged "+mIsViewFocusChanged);
			if(mIsViewFocusChanged){
			onTimeChanged();
			invalidate();
			}
		}
	};

	protected void onTimeChanged() {

		mCalendar.setToNow();
		
//		if(isStart){
//			MyLog(mCalendar.second+"mCalendar.second");
//			if(mCalendar.second != 0){
//				return;
//			}
//		}
		mIsStart = true;
		
		mCaleandar.setTimeInMillis(System.currentTimeMillis());

		String newTime = DateFormat.format(get24HourMode(getContext()),
				mCaleandar).toString();
		
		if(!android.text.format.DateFormat.is24HourFormat(getContext())){
			mAM_PM = mCaleandar.get(mCaleandar.AM_PM) == 0 ? "AM" : "PM";
		}else{
			mAM_PM = "";
		}
		
		if(newTime.indexOf(":") == 1){
			newTime = "0"+newTime;
		}
		String time[] = newTime.split(":");
		mTime_Hour = time[0];
		mTime_Minutes = time[1];
		
		mDate = DateFormat.format("yyyy/MM/dd", mCaleandar).toString();
		
		mWeek = DateUtils.formatDateTime(getContext(),System.currentTimeMillis() ,DateUtils.FORMAT_SHOW_WEEKDAY);
		
		MyLog("newTime = "+mWeek);
	}

	/**
	 * @return true if clock is set to 24-hour mode
	 */
	private String get24HourMode(final Context context) {
		return android.text.format.DateFormat.is24HourFormat(context) ? M24
				: M12;
	}
	
	private boolean isOpen = false;

	private void MyLog(String msg) {
		if (isOpen) {
			Log.d("1616", msg);
		}
	}

}
