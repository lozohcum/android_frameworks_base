package android.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import android.widget.RemoteViews.RemoteView;

/**
 * @date 2013-2-1
 * @author huhailong huhailong@shendu.com
 * @project CM10_frameworks
 * TODO for DeskClock simple widget
 */
@RemoteView
public class ShenduSimpleClockAmPmTextView extends TextView{

	public ShenduSimpleClockAmPmTextView(Context context) {
		super(context);
	}
	
	public ShenduSimpleClockAmPmTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ShenduSimpleClockAmPmTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Typeface hourTypeFace;
		try{
			hourTypeFace = Typeface.createFromFile("/system/fonts/Roboto-Light.ttf");
		}catch(Exception e){
			e.printStackTrace();
			hourTypeFace = Typeface.DEFAULT_BOLD;
		}
		setTypeface(hourTypeFace);
	}

}
