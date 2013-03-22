package com.android.internal.widget.Ring;

import android.graphics.Bitmap;
import android.util.FloatMath;
import android.util.Log;

public class RingItem {
	float distance;		// 激活有效距离
	float ltX,ltY;		// 左上角坐标
	float cenX,cenY;	// 中心坐标
	
	int position;		// 在圆环中的位置
	int width,height;	
	
	int funType = MulitRingView.OnTriggerListener.UNLOCK;
	String className = null;
	Bitmap iconNormal,iconClick;
	boolean isActive = false;
	boolean oldActive = false;
	
	public RingItem(){}
	/**
	 *iconNormal 原始图
	 *iconClick	 点击效果图
	 *position 	 位置
	 *distance	 激活距离，-1 代表图片直径
	 *type		类型，默认 or 自定义
	 *className	
	 * if type==MulitRingView.OnTriggerListener.APP need this 
	 * 	full className for intent
	 */
	public RingItem(Bitmap iconNormal, Bitmap iconClick, int position, float distance, int type, String className){
		this.iconNormal = iconNormal;
		this.iconClick = iconClick==null? iconNormal : iconClick; 
		this.position = position;
		this.distance = distance;
		this.className = className;
		if(iconNormal!=null){
			this.width = iconNormal.getWidth();
			this.height = iconNormal.getHeight();
			this.distance = distance>0?	distance : 28+Math.max(iconNormal.getWidth(), iconNormal.getHeight());
		}
		if(type<0)
			funType = MulitRingView.OnTriggerListener.UNLOCK;
		else
			funType = type;
	}

	public void setCent(float cenX, float cenY) {
		this.cenX = cenX;
		this.cenY = cenY;
	}


	public void setBitmap(Bitmap iconNormal, Bitmap iconClick) {
		this.iconNormal = iconNormal;
		this.iconClick = iconClick;
		if(iconNormal!=null)
			distance = Math.max(iconNormal.getWidth(), iconNormal.getHeight());
	}

	public void setBitmap(Bitmap bitmap,boolean click){
		if(click){
			iconClick = bitmap;
		}else{
			if(bitmap!=null)
				distance = Math.max(bitmap.getWidth(), bitmap.getHeight());
			iconNormal = bitmap;
		}
	}
	public void setPosition(float x,float y){
		this.ltX = x;
		this.ltY = y;
		
		cenX = ltX + (iconNormal==null? 0 : iconNormal.getWidth()>>1);
		cenY = ltY + (iconNormal==null? 0 : iconNormal.getHeight()>>1);
	}
	
	public void checkPosition(float x,float y){
		int xx = (int) Math.abs(x-cenX);
		int yy = (int) Math.abs(y-cenY);
		
		double distance = Math.sqrt(xx*xx + yy*yy);
		if(this.distance>distance){
			oldActive = isActive;
			isActive = true;
		}else{
			oldActive = isActive;
			isActive = false;
		}
	}
}
