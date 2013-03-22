package android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
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
public class Shendu_Flip_Clock extends Shendu_Digital_Clock {
 
	
	private Bitmap mTime_Mask_Pt_Hour ;
	private Bitmap mTime_Mask_Pt_Minutes ;

	private Drawable mShendu_Flip_mDial;
	
	private Bitmap mTime_Hour_Overhead ;
	private Bitmap mTime_Hour_Lower ;
	private Bitmap mTime_Minutes_Overhead ;
	private Bitmap mTime_Minutes_Lower ;
	private Bitmap mTime_Hour_Deck;
	private Bitmap mTime_Minutes_Deck;
	private Bitmap mTime_Flip_Line;
	private Bitmap mTime_Flip_Mask_Middle;
	private Bitmap mTime_Flip_Mask_Side;
	
	private int mHour_mRect_Top = 0;
	private int mHour_mRect_Bottom = 0;
	private int mHour_mRectF_Top = 0;
	private int mHour_mRectF_Bottom = 0;
	
	private int mMinutes_mRect_Top = 0;
	private int mMinutes_mRect_Bottom = 0;
	private int mMinutes_mRectF_Top = 0;
	private int mMinutes_mRectF_Bottom = 0;
	
	private String mShendu_Current_Time_Hour = "";
	private String mShendu_Current_Time_Minutes = "" ;
	
	private String mShendu_Previous_Time_Hour = "" ;
	private String mShendu_Previous_Time_Minutes = "" ;
	
	private String mShendu_Time_Hour_Up = "";
	private String mShendu_Time_Minutes_Up = "";
	
	private String mShendu_Tiem_Deck = "";
	
	private Thread mShendu_ui_thread;
	
	private Typeface mShendu_time_Typeface;

	private Context mContext;
	
