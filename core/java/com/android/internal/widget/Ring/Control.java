package com.android.internal.widget.Ring;

import java.util.ArrayList;

import com.android.internal.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.Resources;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.content.ContentUris;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.net.Uri;
import java.io.InputStream;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.os.Vibrator;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.graphics.BlurMaskFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Control{
	public static boolean debug = true;
	public static final String TAG = "MulitRingView";
	private static final String SYSTEM_FONT_DATE = "/system/fonts/DroidSansFallback.ttf";
	public static String chargeing ;
	public static String batteryFull;
	public static final Typeface chargeFont;
    static{
    	chargeFont = Typeface.createFromFile(SYSTEM_FONT_DATE);
    }
    
   private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Control.init = false;
			logd("unlocked! will unregister unlockReceiver");
			context.unregisterReceiver(this);
			if(exec!=null){
				exec.execute(new Runnable(){
					public void run(){
						doClean();
					}});
			}else doClean();
			
		}
	};
		
	public static final int DOWN 		= 0;
	public static final int RIGHT_DOWN	= 1;
	public static final int RIGHT		= 2;
	public static final int RIGHT_UP 	= 3;
	public static final int UP 			= 4;
	public static final int LEFT_UP		= 5;
	public static final int LEFT 		= 6;
	public static final int LEFT_DOWN	= 7;
	
	private static final int OK = 200;
	
	private static final Integer ICON = 0xfcc0;
	private static final Integer BACKGROUND = 0xfcc1;
	private static final Integer DOTA = 0xfcc2;
	private static final Integer HOLDER = 0xfcc2;
	public static boolean init = false;
	
	private MulitRingView.OnTriggerListener mOnTriggerListener;
	private View mainView;
	private Resources mResource;
	private TypedArray mTypeArray;
	private Vibrator mVibrator = null;
	public ExecutorService exec;
	
	private boolean chargeRuning = true;
	public boolean isRunning = true;
	public boolean dotAlready = false;
	private boolean isDisable = true;			// 禁止解锁
	private boolean vibrateEnabled = true;		// setting 震动
	boolean isBgRingShow = false;				// 是否显示外环
	boolean isArcChargeShow = false;			// 是否显示充电弧
	boolean userChargeShow = true;
	boolean isImageMove = false;				// 是否移动中
	
	private boolean isNeedReset = true;
	
	
	Bitmap holdImage		=null;				// 内环图片
	Bitmap ic_head_bg		=null;
	Bitmap ic_head_bg_hl	=null;
	Bitmap ic_head_top		=null;
	Bitmap ic_move_bg		=null;
	Bitmap ic_dot_20		=null;
	Bitmap ic_dot_30		=null;
	Bitmap ic_dot_50		=null;
	Bitmap ic_dot_70		=null;
	Bitmap ic_bottom_anmi	=null;
	Bitmap ic_bottom		=null;
	Bitmap ic_bottom_activated=null;
	Bitmap ic_left			=null;
	Bitmap ic_left_activated=null;
	Bitmap ic_right			=null;
	Bitmap ic_right_activated=null;
	Bitmap ic_up			=null;
	Bitmap ic_up_activated	=null;
	
	Paint 	imagePaint;								// 内环paint
	Paint	testPaint;
	
	ArrayList<Bitmap> 				dotList ;
	ArrayList<RingItem>				itemList;		// 外环item
	ArrayList<ViewInterface>		viewList;		//  图层
	
	RectF chargeRectF = new RectF();				// 充电区域
	RectF imageRectF = new RectF();					// 原始区域
	RectF viewRectF = new RectF();					// 移动区域
	RectF headBgRectF = new RectF();
	RectF headBgLightRectF = new RectF();
	RectF headTopRectF = new RectF();
	RectF moveBgRectF = new RectF();
	
	
	private float alphaSection ;						// 透明度
	private float[] posHold = new float[2];		// 原始点击点
	float[] posFix = {0.0f,0.0f};				// 点击位置修正距离
	float[] centPoint = new float[2];			// view 中心点
	float chargeTextBigSize=55.0f, chargeTextSmallSize=28.0f;	// 充电文字大小
	
	
	private int mVibrationDuration = 20;
	private int moveOffset;
	private int temp;
	int chargeTextColor = 0xffe7e7e7;
	
	int[] dotOffset = new int[2];
	int[] viewSize = new int[2];				// view 尺寸
	int chargeAngle = 0;						// 充电弧线角度
	int imageRadius,bgRadius,chargeRadius,chargeWidth=4;			// 半径
	int batteryLevel = 0;
	int iconOffset = 0;
	Integer dotOffsetRoll = 0;			// 滚动的小点
	
	private int defaultImageId,iconLeftId,iconUpId,iconRightId,iconBottomId,iconLeftActiveId,iconUpActiveId,iconRightActiveId,iconBottomActiveId;
	
	public void setViewSize(int width,int height){
		if((width>0 &&height>0) &&(viewSize[0]!=width && viewSize[1]!=height)){
			viewSize[0] = width;
			viewSize[1] = height;
			reset();
		}
	}
	
	private Handler mHander = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case OK:
				//init = true;
				if(mainView!=null)	mainView.invalidate();
			}
			super.handleMessage(msg);
		}
	};
	/**
	 * 初始化
	 */
	public void init(View view,TypedArray a){
		
		logd("MulitRingView.control init");
			long t1 = System.currentTimeMillis();
			this.mainView = view;
		if(!init){
			init = true;
			mResource = view.getResources();
			isRunning = false;
			chargeRuning = false;
		
			this.mainView.getContext().registerReceiver(unlockReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
			logd("first init registerReceiver unlock");
			
			exec = Executors.newFixedThreadPool(5);
			mVibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
			chargeing = mResource.getString(R.string.lockscreen_charging);
			batteryFull = mResource.getString(R.string.lockscreen_charged);
			
			this.mTypeArray = a;
			// view 尺寸
			viewSize[0] = 480;//580;
			viewSize[1] = 500;//580;
			// 半径
			bgRadius = 230;//290;
			imageRadius = 100;//115;
		
			dotOffset[0] = 64;
			dotOffset[1] = 8;
			imageRadius = 	(int)mTypeArray.getDimension(R.styleable.MulitRingView_innerRadius, imageRadius);
		    bgRadius 	= 	(int)mTypeArray.getDimension(R.styleable.MulitRingView_outerRadius, bgRadius);
		    iconOffset	=	mTypeArray.getInt(R.styleable.MulitRingView_iconOffset,iconOffset);
		    centPoint[1]=	mTypeArray.getDimension(R.styleable.MulitRingView_ringCenterPaddingTop, centPoint[1]);
		    dotOffset[0]	=(int)mTypeArray.getDimension(R.styleable.MulitRingView_dotOffsetFromCenter,dotOffset[0]);
		    dotOffset[1]	=(int)mTypeArray.getDimension(R.styleable.MulitRingView_dotOffsetSpan,dotOffset[1]);
		    chargeWidth		=(int)mTypeArray.getDimension(R.styleable.MulitRingView_chargeWidth,chargeWidth);
		    chargeTextColor		= mTypeArray.getInt(R.styleable.MulitRingView_chargeTextColor,chargeTextColor);
		    mVibrationDuration  = mTypeArray.getInt(R.styleable.MulitRingView_vibrateDuration,mVibrationDuration);
		    chargeTextBigSize	= mTypeArray.getDimension(R.styleable.MulitRingView_chargeTextBigSize,chargeTextBigSize);
		    chargeTextSmallSize	= mTypeArray.getDimension(R.styleable.MulitRingView_chargeTextSmallSize,chargeTextSmallSize);
		    
		    defaultImageId = getResourceId(R.styleable.MulitRingView_defaultImage);
		    
		  	iconLeftId = getResourceId(R.styleable.MulitRingView_iconLeft);
		    iconLeftActiveId = getResourceId(R.styleable.MulitRingView_iconLeftActive);
		    
		    iconUpId = getResourceId(R.styleable.MulitRingView_iconUp);
		    iconUpActiveId = getResourceId(R.styleable.MulitRingView_iconUpActive);
		    
		    iconRightId = getResourceId(R.styleable.MulitRingView_iconRight);
		    iconRightActiveId = getResourceId(R.styleable.MulitRingView_iconRightActive);
		    
		    iconBottomId = getResourceId(R.styleable.MulitRingView_iconBottom);
		    iconBottomActiveId = getResourceId(R.styleable.MulitRingView_iconBottomActive);
		    
			moveOffset = bgRadius - (iconOffset<<1);
			chargeRadius = imageRadius+chargeWidth;
		
			imagePaint = new Paint();

			dotAlready = false;
			exec.execute(extLoadRunnable);
			exec.execute(iconLoadRunnable);
			exec.execute(holdLoadRunnable);
		
			viewList = new ArrayList<ViewInterface>();
			viewList.add(new OutRing());
			viewList.add(new ImageRing());
			viewList.add(new ChargeArc());
			
		}
		loge("init time="+(System.currentTimeMillis()-t1));
	}
	
	private Runnable extLoadRunnable = new Runnable(){
		public void run(){
		
			imagePaint.setStyle(Paint.Style.STROKE);
			imagePaint.setAntiAlias(true);
			imagePaint.setColor(Color.BLUE);
			imagePaint.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.INNER));
			
			testPaint = new Paint(imagePaint);
			testPaint.setColor(Color.RED);
		
			alphaSection = 255.0f/(bgRadius-imageRadius+10);			// 透明度分段
			vibrateEnabled = Settings.System.getInt(
							mainView.getContext().getContentResolver()
							,Settings.System.LOCKSCREEN_VIBRATE_ENABLED, 1) == 1;
							
			loadDrawableDot();
		}
	};
	
	private Runnable iconLoadRunnable = new Runnable(){
		public void run(){
		 	loadIconBitmap();
    		
    		itemList = new ArrayList<RingItem>();
			itemList.add(new RingItem(ic_bottom	,ic_bottom_activated	,Control.DOWN	,-1,MulitRingView.OnTriggerListener.UNLOCK,	null));		// down
			itemList.add(new RingItem(ic_right	,ic_right_activated		,Control.RIGHT	,-1,MulitRingView.OnTriggerListener.SMS,	null));		// right
			itemList.add(new RingItem(ic_up		,ic_up_activated		,Control.UP		,-1,MulitRingView.OnTriggerListener.CAMERA,	null));		// up
			itemList.add(new RingItem(ic_left	,ic_left_activated		,Control.LEFT	,-1,MulitRingView.OnTriggerListener.CALL,	null));		// left
    		reset();
		}
	};
	
	private Runnable holdLoadRunnable = new Runnable(){
    	public void run(){
    		loadDrawableBg();
    		synchronized (HOLDER) {
    			holdImage = getHolderImage();
    			holdImage.prepareToDraw();
    		}
    		reset();
    	}
    };
	
	private int getResourceId(int id) {
        TypedValue tv = null;
        if(mTypeArray!=null)
        	tv = mTypeArray.peekValue(id);
        return tv == null ? 0 : tv.resourceId;
    }
	
	private void loadIconBitmap(){
		synchronized (ICON) {
			loadDrawable(Control.DOWN, iconBottomId,iconBottomActiveId);
			loadDrawable(Control.RIGHT,	iconRightId	,iconRightActiveId);
			loadDrawable(Control.UP,	iconUpId	,iconUpActiveId);
			loadDrawable(Control.LEFT,	iconLeftId	,iconLeftActiveId);
			
			logd("loadIconBitmap"+"left["+(ic_left!=null)+","+(ic_left_activated!=null)+"]"
							+"right["+(ic_right!=null)+","+(ic_right_activated!=null)+"]"
							+"up["+(ic_up!=null)+","+(ic_up_activated!=null)+"]"
							+"bottom["+(ic_bottom!=null)+","+(ic_bottom_activated!=null)+"]"
				);
		}
	}
	private void loadDrawable(int position,int id,int actId){
		switch(position){
		case Control.DOWN:
			if(ic_bottom==null)				ic_bottom = loadDrawable(id>0? id:R.drawable.ic_lock_unlock);
			if(ic_bottom_activated==null)	ic_bottom_activated = loadDrawable(actId>0?actId:R.drawable.ic_lock_unlock_activated);
			break;
			
		case Control.LEFT:
			if(ic_left==null)				ic_left = loadDrawable(id>0? id:R.drawable.ic_lock_call);
			if(ic_left_activated==null)		ic_left_activated = loadDrawable(actId>0?actId:R.drawable.ic_lock_call_activated);
			break;
			
		case Control.UP:
			if(ic_up==null)					ic_up = loadDrawable(id>0? id:R.drawable.ic_lock_camera);
			if(ic_up_activated==null)		ic_up_activated = loadDrawable(actId>0?actId:R.drawable.ic_lock_camera_activated);
			break;
			
		case Control.RIGHT:
			if(ic_right==null)				ic_right = loadDrawable(id>0? id:R.drawable.ic_lock_sms);
			if(ic_right_activated==null)	ic_right_activated = loadDrawable(actId>0?actId:R.drawable.ic_lock_sms_activated);
			break;
		}
	}
	// load ico for dot
	private void loadDrawableDot(){
		synchronized (DOTA) {
			if(ic_dot_20==null)	ic_dot_20 = loadDrawable(R.drawable.ic_lock_dot_20);
			if(ic_dot_30==null)	ic_dot_30 = loadDrawable(R.drawable.ic_lock_dot_30);
			if(ic_dot_50==null)	ic_dot_50 = loadDrawable(R.drawable.ic_lock_dot_50);
			if(ic_dot_70==null)	ic_dot_70 = loadDrawable(R.drawable.ic_lock_dot_70);
			if(ic_bottom_anmi==null)	ic_bottom_anmi = loadDrawable(R.drawable.ic_lock_unlock_anmi);
		
			if(dotList==null)	dotList = new ArrayList<Bitmap>();
			else				dotList.clear();
		
			dotList.add(ic_dot_20);
			dotList.add(ic_dot_30);
			dotList.add(ic_dot_50);
			dotList.add(ic_dot_70);
			dotList.add(ic_bottom_anmi);
		}
		
		dotAlready = true;	
	}
	// load ico for background
	private void loadDrawableBg(){
		synchronized (BACKGROUND) {
			if(ic_head_bg==null)	ic_head_bg 	= loadDrawable(R.drawable.ic_lock_header_bg);
			if(ic_head_bg_hl==null)	ic_head_bg_hl = loadDrawable(R.drawable.ic_lock_header_bg_hl);
			if(ic_head_top==null)	ic_head_top = loadDrawable(R.drawable.ic_lock_header_top);
			if(ic_move_bg==null)	ic_move_bg = loadDrawable(R.drawable.ic_lock_move_bg);
			logd("loadDrawableBg ic_head_bg:["+(ic_head_bg!=null)
					+"] ic_head_bg_hl:["+(ic_head_bg_hl!=null)
					+"] ic_head_top:["+(ic_head_top!=null)
					+"] ic_move_bg:["+(ic_move_bg!=null)+"]"
					);
		}
	}
	private Bitmap loadDrawable(int rId){
		Bitmap bitmap =  BitmapFactory.decodeResource(mResource, rId);
		bitmap.prepareToDraw();
		return bitmap;
	}
	
	private void releaseDrawable(){
		logd("releaseDrawable");
		if(init) { logd("release init return 1");return;}
		synchronized (DOTA) {
			if(ic_dot_20 != null) {
				ic_dot_20.recycle();
				ic_dot_20=null;
			}
			if(ic_dot_30 != null) {
				ic_dot_30.recycle();
				ic_dot_30=null;
			}
			if(ic_dot_50 != null) {
				ic_dot_50.recycle();
				ic_dot_50=null;
			}
			if(ic_dot_70 != null) {
				ic_dot_70.recycle();
				ic_dot_70=null;
			}
			if(ic_bottom_anmi != null) {
				ic_bottom_anmi.recycle();
				ic_bottom_anmi=null;
			}
		}
		
		synchronized (BACKGROUND) {
			if(ic_head_bg != null) {
				ic_head_bg.recycle();
				ic_head_bg=null;
			}
			if(ic_head_bg_hl != null) {
				ic_head_bg_hl.recycle();
				ic_head_bg_hl=null;
			}
			if(ic_head_top != null) {
				ic_head_top.recycle();
				ic_head_top=null;
			}
			if(ic_move_bg != null) {
				ic_move_bg.recycle();
				ic_move_bg=null;
			}
		}
		synchronized (HOLDER) {
			if(holdImage != null && !init) {
				holdImage.recycle();
				holdImage=null;
			}
		}
		synchronized (ICON) {
			if(ic_up != null) {
				ic_up.recycle();
				ic_up=null;
			}
			if(ic_up_activated != null) {
				ic_up_activated.recycle();
				ic_up_activated=null;
			}
			if(ic_bottom != null) {
				ic_bottom.recycle();
				ic_bottom=null;
			}
			if(ic_bottom_activated != null) {
				ic_bottom_activated.recycle();
				ic_bottom_activated=null;
			}
			if(ic_left != null) {
				ic_left.recycle();
				ic_left=null;
			}
			if(ic_left_activated != null) {
				ic_left_activated.recycle();
				ic_left_activated=null;
			}
			if(ic_right != null) {
				ic_right.recycle();
				ic_right=null;
			}
			if(ic_right_activated != null) {
				ic_right_activated.recycle();
				ic_right_activated=null;
			}
		}

	}
	
	private void reset(){
		logd("onreset");
		// view 中心点
		float cTmp = (bgRadius<<1)>viewSize[0]? bgRadius:viewSize[0]>>1;	
		
		if((centPoint[0]-cTmp) >0.5 || (centPoint[0]-cTmp) <-0.5){
			centPoint[0] = cTmp;
			isNeedReset = true;
		}else{
			isNeedReset = false;
		}
		//centPoint[1] = (centPoint[1] -bgRadius)<viewSize[1] ? centPoint[1] : viewSize[1]>>1;
		
		//if(!init) return;
		//  充电弧线
		chargeRectF.set(centPoint[0] - chargeRadius,
				centPoint[1] - chargeRadius, 
				centPoint[0] + chargeRadius, 
				centPoint[1] + chargeRadius);
		
		// 图片区域
		if(holdImage==null)
			imageRectF.set(centPoint[0]-imageRadius,
							centPoint[1]-imageRadius, 
							centPoint[0]+imageRadius,
							centPoint[1]+imageRadius);
		else
			imageRectF.set(centPoint[0]-(holdImage.getWidth()>>1),
					centPoint[1]-(holdImage.getHeight()>>1),
					centPoint[0]+(holdImage.getWidth()>>1),
					centPoint[1]+(holdImage.getHeight()>>1)
				);
					
		if(ic_head_bg!=null)
			headBgRectF.set(centPoint[0]-(ic_head_bg.getWidth()>>1),
					centPoint[1]-(ic_head_bg.getHeight()>>1),
					centPoint[0]+(ic_head_bg.getWidth()>>1),
					centPoint[1]+(ic_head_bg.getHeight()>>1)
				);
		else
			loge("onreset ic_head_bg==null");
		if(ic_head_bg_hl!=null)
			headBgLightRectF.set(centPoint[0]-(ic_head_bg_hl.getWidth()>>1),
					centPoint[1]-(ic_head_bg_hl.getHeight()>>1),
					centPoint[0]+(ic_head_bg_hl.getWidth()>>1),
					centPoint[1]+(ic_head_bg_hl.getHeight()>>1)
				);
		else
			loge("onreset ic_head_bg_hl==null");
		if(ic_head_top!=null)
			headTopRectF.set(centPoint[0]-(ic_head_top.getWidth()>>1),
					centPoint[1]-(ic_head_top.getHeight()>>1),
					centPoint[0]+(ic_head_top.getWidth()>>1),
					centPoint[1]+(ic_head_top.getHeight()>>1)
				);
		else
			loge("onreset ic_head_top==null");
		if(ic_move_bg!=null)
			moveBgRectF.set(centPoint[0]-(ic_move_bg.getWidth()>>1),
					centPoint[1]-(ic_move_bg.getHeight()>>1),
					centPoint[0]+(ic_move_bg.getWidth()>>1),
					centPoint[1]+(ic_move_bg.getHeight()>>1)
				);
		else
			loge("onreset ic_move_bg==null");
		
		viewRectF.set(centPoint[0] - bgRadius,
						centPoint[1] - bgRadius, 
						centPoint[0] + bgRadius, 
						centPoint[1] + bgRadius);
						
		for(ViewInterface vi: viewList){
			vi.reset();
		}
		if(mainView!=null)	mainView.postInvalidate();
		//if(debug) dump();
		logd("ret over left,right,top,bottom");
		logw("imageRectF:["+imageRectF.left+","+imageRectF.right+","+imageRectF.top+","+imageRectF.bottom+"]");
		logw("headBgRectF:["+headBgRectF.left+","+headBgRectF.right+","+headBgRectF.top+","+headBgRectF.bottom+"]");
		logw("headBgLightRectF:["+headBgLightRectF.left+","+headBgLightRectF.right+","+headBgLightRectF.top+","+headBgLightRectF.bottom+"]");
		logw("headTopRectF:["+headTopRectF.left+","+headTopRectF.right+","+headTopRectF.top+","+headTopRectF.bottom+"]");
		logw("moveBgRectF:["+moveBgRectF.left+","+moveBgRectF.right+","+moveBgRectF.top+","+moveBgRectF.bottom+"]");
		logw("viewRectF:["+viewRectF.left+","+viewRectF.right+","+viewRectF.top+","+viewRectF.bottom+"]");
		System.gc();
	}
	
	/**
	 * 线程，动画
	 */
	private Runnable chargeRunable = new Runnable(){
		public void run(){
			try {
				logd("chargeRunnable start");
				chargeRuning = true;
				while(isRunning && chargeRuning){
					if(0==chargeAngle)	chargeAngle = 360;			// 逆时针
					chargeAngle --;
					mainView.postInvalidateOnAnimation();
					Thread.sleep(75);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				logd("===========chargeRunnable interrupt");
			}finally{
				chargeRuning = false;
			}
		}
	};
	
	public Runnable dotRunnable = new Runnable(){
		public void run(){
			try{
				logd("dotRunnable start");
				isRunning = true;
				while(isRunning){
					synchronized(dotOffsetRoll){
						dotOffsetRoll = (++dotOffsetRoll)%(dotList.size()<<2);
					}
					mainView.postInvalidateOnAnimation();
					Thread.sleep(200);
				}
			} catch(InterruptedException e){
				e.printStackTrace();
				logd("===========dotRunnable interrupt");
			}
		}
	};
	// action up 手指离开屏幕
	private void actionUp(MotionEvent event){
	boolean isActive = false;
		if(mOnTriggerListener!=null){
			RingItem holder = null;
			for(RingItem ri: itemList){
				if(ri.isActive){
					holder = ri;
					isActive = true;
					break;
				}
			}
			if(holder!=null && !isDisable){
				logd("holder.type="+holder.funType+" |classname="+holder.className);
				mOnTriggerListener.onTrigger(holder.funType,holder.className);
			}
		}
		if(!isActive){
			isBgRingShow = false;
			userChargeShow = true;
			isImageMove = false;
			isDisable = true;
			imagePaint.setAlpha(255);
			posFix[0] = 0;
			posFix[1] = 0;
		}
		if(event!=null)	logd("onAction Up:["+event.getX()+":"+event.getY()+"]");
	}
	// action down 手指接触屏幕
	private void actionDown(MotionEvent event){
		if(isInRing(event)){	// 点击区域
			isBgRingShow = true;
			userChargeShow = false;
			isDisable = false;
			logw("inRing arec!");
		}
		if(event!=null)	logd("onAction down:["+event.getX()+":"+event.getY()+"],["+centPoint[0]+":"+centPoint[1]+"]");
	}
	// action move 手指在屏幕上滑动
	private void actionMove(MotionEvent event){
		if(!isImageMove) return;			// 超出可移动范围
		
		float eventX =  event.getX();
        float eventY =  event.getY();
		
//		float rx=event.getRawX();
//		float ry=event.getRawY();
        // 位置修正
        {
			float tx = eventX - posHold[0] ;		// 圆心移动的距离x
			float ty = eventY - posHold[1] ;
			float tRadius = (float) Math.sqrt((tx*tx)+(ty*ty));
			if(tRadius>=moveOffset){
			    float scale = bgRadius / tRadius;
			    eventX = posHold[0]+tx*scale;
			    eventY = posHold[1]+ty*scale;
			}
        }
		posFix[0] = eventX - posHold[0];
		posFix[1] = eventY - posHold[1];
		
		if(!isDisable)
		for(int i=0;i<itemList.size();i++){
			RingItem ri = itemList.get(i);
			ri.checkPosition(eventX,eventY);
			if(ri.isActive){
				if(ri.isActive!=ri.oldActive)	vibrate();
				posFix[0] = ri.cenX - centPoint[0];
				posFix[1] = ri.cenY - centPoint[1];
				break;
			}
		}
		
		
		// 透明度
		float distance = alphaSection * FloatMath.sqrt(posFix[0]*posFix[0]+posFix[1]*posFix[1]);
		distance = distance>255? 255:distance;
		imagePaint.setAlpha((int)(255-distance));
	}
	/*************点击区域*******/
	private boolean isInRing(MotionEvent event){
		float x=event.getX();
		float y=event.getY();
//		float rx=event.getRawX();
//		float ry=event.getRawY();
		if((imageRectF.left < x && imageRectF.right > x) && (imageRectF.top<y && imageRectF.bottom>y)){
			isImageMove = true;
			posHold[0] = x;
			posHold[1] = y;
			if(mOnTriggerListener!=null)
				mOnTriggerListener.onGrabbedStateChange(MulitRingView.OnTriggerListener.CENTER_HANDLE);
			return true;
		}
		return false;
	}
	/********************震动*************/
	private void vibrate() {
        if (mVibrator != null && vibrateEnabled) {
            mVibrator.vibrate(mVibrationDuration);
        }
    }
	/**
	 * 圆形框内显示图
	 */
	private static final String[]  project = new String[]{	Contacts._ID, Contacts.PHOTO_THUMBNAIL_URI};
									
	private Bitmap getHolderImage(){
		Bitmap bitmap = null;
		if(mainView==null) return null;
		String lockhead = Settings.System.getString(mainView.getContext().getContentResolver(), Settings.System.LOCKSCREEN_HEADSCULPTURE);
		try{
			if(lockhead!=null && !lockhead.isEmpty()){
				logd("used headSculpture picture");
				Context settingsContext = mainView.getContext().createPackageContext("com.android.settings", 0);
				String headSculptureFile = settingsContext.getFilesDir() + "/headSculpture";
				bitmap = BitmapFactory.decodeFile(headSculptureFile);	
				if(bitmap!=null)	return bitmap;//toRoundBitmap(bitmap,imageRadius);
				else loge("bitmap null!!");
			}
		 } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
		
		Cursor c = mainView.getContext().getContentResolver().query(Profile.CONTENT_URI, project, Contacts.IS_USER_PROFILE+"=1",null,null);
		if(c!=null){
			if(c.moveToFirst()&&c.getString(1)!=null){
				Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.getLong(0)); 
				InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mainView.getContext().getContentResolver(), uri,true); 
				bitmap = BitmapFactory.decodeStream(input);
			}
			c.close();
		}
		if(bitmap!=null)
			return toRoundBitmap(bitmap,imageRadius);
			
		bitmap = loadDrawable(defaultImageId>0? defaultImageId:R.drawable.default_header);
		return bitmap;//toRoundBitmap(bitmap,imageRadius);
	}
	/********************************************触摸事件**************************************************/
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			actionDown(event);
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			actionUp(event);
			break;
		case MotionEvent.ACTION_MOVE:
			actionMove(event);
			break;
		}
		if(mainView!=null)  mainView.invalidate();
		return true;
	}
	
	/**************************************** 回调接口*******************************************************/
	public void reset(boolean animate){
		logd("on reset("+animate+")");
		// 状态重置
		isDisable = true;
		isBgRingShow = false;
		userChargeShow = true;
		isImageMove = false;
		if(imagePaint!=null)
			imagePaint.setAlpha(255);
		posFix[0] = 0;
		posFix[1] = 0;
		for(RingItem ri:itemList){
			ri.isActive = false;
		}
	}
	public void clean(){
		logd("================relock with out unlock screen");
	}
	public void doClean(){
		logd("on doclean()");
		
    	if(mOnTriggerListener!=null)
				mOnTriggerListener.onGrabbedStateChange(MulitRingView.OnTriggerListener.NO_HANDLE);
		
    	isRunning = false;
        releaseDrawable();
        mOnTriggerListener = null;
    	if(exec!=null){
    		exec.shutdown();
    		exec.shutdownNow();
    		exec = null;
    	}
    	mResource = null;
    	mainView = null;
		System.gc();
    }
	
	/*************************************************lockscreen interface******************************************/
    public void setOnTriggerListener(MulitRingView.OnTriggerListener l) {
        mOnTriggerListener = l;
    }
    
    public boolean onExtStatusChange(int type,boolean show,boolean additional,int level){
    	boolean hander = false;
    	/*
    		com.android.internal.policy.impl.KeyguardStatusViewManager.ExtStatusChange
    		0	==	battery		(0,显示电量，是否充电，电池电量)
    		1	==	music		(1,unuse)
    	 */
    	switch(type){
    	case 0:
    		logd("charge status [show:additional:chargeRuning]=["+show+":"+additional+":"+chargeRuning+"]");
    		batteryLevel = level;
    		
    		isArcChargeShow = show && additional;
    		if(isArcChargeShow && batteryLevel<100 && !chargeRuning && exec!=null){
    			exec.execute(chargeRunable);
    		}else if(batteryLevel==100){
    			chargeRuning = false;
    		}
    		hander = true;
    		break;
    	case 1:
    	
    		hander = false;
    		break;
    	}
    	if(mainView!=null)mainView.postInvalidate();
    	System.gc();
		// 状态改变
		return hander;
	}
	
	/**************************************** 单例模式**************不用看*****************************************/
	private static Control cUtils;
	private Control(){}
	public static Control getInstance(){
		if(cUtils==null)
			cUtils = new Control();
		return cUtils;
	}
    /**
     * 转换图片成圆形
     * @param bitmap 传入Bitmap对象
     * @return
     */
	static PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(Mode.SRC_IN);
	
    public static Bitmap toRoundBitmap(Bitmap bitmap,float radius) {
    		if(radius<=0) return null;
            int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				float roundPx;
				float left,top,right,bottom,dst_left,dst_top,dst_right,dst_bottom;
				if (width <= height) {
					roundPx = width / 2;
					top = 0;
					bottom = width;
					left = 0;
					right = width;
					height = width;
					dst_left = 0;
					dst_top = 0;
					dst_right = width;
					dst_bottom = width;
				} else {
					roundPx = height / 2;
					float clip = (width - height) / 2;
					left = clip;
					right = width - clip;
					top = 0;
					bottom = height;
					width = height;
		    		dst_left = 0;
		    		dst_top = 0;
		    		dst_right = height;
		    		dst_bottom = height;
				}
				
				Bitmap output = Bitmap.createBitmap(width,height, Config.ARGB_8888);
		    	Canvas canvas = new Canvas(output);
				 final int color = 0xff424242;
				final Paint paint = new Paint();
				final Rect src = new Rect((int)left, (int)top, (int)right, (int)bottom);
				final Rect dst = new Rect((int)dst_left, (int)dst_top, (int)dst_right, (int)dst_bottom);
				final RectF rectF = new RectF(dst);
				paint.setAntiAlias(true);
				paint.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));
				canvas.drawARGB(0, 0, 0, 0);
				paint.setColor(color);
				canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
				paint.setXfermode(porterDuffXfermode);
				paint.setAntiAlias(true);
				canvas.drawBitmap(bitmap, src, dst, paint);
				bitmap.recycle();
				
				//float sxy = 2*radius/output.getWidth();
				//Matrix matrix = new Matrix();
				//matrix.postScale(sxy, sxy);
				
				//return Bitmap.createBitmap(output, 0, 0,output.getWidth(), output.getHeight(), matrix, true);
				
				int w_h = (int)(radius * 2);
				return Bitmap.createScaledBitmap(output,w_h,w_h,true);
    }
    /**
     * log
     */
	public void logd(String msg){
    	if(debug)
    		Log.d(TAG,msg);
    }
    public static void loge(String msg){
    	if(debug)
    		Log.e(TAG,msg);
    }
    public void logw(String msg){
    	if(debug)
    		Log.w(TAG,msg);
    }
	
	public void dump(){
		loge("======================MulitRingView.control:dump=================");
		logd("	debug:"+debug);
		logw("mOnTriggerListener:"+(mOnTriggerListener==null?"null":"not null"));
		logw("	isBgRingShow:"+isBgRingShow);
		logw("	isArcChargeShow:"+isArcChargeShow +" userChargeShow:"+userChargeShow);
		logw("	isImageMove:"+isImageMove);
		logw("itemList:"+itemList.size());
		logw("viewList:"+viewList.size());
		logd("RectF:[left,top,right,bottom]");
		logw("chargeRectF=["+chargeRectF.left+","+chargeRectF.top+","+chargeRectF.right+","+chargeRectF.bottom+"]");
		logw("imageRectF=["+imageRectF.left+","+imageRectF.top+","+imageRectF.right+","+imageRectF.bottom+"]");
		logw("viewRectF=["+viewRectF.left+","+viewRectF.top+","+viewRectF.right+","+viewRectF.bottom+"]");
		logw("headBgRectF=["+headBgRectF.left+","+headBgRectF.top+","+headBgRectF.right+","+headBgRectF.bottom+"]");
		logw("headBgLightRectF=["+headBgLightRectF.left+","+headBgLightRectF.top+","+headBgLightRectF.right+","+headBgLightRectF.bottom+"]");
		logw("headTopRectF=["+headTopRectF.left+","+headTopRectF.top+","+headTopRectF.right+","+headTopRectF.bottom+"]");
		logw("moveBgRectF=["+moveBgRectF.left+","+moveBgRectF.top+","+moveBgRectF.right+","+moveBgRectF.bottom+"]");
		logd("position:[x,y]");
		logw("viewSize=["+viewSize[0]+","+viewSize[1]+"]");
		logw("centPoint=["+centPoint[0]+","+centPoint[1]+"]");
		logw("chargeAngle=["+chargeAngle+"]");
		logw("posHold=["+posHold[0]+","+posHold[1]+"]");
		logw("posFix=["+posFix[0]+","+posFix[1]+"]");
		logd("radius");
		logw("bgRadius="+bgRadius);
		logw("imageRadius="+imageRadius);
		logw("chargeRadius="+chargeRadius);
		logd("alphaSection="+alphaSection);
		loge("======================MulitRingView.control:dump end=================");
	}
}
