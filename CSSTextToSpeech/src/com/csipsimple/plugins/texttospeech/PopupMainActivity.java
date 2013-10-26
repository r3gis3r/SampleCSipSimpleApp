
package com.csipsimple.plugins.texttospeech;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PopupMainActivity extends Activity implements OnClickListener, OnInitListener, OnUtteranceCompletedListener {

    protected static final String THIS_FILE = "PopupMainActivity";


    private SipCallSession initialSession;

    private TextToSpeech tts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_main);

//        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//        lp.copyFrom(getWindow().getAttributes());
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
//        getWindow().setAttributes(lp);
        
        initialSession = getIntent().getParcelableExtra(SipManager.EXTRA_CALL_INFO);

        cacheDir = getExternalCacheDir();
        if(cacheDir != null) {
            Log.d(THIS_FILE, "Cache dir is : " + cacheDir.toString());
        }
        tts = new TextToSpeech(this, this);
        tts.setOnUtteranceCompletedListener(this);

        bindService(new Intent(SipManager.INTENT_SIP_SERVICE), connection, Context.BIND_AUTO_CREATE);

        findViewById(android.R.id.button1).setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }

        tts.shutdown();
        
        clearCache();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.popup_main, menu);
        return true;
    }

    private SipCallSession[] callsInfo;

    /**
     * Service binding
     */
    private boolean serviceConnected = false;
    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            try {
                // Log.d(THIS_FILE,
                // "Service started get real call info "+callInfo.getCallId());
                callsInfo = service.getCalls();
                serviceConnected = true;
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Can't get back the call", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceConnected = false;
            callsInfo = null;
        }
    };

    private File cacheDir;

    private boolean ttsInitDone = false;

    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean canTTS() {
        String toSayStr = ((EditText) findViewById(android.R.id.text1)).getText().toString();
        if (TextUtils.isEmpty(toSayStr)) {
            Log.d(THIS_FILE, "Nothing to say");
            return false;
        }
        if (!ttsInitDone) {
            Log.d(THIS_FILE, "TTS Engine not ready");
            return false;
        }
        if (!serviceConnected || service == null) {
            Log.e(THIS_FILE, "SIP Service not available");
            return false;
        }
        if(cacheDir == null) {
            Log.e(THIS_FILE, "No cache dir to generate temp files");
            return false;
        }
        return true;
    }
    
    /**
     * Ensures at least one language is available for tts
     */
    private boolean checkAndSetLanguageAvailable() {
        // checks if at least one language is available in Tts
        final Locale defaultLocale = Locale.getDefault();
        // If the language for the default locale is available, then
        // use that.
        int defaultAvailability = tts.isLanguageAvailable(defaultLocale);

        if (defaultAvailability == TextToSpeech.LANG_AVAILABLE ||
                defaultAvailability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                defaultAvailability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            tts.setLanguage(defaultLocale);
            Log.d(THIS_FILE, "Set locale " + defaultLocale.toString());
            return true;
        }

        for (Locale locale : Locale.getAvailableLocales()) {
            int availability = tts.isLanguageAvailable(locale);
            if (availability == TextToSpeech.LANG_AVAILABLE ||
                    availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                Log.d(THIS_FILE, "Set locale " + locale.toString());
                tts.setLanguage(locale);
                return true;
            }
        }
        return false;
    }

    private HashMap<String, String> createParams(String utterance) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utterance);
        return params;
    }
    

    @Override
    public void onClick(View v) {
        if (v.getId() == android.R.id.button1) {

            String toSayStr = ((EditText) findViewById(android.R.id.text1)).getText().toString();
            if (!canTTS()) {
                Log.e(THIS_FILE, "Cannot say it :(");
                return;
            }
            String md5name = md5(toSayStr);
            File targetFile = new File(cacheDir,  md5name + ".wav");
            if (!targetFile.exists()) {
                int res = tts.synthesizeToFile(toSayStr, createParams(md5name), targetFile.getAbsolutePath());
                if(res == TextToSpeech.ERROR) {
                    Log.e(THIS_FILE, "Error while generating file synthesis");
                }
            }
            if(generatedUtterances.contains(md5name)) {
                actualPlay(md5name);
            }
        }
    }

    @Override
    public void onInit(int status) {
        Log.d(THIS_FILE, "TTS init done");
        if (status == TextToSpeech.SUCCESS) {
            checkAndSetLanguageAvailable();
            ttsInitDone = true;
        } else {
            // TODO : inform user about problems
        }
    }

    private void clearCache() {
        try {
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDir(cacheDir);
            }
        } catch (Exception e) {
            Log.e(THIS_FILE, "Error while cleaning cache dir", e);
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    private List<String> generatedUtterances = new ArrayList<String>();
    
    @Override
    public void onUtteranceCompleted(String utteranceId) {
        generatedUtterances.add(utteranceId);
        actualPlay(utteranceId);
    }
    

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void actualPlay(String utteranceId) {
        File targetFile = new File(cacheDir,  utteranceId + ".wav");
        if (!canTTS()) {
            Log.e(THIS_FILE, "Cannot say it :(");
            return;
        }

        int callId = SipCallSession.INVALID_CALL_ID;
        if (initialSession != null) {
            Log.d(THIS_FILE, "Use initial session as available");
            callId = initialSession.getCallId();
        } else {
            for (SipCallSession call : callsInfo) {
                if (call.isActive()) {
                    callId = call.getCallId();
                    break;
                }
            }
        }
        if (callId == SipCallSession.INVALID_CALL_ID) {
            Toast.makeText(PopupMainActivity.this, "No call available to play sound",
                    Toast.LENGTH_SHORT).show();
            return;
        }


        if (targetFile != null && targetFile.exists() && targetFile.canRead()) {
            // Ensure readable from pj process/app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                targetFile.setReadable(true, false);
            } else {
                try {
                    Runtime.getRuntime().exec("chmod 644 " + targetFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(THIS_FILE, "cannot change file permission", e);
                }
            }
            try {
                service.playWaveFile(targetFile.getAbsolutePath(), callId, 1);
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "cannot call remote procedure", e);
            }
        }else {
            Log.e(THIS_FILE, "Was not possible to generate finally");
        }
    }

}
