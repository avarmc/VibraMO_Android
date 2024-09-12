package com.psymaker.vibraimage.vibrama;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.psymaker.vibraimage.vibrama.med.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SeqMeasure {
    private VibraimageActivityBase mApp;
    public SeqMeasure(VibraimageActivityBase mApp) {
        this.mApp = mApp;
    }

    public static void onMeasure(VibraimageActivityBase mAppBase) {
        String lt = jni.seqGetLT();
        if(!lt.equals("MOL")) {
            return;
        }

        Thread thread = new Thread(new Runnable() {
            private VibraimageActivityBase mApp;
            // instance initializer
            {
                this.mApp = mAppBase;
            }

            @Override
            public void run() {
                SeqMeasure seq = new SeqMeasure( this.mApp);
                seq.onMeasureCall();
            }
        });

        thread.start();
    }

    private void onMeasureCall() {

        while(!mApp.bPaused) {
            int started = jni.EngineGetIt("VI_INFO_M_STARTED");
            int aborted = jni.EngineGetIt("VI_INFO_M_ABORTED");
            if (aborted != 0)
                break;


            if (started != 0) {
                String answer = getAnswerHTTP(false, true);
                jni.EnginePutStrt("VI_VAR_SEQ_MEASURE_ANSWER", answer);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }

        }
    }

    public void Unregister() {
        String hard = jni.seqGetHard();
        String check = jni.seqGetCheck();
        String key = jni.seqGetKey();


        String lt = jni.seqGetLT();
        String url_base = mApp.getApp().getString(R.string.url_base);
        String info = "cnt";

        String unreg = jni.EngineUnregister().toUpperCase();
        String url = url_base+"?info=" + info + "&lt="+lt+"&h="+hard+"&c="+check+"&key="+key;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDateandTime = sdf.format(new Date());
        url += "&dt="+currentDateandTime;
        url += "&unregister="+unreg;
        jni.resetSeq();
        mApp.putStr("seqA", "");

        Http http = new Http(mMessageHandler);

        http.readFileHTTP(url);
        jni.stopEngine();

        mApp.getApp().finishAffinity();
        System.exit(0);
    }

    public String getAnswerHTTP(boolean bDemo,boolean bMeasure)
    {
        String hard = jni.seqGetHard();
        String check = jni.seqGetCheck();
        String key = jni.seqGetKey();


        String lt = jni.seqGetLT();
        String url_base = mApp.getApp().getString(R.string.url_base);
        String info = "cnt";
        if(bMeasure)
            info = info + "/measure";

        String url = url_base+"?info=" + info + "&lt="+lt+"&h="+hard+"&c="+check;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDateandTime = sdf.format(new Date());

        url += "&dt="+currentDateandTime;

        if(bMeasure) {
            int started = jni.EngineGetIt("VI_INFO_M_STARTED");
            if(started == 0)
                return "";

            String hash = jni.EngineGetStrt("VI_VAR_SEQ_MEASURE_CODE");
            url += "&measure="+hash;
        }

        if(bDemo)
        {
            if( mApp.getInt("demo") < 5 )
            {
                url += "&key=DEMO";
            }
        } else
            url += "&key="+key;

        Http http = new Http(mMessageHandler);

        return http.readFileHTTP(url);
    }

    private final Handler mMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {

            if (mApp != null) {
                Toast.makeText(mApp.getApp(), (String) msg.obj, Toast.LENGTH_LONG).show();
            }
        }
    };
}
