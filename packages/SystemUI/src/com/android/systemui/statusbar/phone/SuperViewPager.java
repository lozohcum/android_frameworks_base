package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class SuperViewPager extends ViewPager {

    public PhoneStatusBar mService;

    private static boolean enable;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        //int mScrollViewHeight = ((ViewGroup)((ViewGroup)this.getChildAt(0)).getChildAt(0)).getChildAt(0).getHeight();
        int mScrollViewHeight = mService.mScrollView.getHeight();
        int mNotiHeight = mService.mScrollView.getChildAt(0).getHeight();
        if (mNotiHeight > mScrollViewHeight) {
            mNotiHeight = mScrollViewHeight;
        }
        switch (arg0.getAction()) {
        case MotionEvent.ACTION_DOWN:
            // TODO Auto-generated method stub
            mService.closeWifiMenu();
            if (SuperViewPager.enable) {
                float y = arg0.getY();
                if (y > mNotiHeight || this.getCurrentItem() > 0) {
                    return super.onInterceptTouchEvent(arg0);
                } else {
                    return false;
                                    }
            } else {
            return false;
            }
        case MotionEvent.ACTION_MOVE:
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
        }
//        // TODO Auto-generated method stub
        return super.onInterceptTouchEvent(arg0);
    }

//    @Override
//    protected void onConfigurationChanged(Configuration newConfig) {
//        // TODO Auto-generated method stub
//        super.onConfigurationChanged(newConfig);
//        mService.updateExpandedViewPos(PhoneStatusBar.EXPANDED_FULL_OPEN);
//    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        return super.onTouchEvent(arg0);
    }

    public SuperViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        SuperViewPager.enable = true;
        // TODO Auto-generated constructor stub
    }

    public static void setPagingEnable(boolean enable) {
//        Log.i("dongcidaci", "PagingEnable = " + enable);
        SuperViewPager.enable = enable;
    }
    
    public static boolean getPagingEnable() {
        return enable;
    }
}
