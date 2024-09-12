package com.psymaker.vibraimage.vibrama;

import android.content.Context;
import android.os.Build;

import com.psymaker.vibraimage.jnilib.VIEngine;
import com.psymaker.vibraimage.vibrama.med.R;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by user on 06.07.2017.
 */

public class jni extends VIEngine {
    private VibraimageActivityBase mApp;

    private int mProcessing = 0;

    public  jni(VibraimageActivityBase app)
    {
        mApp = app;
    }

    @Override
    public void Init(Context ctx) {
         super.Init(ctx);

         String regBase = EngineGetStrt("VI_VAR_REG_BASE");


         EnginePutFt("VI_INFO_CHQ_SET_FPS_MIN", 10);
         EnginePutIt("VI_INFO_CHQ_SET_ENABLE", 1);
         EnginePutIt("VI_FACE_ENABLE", 1);
         EnginePutIt("VI_FACE_MODE", 1);
         EnginePutIt("VI_FACE_DRAW", 1);
         EnginePutIt("VI_INFO_AI_LIVE", 0);
         EnginePutIt("VI_VAR_N0_RQST", 50);
         EnginePutIt("VI_VAR_N0", 50);
         EnginePutFt("VI_INFO_M_PERIOD", 60);
         EnginePutFt("VI_VAR_FPSMAXR", 5);
         EnginePutIt("VI_INFO_XLS_OPEN", 1);
    }
	
    @Override
    public  void  export_file(String src_xml_file, String src_html_file, String dst_html_file,int bOverwrite)
    {
        mProcessing++;
        EnginePutIt("VI_FILTER_PAUSE",1);

        ResultWriter html = new ResultWriter(mApp);
        html.export_file(src_xml_file, src_html_file, dst_html_file, bOverwrite);

        mProcessing--;
    }

    public String getStr(String id)
    {
        return mApp.getStr(id);
    }

    public boolean isProcessing() { return mProcessing > 0; }



    public static String seqGetHard()
    {
        return EngineGetStr(1);
    }

    public static String seqGetLicense()
    {
        return EngineGetStr(2);
    }

    public static String seqGetKey()
    {
        return EngineGetStr(6);
    }
    public static String seqGetLT()
    {
        return EngineGetStr(7);
    }

    public static String seqGetCheck()
    {
        return EngineGetStr(5);
    }

    public static void seqPutKey(String v)
    {
        EnginePutStr(6,v);
    }



    public static int seqGetTest()
    {
        if( ! isReady() ) {

            if(getThis() != null)
                getThis().InitStep();
            return 1;
        }

        int n4 = EngineGetI(4);

        String hash = seqGetHard();
        if(hash.length() == 0)
            return 2;

        if (n4 == 0 || (n4 & 0x0f) != 0 || hash.length() < 32)
            return 1;

        if (n4 + 301 != 0x1BD11A00 + 301)
            return 2;

        return 0;
    }




}