	private boolean mIsHourChange = false;
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			setViewNumber(!isStart());
		}
		
	};
	
	public Shendu_Flip_Clock(Context context) {
		
		this(context,null);
	
		// TODO Auto-generated constructor stub
	}

	public Shendu_Flip_Clock(Context context, AttributeSet attrs) {
		this(context, attrs,0);
		// TODO Auto-generated constructor stub
	}

	public Shendu_Flip_Clock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		this.mContext = context;
		mShendu_Flip_mDial = context.getResources().getDrawable(com.android.internal.R.drawable.shendu_time_flip_bg);
		mTime_Mask_Pt_Hour = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.shendu_time_flip_mask_left);
		mTime_Mask_Pt_Minutes = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.shendu_time_flip_mask_right);
		mTime_Flip_Line = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.shendu_time_flip_line);
		mTime_Flip_Mask_Middle = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.shendu_time_flip_mask_middle);
		mTime_Flip_Mask_Side = BitmapFactory.decodeResource(getResources(), com.android.internal.R.drawable.shendu_time_flip_mask_side);
		try{
			mShendu_time_Typeface =  Typeface.createFromFile("system/fonts/kalinga.ttf");
		}catch(Exception e){
			mShendu_time_Typeface = Typeface.DEFAULT;
		}
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
//		super.onDraw(canvas);
		int mWidth = this.getWidth();
		Paint paint = new Paint();
		paint.setColor(0xff525252);
		paint.setAntiAlias(true);
		paint.setDither(true);
		int mDialOffset = (mWidth-mShendu_Flip_mDial.getMinimumWidth())/2;
		int time_pt_h = mTime_Mask_Pt_Hour.getHeight();
		int date_r = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_date_r);
		int date_l = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_date_l);
		int week_r = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_week_r);
		int week_l = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_week_l);
	    
		int top_Offset = time_pt_h/2- (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_top_off);
		int x = 0;
		int y = 0;
		DrawTime (canvas, mDialOffset,time_pt_h,top_Offset);
		paint.setTextSize(mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_date_size));
		paint.setColor(0xffffffff);
		canvas.drawText(getmDate(), mDialOffset+date_r, date_l+top_Offset, paint);
		paint.setTextAlign(Align.RIGHT);
		canvas.drawText(getmWeek(), mDialOffset+week_r, week_l+top_Offset, paint);
		canvas.save();
	}
	
	
	private void DrawTime(Canvas canvas , int mDialOffset , int time_pt_h ,int top_Offset){
//		Bitmap time_Hour = time_Mask_Pt;
//		Bitmap time_Minutes = time_Mask_Pt;
//		canvas.restore();
		int time_pt_w = mTime_Mask_Pt_Hour.getWidth();

		Paint paint = new Paint();
		paint.setColor(0xff4B4B4B);
		paint.setAntiAlias(true);
		paint.setDither(true);
		int hour_offset = mDialOffset+(int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_time_hour_offset) ;
		int minutes_offset = mDialOffset+(int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_time_minutes_offset);
		recycleTimeBitmap();
		int bg_w = mShendu_Flip_mDial.getIntrinsicWidth();
		int bg_h = mShendu_Flip_mDial.getIntrinsicHeight();
		int middle_w = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_filp_widget_middle_w);

		int middle_h = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_filp_widget_middle_h);

		int line_w = (int) mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_filp_widget_line_w);

		int top_set = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_filp_widget_bg_topset);
		
		mShendu_Flip_mDial.setBounds(mDialOffset, top_Offset-top_set , bg_w+mDialOffset, bg_h+top_Offset-top_set);
		mShendu_Flip_mDial.draw(canvas);
		
		mTime_Hour_Overhead =  createTimeBitmap(time_pt_w, time_pt_h, paint, /*shendu_Time_Hour_Up*/getmTime_Hour(),hour_offset,true,top_Offset);
		mTime_Hour_Lower =  createTimeBitmap(time_pt_w, time_pt_h, paint, mShendu_Current_Time_Hour,hour_offset,true,top_Offset);
		mTime_Minutes_Overhead = createTimeBitmap(time_pt_w, time_pt_h, paint, /*shendu_Time_Minutes_Up*/getmTime_Minutes() ,minutes_offset,false,top_Offset);
		mTime_Minutes_Lower =  createTimeBitmap(time_pt_w, time_pt_h, paint, mShendu_Current_Time_Minutes,minutes_offset,false,top_Offset);
		
		Rect hour_Rect2 = new Rect(hour_offset, top_Offset, time_pt_w+hour_offset, time_pt_h+top_Offset);
		RectF hour_RectF2 = new RectF(hour_offset, top_Offset, time_pt_w+hour_offset, time_pt_h+top_Offset);

		Rect hour_Rect = new Rect(hour_offset, mHour_mRect_Top +top_Offset, time_pt_w+hour_offset, mHour_mRect_Bottom+top_Offset);
		RectF hour_RectF = new RectF(hour_offset, mHour_mRectF_Top+top_Offset, time_pt_w+hour_offset, mHour_mRectF_Bottom+top_Offset);

		Rect minutes_Rect2 = new Rect(minutes_offset, top_Offset, time_pt_w+minutes_offset, time_pt_h+top_Offset);
		RectF minutes_RectF2 = new RectF(minutes_offset, top_Offset, time_pt_w+minutes_offset, time_pt_h+top_Offset);
		
		Rect minutes_Rect = new Rect(minutes_offset, mMinutes_mRect_Top+top_Offset , time_pt_w+minutes_offset, mMinutes_mRect_Bottom+top_Offset);
		RectF minutes_RectF = new RectF(minutes_offset, mMinutes_mRectF_Top+top_Offset, time_pt_w+minutes_offset, mMinutes_mRectF_Bottom+top_Offset);
		

		canvas.drawBitmap(mTime_Hour_Lower, hour_Rect2, hour_RectF2 , paint);
		if( mMinutes_mRectF_Top < time_pt_h && mIsHourChange){
			if(mTime_Hour_Deck != null){
				if(mTime_Hour_Deck != null){
					mTime_Hour_Deck.recycle();
				}
				mTime_Hour_Deck = createTimeBitmap(time_pt_w , time_pt_h, paint ,mShendu_Previous_Time_Hour , mDialOffset,true,top_Offset);
				Rect hour_Deck_Rect = new Rect(hour_offset, time_pt_h/2+top_Offset , time_pt_w+hour_offset, time_pt_h+top_Offset);
				RectF hour_Deck_RectF = new RectF(hour_offset, time_pt_h/2+top_Offset, time_pt_w+hour_offset, time_pt_h+top_Offset);
				canvas.drawBitmap(mTime_Hour_Deck, hour_Deck_Rect, hour_Deck_RectF, paint);
			}
		}
		canvas.drawBitmap(mTime_Hour_Overhead, hour_Rect, hour_RectF, paint);
		

		
		canvas.drawBitmap(mTime_Minutes_Lower, minutes_Rect2, minutes_RectF2, paint);
		
		if( mMinutes_mRectF_Top < time_pt_h){
			if(mTime_Minutes_Deck != null){
				mTime_Minutes_Deck.recycle();
			}
			mTime_Minutes_Deck = createTimeBitmap(time_pt_w , time_pt_h, paint ,mShendu_Previous_Time_Minutes  , minutes_offset,false,top_Offset);
			Rect minutes_Deck_Rect = new Rect(minutes_offset, time_pt_h/2+top_Offset, time_pt_w+minutes_offset, time_pt_h+top_Offset);
			RectF minutes_Deck_RectF = new RectF(minutes_offset, time_pt_h/2+top_Offset, time_pt_w+minutes_offset, time_pt_h+top_Offset);
			canvas.drawBitmap(mTime_Minutes_Deck, minutes_Deck_Rect, minutes_Deck_RectF, paint);
		}
		
		canvas.drawBitmap(mTime_Minutes_Overhead, minutes_Rect, minutes_RectF, paint);
		
		canvas.drawBitmap(mTime_Flip_Line, mDialOffset+line_w, time_pt_h/2+top_Offset , paint);
		canvas.drawBitmap(mTime_Flip_Mask_Middle, mDialOffset+bg_w/2-middle_w, time_pt_h/2-middle_h+top_Offset, paint);
		
	
		canvas.drawBitmap(mTime_Flip_Mask_Side, mDialOffset+4, time_pt_h/2-middle_h+top_Offset, paint);
		canvas.drawBitmap(mTime_Flip_Mask_Side, mDialOffset+bg_w-8, time_pt_h/2-middle_h+top_Offset, paint);
		
	}
	
	
	private Bitmap createTimeBitmap(int time_pt_w , int time_pt_h , Paint paint , String timeText , int offset , boolean isHour , int top_Offset){
		
		paint.setTextSize(mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_widget_mtext_size));
		paint.setTypeface(mShendu_time_Typeface);
		int time_l = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_time_r);
		int time_r = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_time_l);
		int mp_r = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_pm_r);
		int mp_l = (int)mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_pm_l);
		
		Bitmap time_Bitmap =  Bitmap.createBitmap( time_pt_w+offset, time_pt_h+top_Offset, Config.ARGB_8888);
		Canvas time_Bitmap_Canvas = new Canvas(time_Bitmap);
		time_Bitmap_Canvas.drawBitmap(isHour?mTime_Mask_Pt_Hour:mTime_Mask_Pt_Minutes,offset,top_Offset, paint);
		time_Bitmap_Canvas.drawText(timeText, time_l+offset ,time_r+top_Offset , paint);
		if(isHour){
		paint.setTextSize(mContext.getResources().getDimension(com.android.internal.R.dimen.shendu_clock_flip_widget_mp_size));
		paint.setTypeface(Typeface.DEFAULT);
		time_Bitmap_Canvas.drawText(getmAM_PM(), offset+mp_r, mp_l+top_Offset, paint);
		}
		time_Bitmap_Canvas.save(Canvas.ALL_SAVE_FLAG);
		time_Bitmap_Canvas.restore();
		return time_Bitmap;
		
	}
	
	

	private synchronized void setViewNumber(boolean isNew ){
		
		int time_pt_h = mTime_Mask_Pt_Hour.getHeight();
		
		if(isNew){
			mHour_mRect_Top = 0;
			mHour_mRect_Bottom = time_pt_h;
			mHour_mRectF_Top = 0;
			mHour_mRectF_Bottom = time_pt_h;
		
			mMinutes_mRect_Top = 0;
			mMinutes_mRect_Bottom = time_pt_h;
			mMinutes_mRectF_Top = 0;
			mMinutes_mRectF_Bottom = time_pt_h;
		
			mShendu_Time_Hour_Up = mShendu_Current_Time_Hour;
			mShendu_Time_Minutes_Up = mShendu_Current_Time_Minutes;

			return ;
		}
		if( mMinutes_mRectF_Top < time_pt_h/2 ){
			mMinutes_mRectF_Top = mMinutes_mRectF_Top + 5;
			mMinutes_mRect_Top = 0 ;
			mMinutes_mRect_Bottom = time_pt_h/2 ;
			mMinutes_mRectF_Bottom = time_pt_h/2 ;
			mShendu_Time_Hour_Up = mShendu_Previous_Time_Hour;
			mShendu_Time_Minutes_Up = mShendu_Previous_Time_Minutes;
			MyLog("mRectF_bottom = "+ mMinutes_mRectF_Bottom);
			
		}else
			if( mMinutes_mRectF_Bottom < time_pt_h ){
				mMinutes_mRectF_Top = time_pt_h/2 ;
				mMinutes_mRect_Top = time_pt_h/2 ;
				mMinutes_mRect_Bottom = time_pt_h;
				mMinutes_mRectF_Bottom = mMinutes_mRectF_Bottom+((time_pt_h-mMinutes_mRectF_Bottom)<5? (time_pt_h-mMinutes_mRectF_Bottom): 5) ;
				mShendu_Time_Hour_Up = mShendu_Current_Time_Hour;
				mShendu_Time_Minutes_Up = mShendu_Current_Time_Minutes;
			    MyLog("mRectF_bottom 1 = "+ mMinutes_mRectF_Bottom);
			}else{
				MyLog("mRectF_bottom 1 = "+ false);
				isStop = false;
			}
		if(mIsHourChange){
			mHour_mRect_Top = mMinutes_mRect_Top;
			mHour_mRect_Bottom = mMinutes_mRect_Bottom;
			mHour_mRectF_Top = mMinutes_mRectF_Top;
			mHour_mRectF_Bottom = mMinutes_mRectF_Bottom;
		}
		
		invalidate();

	}

	private boolean isStop = true;
	int i =0;
	@Override
	protected void onTimeChanged() {
		// TODO Auto-generated method stub
		
	super.onTimeChanged();
	mShendu_Current_Time_Minutes = getmTime_Minutes()/*i+++""*/;
		final int time_pt_h = mTime_Mask_Pt_Hour.getHeight();
			isStop = true;
			if(mShendu_Previous_Time_Minutes.equals(mShendu_Current_Time_Minutes)){
				return;
			}
			mShendu_ui_thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mShendu_Tiem_Deck = mShendu_Previous_Time_Minutes;
					mMinutes_mRectF_Bottom = 0;
					mMinutes_mRectF_Top = 0;
					
					while(isStop){
						mHandler.sendEmptyMessage(1);
						try{
							Thread.sleep(14);
						}catch(InterruptedException e){
//						          setViewNumber(true);
							  e.printStackTrace();
							  Thread.currentThread().interrupt();
						}
					}
					/** shutao 2012-9-27*/
					MyLog("hour_mRectF_Top ---stop");
					mShendu_Previous_Time_Minutes = mShendu_Current_Time_Minutes;
					mShendu_Tiem_Deck = mShendu_Current_Time_Minutes;
					if(mHour_mRectF_Top < time_pt_h/2 || mHour_mRectF_Bottom < time_pt_h ){
						MyLog("interrupt"+mHour_mRectF_Top);
						setViewNumber(true);
					}
					if (mShendu_ui_thread != null){
						MyLog("interrupt");
						mShendu_ui_thread.interrupt();
				
					}
				}
			});
			mShendu_ui_thread.setPriority(Thread.MIN_PRIORITY);
			mShendu_ui_thread.start();

		if(mShendu_Current_Time_Hour.equals(getmTime_Hour())){
			if(mHour_mRectF_Top < time_pt_h/2 || mHour_mRectF_Bottom < time_pt_h ){
				MyLog("hour_mRectF_Top ---stop"+mHour_mRectF_Top);
				mIsHourChange = true;
			}else{
			mShendu_Previous_Time_Hour = mShendu_Current_Time_Hour;
			mIsHourChange = false;
			}
		}else{
			mShendu_Current_Time_Hour = getmTime_Hour();
			mIsHourChange = true;
		}

		MyLog("onTimeChanged"+isStart());
	}
	
	private void recycleTimeBitmap(){
		if(mTime_Hour_Overhead != null){
			mTime_Hour_Overhead.recycle();
			mTime_Hour_Lower.recycle();
			mTime_Minutes_Overhead.recycle();
			mTime_Minutes_Lower.recycle();
		}
	}
	
	
	
	private boolean isOpen = false;

	private void MyLog(String msg) {
		if (isOpen) {
			Log.d("1616", msg);
		}
	}
}
