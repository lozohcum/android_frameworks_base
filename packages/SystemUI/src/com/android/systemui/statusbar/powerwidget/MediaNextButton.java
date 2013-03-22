package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.Context;
import android.view.KeyEvent;

public class MediaNextButton extends MediaKeyEventButton {
    public MediaNextButton() { mType = BUTTON_MEDIA_NEXT; }

    @Override
    protected void updateState(Context context) {
        mIcon = R.drawable.stat_media_next;
        mState = STATE_DISABLED;
        setText(context.getResources().getString(R.string.powerwidget_next), TEXT_COLOR_ENABLE);
    }

    @Override
    protected void toggleState(Context context) {
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    @Override
    protected boolean handleLongClick(Context context) {
        return false;
    }
}
