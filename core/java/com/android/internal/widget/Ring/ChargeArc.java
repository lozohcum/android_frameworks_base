package com.android.internal.widget.Ring;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;

public class ChargeArc implements ViewInterface {
	private static boolean useCenter = false;
	private Control control;
	public Paint textPaint;
	private Paint arcPaint;
	private int charegeColor = 0xff5bdb1a;
	
	private float chargeTextPosition0 = 5.0f, chargeTextPosition1=20.0f;
	private String msg = null;
	
	
	public ChargeArc() {
		control = Control.getInstance();

		arcPaint = new Paint();
		arcPaint.setAntiAlias(true);
		arcPaint.setStyle(Paint.Style.STROKE);
		arcPaint.setColor(charegeColor);
		arcPaint.setStrokeWidth(control.chargeWidth+1);
		arcPaint.setStrokeCap(Paint.Cap.ROUND);
		
		
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		textPaint.setColor(control.chargeTextColor);
		textPaint.setTypeface(control.chargeFont);
		textPaint.setTextAlign(Align.CENTER);
		textPaint.setShadowLayer(5, 0, 1, 0x4C000000);
		
		chargeTextPosition0 = (float)(control.centPoint[1]+control.chargeTextBigSize*0.35);
		chargeTextPosition1 = (float)(control.centPoint[1]+control.chargeTextBigSize*0.92);
	}
	@Override
	public void reset(){
	
	}

	@Override
	public void draw(Canvas canvas) {
		if(!control.isArcChargeShow || !control.userChargeShow) return;
		textPaint.setTextSize(control.chargeTextBigSize);
		canvas.drawText(control.batteryLevel+"%", control.centPoint[0], chargeTextPosition0, textPaint);		// battery level
		
		if(control.batteryLevel<100){
			msg = control.chargeing;
		}else{
			msg = control.batteryFull;
		}
		textPaint.setTextSize(control.chargeTextSmallSize);
		canvas.drawText(msg==null? "":msg, control.centPoint[0], chargeTextPosition1, textPaint);			// charge status
		
		canvas.save();
		if(control.batteryLevel<100) 
			for( int i=0;i<255;i++){
				arcPaint.setAlpha(255-i);
				canvas.drawArc(control.chargeRectF, control.chargeAngle+i, 2, useCenter, arcPaint);
			}
		else{
			arcPaint.setAlpha(255);
			canvas.drawCircle(control.centPoint[0], control.centPoint[1], control.chargeRadius, arcPaint);
		}
		
		canvas.restore();
		
		
	}

}
