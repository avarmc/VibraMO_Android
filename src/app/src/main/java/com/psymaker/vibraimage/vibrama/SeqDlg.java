package com.psymaker.vibraimage.vibrama;

import android.view.LayoutInflater;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.StyleRes;

import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import com.psymaker.vibraimage.vibrama.med.R;

public class SeqDlg extends Dialog implements
        android.view.View.OnClickListener {

    private Dialog d;

    private EditText edtKey;
    private EditText edtHardware;
    private EditText edtLicense;
    public int mResult = 0;


    private VibraimageActivityBase mApp;
    private SeqMeasure  mSeq;
    public SeqDlg(VibraimageActivityBase mApp) {
        super(mApp.getApp());
        this.mApp = mApp;
        mSeq = new SeqMeasure(mApp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.seq_dlg);

        if( mApp.getInt("demo") < 3 )
            ((Button) findViewById(R.id.btn_demo)).setOnClickListener(this);
        else
            ((Button) findViewById(R.id.btn_demo)).setVisibility(View.GONE);

        ((Button) findViewById(R.id.btn_check)).setOnClickListener(this);
        ((Button) findViewById(R.id.btn_cancel)).setOnClickListener(this);
        ((Button) findViewById(R.id.btn_http)).setOnClickListener(this);

        edtHardware = (EditText) findViewById(R.id.seq_hardware);
        edtLicense = (EditText) findViewById(R.id.seq_license);
        edtKey = (EditText) findViewById(R.id.seq_key);

        edtHardware.setText( jni.seqGetHard() );
        edtLicense.setText( jni.seqGetLicense());
        edtKey.setText( jni.seqGetKey());

        setCanceledOnTouchOutside(false);
        setCancelable(false);
    }

    private void make_check()
    {
        String license = edtLicense.getText().toString().toUpperCase().trim();
        String key = edtKey.getText().toString().toUpperCase().trim();
        mApp.putStr("seqK", key);

        edtHardware.setText( jni.EngineGetStr(1));

        if((key.length() < 6 && !key.equals("DEMO")) || key.length() > 128 || license.length() < 32 || license.length() > 72)
            return;
        mResult = 1;
        mApp.putStr("seqA", license);

        jni.seqPutKey(key);
        jni.seqPutLicense( license);
        mApp.mProc.EngineCheck();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        dismiss();

        jni.EnginePutStr(9,"");
        mApp.mProc.EngineCheck();
    }

    public boolean isValidKey() {
        String key = edtKey.getText().toString().toUpperCase().trim();
        /*
        if(key.endsWith("_M"))
            return true;
        if(key.endsWith("_MOL"))
            return true;
        if(key.equals("DEMO"))
            return true;
         */
        return key.length() >= 8;
    }

    public String getAnswerHTTP(boolean bDemo,boolean bMeasure) {
        String hard = jni.seqGetHard();
        String key = jni.seqGetKey();

        if(!bDemo && (!hard.equals(edtHardware.getText().toString()) || key.length() < 6) )
            return "";

        String http = mSeq.getAnswerHTTP(bDemo,bMeasure);
        if( http.indexOf("\n") > 0 )
            http = http.substring(0,http.indexOf("\n") );
        return http;
    }

    private void makeTest(boolean bDemo) {
        String key = edtKey.getText().toString().toUpperCase();
        if(!isValidKey())
            return;

        if(key.length() >= 6 || key.equalsIgnoreCase("demo")) {
            mApp.putStr("seqK", key);
            jni.EnginePutStr(6, key);
            String http = getAnswerHTTP(bDemo,false);
            if( http.indexOf("\n") > 0 )
                http = http.substring(0,http.indexOf("\n") );
            edtLicense.setText(http);
            make_check();
        }
    }

    private boolean makeDemo() {
        int demo = mApp.getInt("demo");
        if(demo >= 3)
            return false;
        mApp.putInt("demo",demo+1);
        edtKey.setText("demo");
        makeTest(true);

        int nTest= jni.seqGetTest();

        return nTest == 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_http: {
                if(isValidKey()) {
                    makeTest(false);
                    make_check();
                }
                break;
            }
            case R.id.btn_check: {
                make_check();
                break;
            }
            case R.id.btn_demo: {
                int demo = mApp.getInt("demo");
                if(demo < 3) {
                    makeDemo();
                } else
                    dismiss();
                break;
            }
            case R.id.btn_cancel:
                mResult = -1;
                dismiss();
                break;
            default:
                break;
        }
    }


    @Override
    public void onStop ()
    {
        if(mResult == 0)
            mResult = -1;
    }



}
