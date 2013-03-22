package com.android.systemui.statusbar.phone;

import android.app.StatusBarManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class SuperScrollView extends ScrollView {

	private static Context mContext;
	private PullUpViewOnTouchListener mListener;

	public SuperScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mListener = new PullUpViewOnTouchListener();
		// TODO Auto-generated constructor stub
	}

	public SuperScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
   mListener = new PullUpViewOnTouchListener();
		// TODO Auto-generated constructor stub
	}

	public SuperScrollView(Context context) {
		super(context);
		mContext = context;
   mListener = new PullUpViewOnTouchListener();
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		if (this.getChildAt(0).getHeight() > this.getHeight()) {  //need to scroll
			return super.onTouchEvent(ev);
		} else {
			mListener.onTouch(this, ev);
			return true;
		}
	}

	public static class PullUpViewOnTouchListener implements OnTouchListener{
		static float beginX, beginY, endX, endY;
		static boolean pressing = false;

		public boolean onTouch(View view, MotionEvent event) {
			// TODO Auto-generated method stub
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!pressing) {
					pressing = true;
					beginX = event.getX();
					beginY = event.getY();
                }
				break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if (pressing) {
						pressing = false;
						endX = event.getX();
						endY = event.getY();
						if (beginY - endY > 80 && (Math.abs((beginY - endY) / (beginX - endX)) >= 2) ) {
							final StatusBarManager statusBar = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
							if (statusBar != null) {
								statusBar.collapse();
							}
                    }
                }
            }
			return true;
        }
    }
}
