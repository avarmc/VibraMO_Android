package com.psymaker.vibraimage.vibrama;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.psymaker.vibraimage.vibrama.ui.FragmentVI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import com.psymaker.vibraimage.vibrama.med.R;

/**
 * Created by user on 22.05.2017.
 */

public class ImageProc {

    private static final String TAG = "VibraimageTAG";

    VibraimageActivityBase mApp;

    static class theLock extends Object {}
    static public theLock lockObject = new theLock();
    static public theLock checkLock = new theLock();

    public int mSkip       = 0;

    private int nFrame      = 0;
    private float tVideo = 0;

    private Map<String,Integer> mTags = new TreeMap<String,Integer>();

    protected int deviceRotation = Surface.ROTATION_0;

    private byte[] tempYbuffer = null;

    public static void L(String str)
    {
        Log.i(TAG, str);
    }

    public ImageProc(VibraimageActivityBase mApp)
    {
        this.mApp = mApp;
    }

    public void InitDB() {
        String dataDir = null;
        String dbDir = null;

        try {
            dataDir = mApp.getAppDataDir();
            dbDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
            if(dbDir != null) {
                dbDir += "/VibraMA";

                File outDirectory = new File(dbDir + "/");
                if (!outDirectory.exists())
                    outDirectory.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(dataDir != null && dbDir != null)
            jni.SetDataDir( dataDir, dbDir);
    }

    public void Init()
    {
        if(jni.isStarted() != 0)
            jni.stopEngine();

        synchronized (lockObject) {
            int nP =  Runtime.getRuntime().availableProcessors();
            switch(nP)
            {
                case 1:
                case 2:
                case 3:
                    jni.startEngine(  nP);
                    break;
                case 4:
                case 5:
                    jni.startEngine(  nP-1);
                    break;
                default:
                    jni.startEngine(  nP-2 );
                    break;
            }

            InitLang();
            InitTemplate();



            mApp.mJni.Init(mApp.getApp());

            jni.EnginePutIt("VI_FILTER_FPS2INF",0);
            jni.EnginePutIt("VI_FILTER_FPS2INR",0);

            jni.EnginePutIt("VI_FILTER_REALTIME",1);

            jni.EnginePutIt("VI_FILTER_FPS2INBF", 0);
            jni.EnginePutIt("VI_FILTER_FPS2INBR", 0);

            jni.EnginePutIt("VI_FILTER_FPS2INAF",10);
            jni.EnginePutIt("VI_FILTER_FPS2INAR",2);
            EngineCheck();
        }
    }

    private void InitTemplate() {
        AssetManager assetManager = mApp.getApp().getAssets();
        Locale current = mApp.getApp().getResources().getConfiguration().locale;
        String lang = current.getLanguage();
        String filename = null, filename0 = null;
        String localPath = mApp.getApp().getFilesDir().getAbsolutePath();

        try {
            String[] filelist = assetManager.list("");
            for(String name:filelist) {
                if( name.endsWith("M_"+lang+".html") )
                    filename = name;
                else
                if( name.endsWith("M.html"))
                    filename0 = name;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(filename == null)
            filename = filename0;

        if(filename != null)
            jni.EnginePutStrt("VI_INFO_PATH_M_TEMPLATE_XLS",localPath+"/"+filename);
        else
            jni.EnginePutStrt("VI_INFO_PATH_M_TEMPLATE_XLS","");
    }

    private void Align() {
        if(mApp.mVi != null) {
            mApp.runOnUiThread(new Runnable() {
                public void run() {
                    mApp.mVi.configureTransform();
                }
            });
        }
    }
    private void InitLang()
    {
        Field[] fields = R.string.class.getDeclaredFields(); // or Field[] fields = R.string.class.getFields();

        for (int  i =0; i < fields.length; i++) {
            String tag = fields[i].getName();
            if(tag.contains("$"))
                continue;

            int resId = mApp.getResources().getIdentifier(tag, "string", mApp.getPackageName());
            if (resId != 0) {
                String v = mApp.getResources().getString(resId);
                jni.PutLang(tag,v);
            }
        }
    }
    public void Stop()
    {
        synchronized (lockObject) {
            jni.stopEngine();
        }
    }

    public void onTime(float t) {
        if( t >= 0 ) {
            jni.EnginePutIt("VI_INFO_TIME_VIDEO_EXT",1);
            jni.EnginePutFt("VI_INFO_TIME_VIDEO",t);
        } else {
            jni.EnginePutIt("VI_INFO_TIME_VIDEO_EXT",0);
        }
    }

    public void onImage(Bitmap bmp)
    {
        if(mApp.mVi == null || mApp.mVi.mVideo == null ||mSkip > 0 || mApp.mJni.isProcessing() || !mApp.mJni.isReady()  /* || mApp.mHttp.nRead <= 0  */)
            return;

        if (!mApp.mVi.mVideo.checkState())
            return;


        int n4= jni.EngineGetI(4);
        //  L(String.format("N4: %08X", n4 ));
        if( n4+305 != 0x1BD11A00+305 )
            return;

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        mApp.mJni.EnginePutIt("VI_INFO_AI_LIVE_T",5);

        jni.EnginePutIt("VI_FACE_CROP_Y",0);


        float t = mApp.mVi.mVideo.getTime();
        jni.EnginePutFt("VI_VAR_VIDEO_TIMER",t);
        if(t < tVideo)
            jni.EnginePutIt("VI_VAR_RESET_TIMER",1);
        tVideo = t;
        synchronized (lockObject) {
            /********** Critical Section *******************/

            if(jni.isStarted() != 1) {
                L("Engine is not started");
                return;
            }


            boolean skip = false;


            if(!skip) {

                int strideR = w*4;
                int size = strideR*h;

                float fps = jni.EngineAddImage32(pixels, size, w, h,strideR, deviceRotation);

                onPeekResult(w, h, false);

            }

            /********** Critical Section *******************/
        }

        if(nFrame == 0)
            Align();

        nFrame++;
        //     L("onImage "+w+"x"+h);
    }

    public void onImage(Image image)
    {
        if(mApp.mVi == null || mApp.mVi.mVideo == null ||mSkip > 0 || mApp.mJni.isProcessing() || !mApp.mJni.isReady()  /* || mApp.mHttp.nRead <= 0 */ )
            return;

        if (!mApp.mVi.mVideo.checkState())
            return;

        int n4= jni.EngineGetI(4);
        if( n4+305 != 0x1BD11A00+305 )
            return;
//        jni.EnginePutFt("VI_INFO_M_PERIOD", 10);
//        jni.EnginePutFt("VI_INFO_M_DELAY", 1);

        int w = image.getWidth();
        int h = image.getHeight();

        float t = mApp.mVi.mVideo.getTime();
        jni.EnginePutFt("VI_VAR_VIDEO_TIMER",t);
        if(t < tVideo)
            jni.EnginePutIt("VI_VAR_RESET_TIMER",1);
        tVideo = t;

        synchronized (lockObject) {
            /********** Critical Section *******************/

            if(jni.isStarted() != 1) {
                L("Engine is not started");
                return;
            }



            boolean skip = false;


            if(!skip) {
                Image.Plane[] planes = image.getPlanes();
                Image.Plane Y = planes[0];


                int Yb = Y.getBuffer().remaining();

                int strideR = Y.getRowStride();

                int size = Yb;
                if(tempYbuffer == null || tempYbuffer.length != size)
                    tempYbuffer = new byte[size];
                Y.getBuffer().get(tempYbuffer, 0, Yb);


                float fps = jni.EngineAddImage(tempYbuffer, size, w, h,strideR, deviceRotation);

                onPeekResult(w, h, false);

                int aa = jni.EngineGetIt("VI_FILTER_REALTIME");
                int mFrameProcI = jni.EngineGetIt("VI_VAR_NFRAME_IN");
                int mFrameProcF = jni.EngineGetIt("VI_VAR_NFRAME_OUTF");
                int mFrameProcR = jni.EngineGetIt("VI_VAR_NFRAME_OUTF");

                Log.d(TAG,"ProcessImage i="+mFrameProcI+" f="+mFrameProcF+" r="+mFrameProcR);
            }

            /********** Critical Section *******************/
        }


        if(nFrame == 0)
            Align();

        nFrame++;
        float fpsI = jni.EngineGetFt("VI_VAR_FPSIN");
        L("onImage "+nFrame + " " +fpsI );
    }

    protected void onPeekResult(int w,int h, boolean bResult) {

        FragmentVI wndVI = mApp.getWndVI();
        if (wndVI == null || wndVI.mDraw == null)
            return;

        if(jni.EngineGetIt("VI_VAR_NFRAME") < 3)
            return;
        int nw,nh;

        if(deviceRotation == 0 || deviceRotation == Surface.ROTATION_180)
        {
            nh = (w+31)&(~31);
            nw = (h+31)&(~31);
        } else
        {
            nw = (w+31)&(~31);
            nh = (h+31)&(~31);
        }

        synchronized (wndVI.mDraw.mBitmapLock) {

            if (wndVI.mDraw.mBitmap == null || wndVI.mDraw.mBitmap.getWidth() != nw || wndVI.mDraw.mBitmap.getHeight() != nh)
                wndVI.mDraw.mBitmap = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888);


            int n4= jni.EngineGetI(4);
            if( n4+302 != 0x1BD11A00+302 )
                return;
            jni.EngineDrawResult(jni.EngineGetIt("VI_MODE_RESULT"), jni.EngineGetIt("VI_MODE_AURA"), wndVI.mDraw.mBitmap);
/*
            for(int y = 0; y < nh; ++y) {
                int x = y*nw/nh;
                wndVI.mDraw.mBitmap.setPixel(x, y, 0xFF0000FF);
                wndVI.mDraw.mBitmap.setPixel(nw-1-x, y, 0xFF0000FF);
            }
*/

        }
        wndVI.mDraw.postInvalidate();
    }

    public void setMode(int vResult,int vAura)
    {
        jni.EnginePutIt("VI_MODE_RESULT",vResult);
        jni.EnginePutIt("VI_MODE_AURA",vAura);
    }


    public void setModePreset(int v)
    {
        int modeB = jni.EngineGetIt("VI_MODE_B");

        switch(v)
        {
            case Const.VI_PRESET_VI:
                if(modeB != 0)
                    setMode( Const.VI_RESULT_VI0_B, 0 );
                else
                    setMode( Const.VI_RESULT_VI0_A, 0 );
                break;

            case Const.VI_PRESET_AV:
                if(modeB != 0)
                    setMode( Const.VI_RESULT_VI0_B, Const.VI_RESULT_VI0_B );
                else
                    setMode( Const.VI_RESULT_VI0_A, Const.VI_RESULT_VI0_A );
                break;

            case Const.VI_PRESET_AR:
                if(modeB != 0)
                    setMode( Const.VI_RESULT_SRC_0, Const.VI_RESULT_VI0_B );
                else
                    setMode( Const.VI_RESULT_SRC_0, Const.VI_RESULT_VI0_A );
                break;

            case Const.VI_PRESET_LD:
                if(modeB != 0)
                    setMode( Const.VI_RESULT_SRC_0, 0 );
                else
                    setMode( Const.VI_RESULT_SRC_0, 0 );
                break;

            default:
                break;
        }
    }

    public int getModePerset()
    {
        int modeB = jni.EngineGetIt("VI_MODE_B");
        int mModeResult = jni.EngineGetIt("VI_MODE_RESULT");
        int mModeAura = jni.EngineGetIt("VI_MODE_AURA");

        if(modeB != 0)
        {
            if( mModeResult == Const.VI_RESULT_VI0_B && mModeAura == 0)
                return Const.VI_PRESET_VI;
            if( mModeResult == Const.VI_RESULT_VI0_B && mModeAura == Const.VI_RESULT_VI0_B)
                return Const.VI_PRESET_AV;
            if( mModeResult == Const.VI_RESULT_SRC_0 && mModeAura == Const.VI_RESULT_VI0_B)
                return Const.VI_PRESET_AR;
            if( mModeResult == Const.VI_RESULT_SRC_0 && mModeAura == 0)
                return Const.VI_PRESET_LD;
        } else
        {
            if( mModeResult == Const.VI_RESULT_VI0_A && mModeAura == 0)
                return Const.VI_PRESET_VI;
            if( mModeResult == Const.VI_RESULT_VI0_A && mModeAura == Const.VI_RESULT_VI0_A)
                return Const.VI_PRESET_AV;
            if( mModeResult == Const.VI_RESULT_SRC_0 && mModeAura == Const.VI_RESULT_VI0_A)
                return Const.VI_PRESET_AR;
            if( mModeResult == Const.VI_RESULT_SRC_0 && mModeAura == 0)
                return Const.VI_PRESET_LD;
        }
        return 0;
    }

    public void onFaceNotDetected()
    {
        jni.EngineSetFace(0,0,0,0);
    }

    public void onFaceDetected(Rect r, Rect rMax)
    {
        Size s = mApp.mVi.mVideo.mPreviewSize;

        int nFrame = jni.EngineGetIt("VI_VAR_NFRAME" );

        if( jni.isStarted() != 1 || s.getWidth() == 0 || s.getHeight() == 0 || nFrame < 3) {
            return;
        }

        int mw = rMax.width();
        int mh = rMax.height();
        int x = (r.centerX()-rMax.left)*s.getWidth()/mw;
        int y = (r.centerY()-rMax.top)*s.getHeight()/mh;
        int w = (r.width()*s.getWidth()/mw)*3/2;
        int h = (r.height()*s.getHeight()/mh)*3/2;

        if( deviceRotation == Surface.ROTATION_90)
        {
            jni.EngineSetFace(x,y,w,h);

        } else
        if( deviceRotation == Surface.ROTATION_270)
        {
            jni.EngineSetFace(x,y,w,h);
        } else
        if( deviceRotation == Surface.ROTATION_180)
        {
            jni.EngineSetFace(y,x,h,w);
        } else
            jni.EngineSetFace(y,x,h,w);

    }

    public void AddTag(String tag)
    {
        mTags.put(tag,1);
    }


    public void EngineCheck()
    {
        synchronized (checkLock) {
            String cmd = jni.EngineCheck();
            mApp.StateCheckFn(cmd);
        }
    }

    private Thread checkThread = null;
    private Runnable checkProc =new Runnable() {
        public void run() {
            while(!mApp.bPaused)
            {
                EngineCheck();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            checkThread = null;
        }
    };


    public void onResume()
    {
        L("onResume");


        Reg reg = new Reg(mApp);
        reg.load();

        checkThread = new Thread(checkProc);
        checkThread.start();

    }

    public void onPause() {
        L("onPause");



        Reg reg = new Reg(mApp);
        reg.save();

        while(checkThread != null)
        {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onOrientationChanged( int o )
    {
        if(o != deviceRotation) {
            deviceRotation = o;
            return true;
        }
        return false;
    }


}
