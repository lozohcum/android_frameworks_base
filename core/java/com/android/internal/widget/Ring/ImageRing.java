package com.android.internal.widget.Ring;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Bitmap;

public class ImageRing implements ViewInterface{
	private Control control;
	private RectF roundRectF = null;
	private Rect moveRect = new Rect();
	private Paint whitePaint = null;
	private int dotOffset;
	private int iconBottomOffset;
	private RingItem bottomItem = null;
	private float[] dotP = new float[2];		// x,y
	
	public ImageRing(){
		control = Control.getInstance();
		reset();
		control.logd("ImageRing()");
	}
	@Override
	public void reset(){
		
		roundRectF = new RectF(
				control.centPoint[0] - control.bgRadius,
				control.centPoint[1] - control.bgRadius,
				control.centPoint[0] + control.bgRadius,
				control.centPoint[1] + control.bgRadius
				);
				
		if(control.ic_dot_30!=null){
			dotOffset = control.ic_dot_30.getWidth()>>1;
		}
		if(control.ic_bottom!=null){
			iconBottomOffset = control.ic_bottom.getWidth()>>1;
		}
		
		whitePaint = new Paint();
		whitePaint.setAntiAlias(true);
		whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		whitePaint.setColor(0);
		whitePaint.setAlpha(64);
		
		dotP[0] = control.centPoint[0]-dotOffset;
		dotP[1] = control.centPoint[1]+control.dotOffset[0];
	}
	@Override
	public void draw(Canvas canvas) {
		if(control.isImageMove){
			/*
			canvas.clipRect(control.viewRectF);
			moveRect.set(
				(int)(control.moveBgRectF.left+control.posFix[0]),
				(int)(control.moveBgRectF.top+control.posFix[1]),
				(int)(control.moveBgRectF.right+control.posFix[0]),
				(int)(control.moveBgRectF.bottom+control.posFix[1])
				);
			canvas.drawRoundRect(roundRectF, control.bgRadius, control.bgRadius, mPaint);
			*/
			int alpha = control.imagePaint.getAlpha();
			control.imagePaint.setAlpha(255);
			canvas.drawBitmap(control.ic_move_bg, 
						(float)control.moveBgRectF.left+control.posFix[0], 
						(float)control.moveBgRectF.top+control.posFix[1], 
						control.imagePaint);
			control.imagePaint.setAlpha(alpha);
			canvas.drawBitmap(control.ic_head_bg_hl,
						 (float)control.headBgLightRectF.left+control.posFix[0], 
						 (float)control.headBgLightRectF.top+control.posFix[1], 
						 control.imagePaint);	// 背景
						 
			if(control.holdImage!=null)
			canvas.drawBitmap(control.holdImage, 
						(float)control.imageRectF.left+control.posFix[0], 
						(float)control.imageRectF.top+control.posFix[1], 
						control.imagePaint);
						
			canvas.drawBitmap(control.ic_head_top, 
						(float)control.headTopRectF.left+control.posFix[0], 
						(float)control.headTopRectF.top+control.posFix[1], 
						control.imagePaint);
					
			return;
		}
		if(control.ic_head_bg!=null && !control.ic_head_bg.isRecycled())
			canvas.drawBitmap(control.ic_head_bg, control.headBgRectF.left, control.headBgRectF.top, control.imagePaint);	// 白色背景
		else	control.loge("ic_head_bg ==null or isRecycled");
		
		if(control.holdImage!=null && !control.holdImage.isRecycled())
			canvas.drawBitmap(control.holdImage, control.imageRectF.left, control.imageRectF.top, control.imagePaint);		// 头像
		else	control.loge("holdImage ==null or isRecycled");
		
		if(control.isArcChargeShow && control.userChargeShow)					
			canvas.drawCircle(control.centPoint[0], control.centPoint[1], control.imageRadius, whitePaint);	// 充电半透明层
			
		if(control.ic_head_top!=null && !control.ic_head_top.isRecycled())
			canvas.drawBitmap(control.ic_head_top, control.headTopRectF.left, control.headTopRectF.top, control.imagePaint);//半透明层
		else	control.loge("ic_head_top ==null or isRecycled");
		
		if(bottomItem==null){
			for(RingItem ri: control.itemList){
				if(ri.position == Control.DOWN){
					bottomItem = ri;
					break;
				}
			}
		}else{
			// 圆点
			// sorry for realize animation use this function, foreach done this not easy than that
			if(!control.dotAlready)	 {control.loge("image dot is not already!");return;}
			synchronized(control.dotOffsetRoll){
				switch(control.dotOffsetRoll){
				case 0:
					canvas.drawBitmap(control.dotList.get(3),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				case 1:
					canvas.drawBitmap(control.dotList.get(2),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(3),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				case 2:
					canvas.drawBitmap(control.dotList.get(1),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(2),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(3),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				case 3:
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(1),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(2),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(3),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				case 4:
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(1),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(2),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				case 5:
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(1),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				default:
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1] ,control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 2*control.dotOffset[1],control.imagePaint);
					canvas.drawBitmap(control.dotList.get(0),dotP[0],dotP[1]+ 3*control.dotOffset[1],control.imagePaint);
					break;
				}// end switch
				// 小锁
				if(control.dotOffsetRoll>3 && control.dotOffsetRoll<=6)
					canvas.drawBitmap(bottomItem.iconNormal, bottomItem.ltX, bottomItem.ltY, control.imagePaint);
				else 
					canvas.drawBitmap(control.dotList.get(4), bottomItem.ltX, bottomItem.ltY, control.imagePaint);
			}
		}
	}
	
}
