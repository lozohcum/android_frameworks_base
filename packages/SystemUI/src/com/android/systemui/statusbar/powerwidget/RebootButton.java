package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.PowerManager;
import android.os.SystemClock;

public class RebootButton extends PowerButton {
    public RebootButton() { mType = BUTTON_SLEEP; }

    @Override
    protected void updateState(Context context) {
        mIcon = R.drawable.stat_reboot;
        mState = STATE_DISABLED;
        setText(context.getResources().getString(R.string.powerwidget_reboot), TEXT_COLOR_ENABLE);
    }

    @Override
    protected void toggleState(Context context) {
//        Intent reboot = new Intent(Intent.ACTION_REBOOT);
//        reboot.putExtra("nowait", 1);
//        reboot.putExtra("interval", 1);
//        reboot.putExtra("window", 1);
//        reboot.putExtra(Intent.EXTRA_KEY_CONFIRM, true);
//        mView.getContext().sendBroadcast(reboot);

        Intent shutdown = new Intent(Intent.ACTION_REBOOT);
        shutdown.putExtra(Intent.EXTRA_KEY_CONFIRM, true);
        shutdown.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mView.getContext().startActivity(shutdown);

        final StatusBarManager statusBar =
                (StatusBarManager) mView.getContext().getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBar != null) {
          statusBar.collapse();
        }
    }

    @Override
    protected boolean handleLongClick(Context context) {
        return false;
    }
}
