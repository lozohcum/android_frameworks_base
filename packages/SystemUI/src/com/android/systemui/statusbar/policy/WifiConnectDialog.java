
package com.android.systemui.statusbar.policy;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar.WifiAuthentication;

import java.util.List;

public class WifiConnectDialog extends Activity implements OnClickListener, OnCheckedChangeListener {
    public String SSID = "";
    private TextView mTitle;
    private TextView mCrypot;
    private EditText mPassword;
    private Button mConnectButton;
    private Button mCancelButton;
    private CheckBox mShowPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_connect_dialog);
        SSID = this.getIntent().getStringExtra("SSID");

        mTitle = (TextView) findViewById(R.id.ssid);
        mConnectButton = (Button) findViewById(R.id.connect);
        mCancelButton = (Button) findViewById(R.id.cancel);
        mPassword = (EditText) findViewById(R.id.pwd);
        mShowPassword = (CheckBox) findViewById(R.id.checkBoxShowPassword);

        mTitle.setText(SSID);

        mConnectButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mShowPassword.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                showIME();
            }
        }, 500);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        hideIME();
        super.onPause();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        hideIME();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.connect:
                WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                manager.startScan();
                List<ScanResult> result = manager.getScanResults();
                WifiConfiguration config = null;
                for (int i = 0; i < result.size(); i++) {
                    if (result.get(i).SSID.equals(SSID)) {
                        config = WifiAuthentication.CreateWifiInfo(SSID,
                                (mPassword.getText().toString() == null) ? "" : mPassword.getText()
                                        .toString(),
                                WifiAuthentication.getSecurityType(result.get(i)));
                    }
                }
                if (config != null) {
                    manager.enableNetwork(manager.addNetwork(config), true);
                }
                manager.saveConfiguration();
            case R.id.cancel:
                hideIME();
                finish();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        mPassword.setInputType(isChecked ? InputType.TYPE_CLASS_TEXT
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        mPassword.setSelection(mPassword.getText().length());
    }

    private void hideIME() {
        mPassword.clearFocus();
        InputMethodManager inputManager = (InputMethodManager)
                mPassword.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(mPassword.getWindowToken(),
                0);
    }

    private void showIME() {
        InputMethodManager inputManager = (InputMethodManager)
                mPassword.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInputFromWindow(mPassword.getWindowToken(), InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}
