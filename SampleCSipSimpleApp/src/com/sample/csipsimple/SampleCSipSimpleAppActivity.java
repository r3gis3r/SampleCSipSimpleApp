package com.sample.csipsimple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;

public class SampleCSipSimpleAppActivity extends Activity implements OnClickListener {
    private static final String THIS_FILE = "SampleCSipSimpleAppActivity";

    private static final String SAMPLE_ALREADY_SETUP = "sample_already_setup";

    private long existingProfileId = SipProfile.INVALID_ID;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        // Retrieve private preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean alreadySetup = prefs.getBoolean(SAMPLE_ALREADY_SETUP, false);
        if(!alreadySetup) {
            // Activate debugging .. here can come various other options
            // One can also decide to reuse csipsimple activities to setup config
            SipConfigManager.setPreferenceStringValue(this, SipConfigManager.LOG_LEVEL, "4");
        }
        
        // Bind view buttons
        ((Button) findViewById(R.id.start_btn)).setOnClickListener(this);
        ((Button) findViewById(R.id.save_acc_btn)).setOnClickListener(this);
        
        // Get current account if any
        Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                SipProfile.FIELD_ID,
                SipProfile.FIELD_ACC_ID,
                SipProfile.FIELD_REG_URI
        }, null, null, SipProfile.FIELD_PRIORITY+ " ASC");
        if(c != null) {
            try {
                if(c.moveToFirst()) {
                    SipProfile foundProfile = new SipProfile(c);
                    existingProfileId = foundProfile.id;
                    ((TextView) findViewById(R.id.field_user)).setText(foundProfile.getSipUserName() + "@" + foundProfile.getSipDomain());
                }
            }catch(Exception e) {
                Log.e(THIS_FILE, "Some problem occured while accessing cursor", e);
            }finally {
                c.close();
            }
            
        }
    }
    
    private String getValidAccountFieldsError() {
        String pwd =  ((EditText) findViewById(R.id.field_password)).getText().toString();
        String fullUser = ((EditText) findViewById(R.id.field_user)).getText().toString();
        String[] splitUser = fullUser.split("@");

        if(TextUtils.isEmpty(fullUser)) {
            return "Empty user";
        }
        if(TextUtils.isEmpty(pwd)) {
            return "Empty password";
        }
        if(splitUser.length != 2) {
            return "Invaid user, should be user@domain";
        }
        return "";
    }

    @Override
    public void onClick(View clickedView) {
        int clickedId = clickedView.getId();
        if(clickedId == R.id.start_btn) {
            
            Intent it = new Intent(SipManager.INTENT_SIP_SERVICE);
            startService(it);
        }else if(clickedId == R.id.save_acc_btn) {
            String pwd =  ((EditText) findViewById(R.id.field_password)).getText().toString();
            String fullUser = ((EditText) findViewById(R.id.field_user)).getText().toString();
            
            String[] splitUser = fullUser.split("@");
            
            String error = getValidAccountFieldsError();
            if(TextUtils.isEmpty(error)) {
                
                // We do some VERY basic thing here (minimal), a real app should probably manage input differently
                SipProfile builtProfile = new SipProfile();
                builtProfile.display_name = "Sample account";
                builtProfile.id = existingProfileId;
                builtProfile.acc_id = "<sip:"+fullUser+">";
                builtProfile.reg_uri = "sip:"+splitUser[1];
                builtProfile.realm = "*";
                builtProfile.username = splitUser[0];
                builtProfile.data = pwd;
                builtProfile.proxies = new String[] {"sip:"+splitUser[1]};
                
                ContentValues builtValues = builtProfile.getDbContentValues();
                
                if(existingProfileId != SipProfile.INVALID_ID) {
                    getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, existingProfileId), builtValues, null, null);
                }else {
                    Uri savedUri = getContentResolver().insert(SipProfile.ACCOUNT_URI, builtValues);
                    if(savedUri != null) {
                        existingProfileId = ContentUris.parseId(savedUri);
                    }
                }
            }else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(error)
                        .setTitle("Invalid settings")
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
            
        }
    }
}