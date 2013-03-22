package com.android.internal.widget.Ring;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap;

public class OutRing implements ViewInterface{
	private Paint mPaint = new Paint();
	private Control control;
	private RectF mShadeArcRectF = new RectF();
	private int alpha = 45;
	/*
	 * 坐标系 右下
	 */
	private int fix[][] = {{0,2},	// 修正位置 下
						{1,1},		// 修正位置 右下
						{2,0},		// 修正位置 右
						{1,-1},		// 修正位置 右上
						{0,-2},		// 修正位置 上
						{-1,-1},	// 修正位置 左上
						{-2,0},		// 修正位置 左
						{-1,1}};	// 修正位置 左下
	
	public OutRing() {
		control = Control.getInstance();
		
		mPaint.setAntiAlias(true);
		//reset();
	}
	@Override
	public void reset(){
		mShadeArcRectF.set(control.centPoint[0]-control.bgRadius,
					control.centPoint[1]-control.bgRadius-1,
					control.centPoint[0]+control.bgRadius,
					control.centPoint[1]+control.bgRadius-1
					);
		for(RingItem ri: control.itemList){
			int fx = fix[ri.position][0];
			int fy = fix[ri.position][1];
			
			float tmpx = control.centPoint[0] + (float)0.5*control.bgRadius*fx - (fx>0?ri.width+control.iconOffset :(fx<0?-control.iconOffset:ri.width>>1));		// 图标左上角 x
			float tmpy = control.centPoint[1] + (float)0.5*control.bgRadius*fy - (fy>0?ri.height+control.iconOffset:(fy<0?-control.iconOffset:ri.height>>1));	// 图标左上角 y
			
			ri.setPosition(tmpx,tmpy);
			
		}
	}
	@Override
	public void draw(Canvas canvas) {
		if(!control.isBgRingShow) return;
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setAlpha(alpha<<1);
		canvas.drawCircle(control.centPoint[0], control.centPoint[1], control.bgRadius, mPaint);
		
		mPaint.setColor(Color.WHITE);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setAlpha(alpha);
		canvas.drawArc(mShadeArcRectF, -3, 176, false, mPaint);
		
		mPaint.setAlpha(255);
		if(control.itemList.size()==0)	control.loge("itemList is null!!");
		for(RingItem ri: control.itemList){
			Bitmap bm = ri.isActive? ri.iconClick: ri.iconNormal;
			if(bm!=null){
				canvas.drawBitmap(bm, ri.ltX, ri.ltY, mPaint);
			}else{
				canvas.drawText("n/a",ri.ltX,ri.ltY,mPaint);
				control.loge("OutRing [item] bitmap is null!!["+ri.position 
							+ "]----["+ri.ltX+","+ri.ltY
							+"]----(normal:"+(ri.iconNormal!=null)+", click:"+(ri.iconClick!=null) );
			}
		}
	}
}
