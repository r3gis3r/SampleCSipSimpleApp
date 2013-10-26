
package com.csipsimple.plugins.dtmfbutton;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;

public class PushButtonPopup extends Activity implements OnTouchListener {
    
    private static final String THIS_FILE = "PushButtonPopup";

    private int callId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_button_popup);

        SipCallSession callInfo = getIntent().getParcelableExtra(SipManager.EXTRA_CALL_INFO);
        callId = callInfo.getCallId();
        
        bindService(new Intent(SipManager.INTENT_SIP_SERVICE), connection, Context.BIND_AUTO_CREATE);

        findViewById(android.R.id.button1).setOnTouchListener(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }
    }

    /**
     * Service binding
     */
    private boolean serviceConnected = false;
    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            serviceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceConnected = false;
        }
    };

    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(!serviceConnected) {
            return true;
        }
        int action = event.getAction();
        try {
            if(action == MotionEvent.ACTION_DOWN) {
                service.sendDtmf(callId, KeyEvent.KEYCODE_STAR);
                service.sendDtmf(callId, KeyEvent.KEYCODE_9);
            } else if (action == MotionEvent.ACTION_UP) {
                service.sendDtmf(callId, KeyEvent.KEYCODE_POUND);
                service.sendDtmf(callId, KeyEvent.KEYCODE_9);
            }
        } catch (RemoteException e) {
            Log.e(THIS_FILE, "Cannot ask sip service to send dtmf", e);
        }
        return false;
    }
}
