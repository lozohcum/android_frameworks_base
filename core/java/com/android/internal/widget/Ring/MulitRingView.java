package com.android.internal.widget.Ring;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.widget.*;
import android.content.res.TypedArray;
import com.android.internal.R;

public class MulitRingView extends View {
	private Control control;

	public MulitRingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public MulitRingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public MulitRingView(Context context) {
		this(context,null);
	}
	
	private void init(Context context, AttributeSet attrs){
		control = Control.getInstance();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MulitRingView);
		control.init(this,a);
		a.recycle();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(!control.isRunning){
			control.exec.execute(control.dotRunnable);
		}
		canvas.clipRect(0, control.centPoint[1]-control.bgRadius-control.imageRadius-5, this.getWidth(), this.getHeight());
		
		for(ViewInterface vi:control.viewList){
			vi.draw(canvas);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int minimumWidth = (control.bgRadius + control.imageRadius)<<1;
		int minimumHeight = minimumWidth;
		int computedWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int computedHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        
		control.setViewSize(computedWidth, computedHeight);
		setMeasuredDimension(computedWidth, computedHeight);
	}
	
	private int resolveMeasured(int measureSpec, int desired){
        int result = 0;
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                result = desired;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(specSize, desired);
                break;
            case MeasureSpec.EXACTLY:
            default:
                result = specSize;
        }
        return result;
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return control.onTouchEvent(event);
	}
	
/**********************************************************************************************************************/
	
	
	public void reset(boolean animate){
		// 重设
		control.reset(animate);
	}
	
	public void cleanUp(){
		// 资源释放
		control.clean();
	}
	//public void updateResources(){}
	
	//public void ping(){}
	
	//public void setEnableTarget(int resourceId, boolean enabled){}
	
	//public int getTargetPosition(int resourceId){ return -1;}
	
	
	/**
     * Interface definition for a callback to be invoked when the dial
     * is "triggered" by rotating it one way or the other.
     */
    public interface OnTriggerListener {
        int NO_HANDLE = 0;
        int CENTER_HANDLE = 1;
        
        int UNLOCK	= 101;
        int SOUND	= 102;
        int CAMERA	= 103;
        int SMS		= 104;
        int CALL	= 105;
        int APP		= 106;
        
        public void onTrigger(int func,String extern);
        public void onGrabbedStateChange(int handler);
    }
    
    public void setOnTriggerListener(OnTriggerListener l) {
        control.setOnTriggerListener(l);
    }
    
	public boolean onExtStatusChange(int type,boolean show,boolean extInfo,int level){
		return control.onExtStatusChange(type,show,extInfo,level);
	}
	
}
