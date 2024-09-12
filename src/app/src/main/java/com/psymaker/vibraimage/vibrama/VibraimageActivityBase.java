package com.psymaker.vibraimage.vibrama;



import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.psymaker.vibraimage.vibrama.ui.FragmentCfgCamera;
import com.psymaker.vibraimage.vibrama.ui.FragmentVI;

import com.psymaker.vibraimage.vibrama.ui.ViProgress;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.psymaker.vibraimage.vibrama.med.R;

/**
 * Activity displaying a fragment that implements RAW photo captures.
 */
public class VibraimageActivityBase
{
    private static final String TAG = "ViApp";
    public  FragmentVI mVi;
    public Fragment  mCurrentFragment;

    public ImageProc  mProc;
    public ViProgress mProgress;

    public jni        mJni;

    private VibraimageActivity mApp;
    private ViMenu mMenu;


    public boolean bPaused = true;

    public Map<String, Integer> permissions  = new HashMap<String, Integer>();

    private ViSpeechRecognizer mVoice;


    public VibraimageActivityBase(VibraimageActivity mApp)
    {
        this.mApp = mApp;
        mJni = new jni(this);

    }
    public void Sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {

        mMenu = new ViMenu(this);

        if(mProc == null) {
            mProc = new ImageProc(this);
            mProc.Init();
        }

        mApp.setContentView(R.layout.activity_vi);

        mVi = (FragmentVI)mApp.getSupportFragmentManager().findFragmentById(R.id.vi_fragment);

        mProgress = (ViProgress) mApp.getSupportFragmentManager().findFragmentById(R.id.vi_progress);
        hideAll();
        showFragment(R.id.vi_fragment);

        if( seqHandler == null ) {
            seqHandler = new Handler();
            seqHandler.postDelayed(seqRun, 40);
        }

        mVoice = new ViSpeechRecognizer( this );
        mVoice.listen();

//        VIEngineQuestion q = new VIEngineQuestion();
//        q.nq = 10;
//        jni.EnginePutStrt("VI_VAR_MI_QUESTION",( q == null ) ? "" : q.getXml());
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        mApp.getMenuInflater().inflate(R.menu.menu, menu);
        mMenu.onPrepareOptionsMenu(menu);
        return true;
    }


    public int onOptionsItemSelected(MenuItem item) {
        return mMenu.onOptionsItemSelected(item);
    }


    public boolean onBackPressed() {
        if(isFragmentVI() ) {
            if(jni.EngineGetIt("VI_FILTER_PAUSE") != 0)
            {
                jni.EnginePutIt("VI_FILTER_PAUSE",0);
                jni.EnginePutIt("VI_VAR_RESET",1);
                return true;
            }

            return false;
        }


        showFragment(R.id.vi_fragment);
        return true;
    }

    public boolean onPrepareOptionsMenu(final Menu menu) {
        mMenu.onPrepareOptionsMenu(menu);
        return false;
    }


    public boolean isFragmentVI()
    {
        if(mCurrentFragment != null && mCurrentFragment instanceof FragmentVI)
            return true;
        return false;
    }




    protected void hideAll()
    {
        FragmentManager fm = mApp.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fr;

        fr = (Fragment)fm.findFragmentById(R.id.vi_fragment);
        if(fr != null) { ft.hide(fr); }

        fr = (Fragment)fm.findFragmentById(R.id.vi_fragment_v4);
        if(fr != null) { ft.hide(fr); }

        fr = (Fragment)fm.findFragmentById(R.id.vi_fragment_cfg);
        if(fr != null) { ft.hide(fr);  }


       fr = (Fragment)fm.findFragmentById(R.id.vi_fragment_cfg_camera);
        if(fr != null) { ft.hide(fr);  }

        fr = (Fragment)fm.findFragmentById(R.id.vi_fragment_web);
        if(fr != null) { ft.hide(fr);  }

        ft.commit();
    }


    public boolean showFragment(int id)
    {
        FragmentManager fm = mApp.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);

        if(mCurrentFragment != null)
        {
            if(mCurrentFragment.getId() != id) {
                ft.hide(mCurrentFragment);
            }
        }

        mCurrentFragment = (Fragment)fm.findFragmentById(id);
        if(mCurrentFragment != null) {
            ft.show(mCurrentFragment);
        }
        ft.commit();

        if(mVoice != null)
            mVoice.listen();

        return true;
    }

    private int    seqCount = 0;
    private SeqDlg seqDlg = null;
    private Handler seqHandler = null;


    private Runnable seqRun = new Runnable() {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if(!showSeq()) {
                        return;
                    }

                    seqHandler.postDelayed(this, 400);
                }
            });

        }
    };



    protected boolean showSeq()
    {
        if(bPaused)
            return true;

        int nTest= jni.seqGetTest();
        if(nTest == 0) {
            mVi.cameraRestart();
            return false;
        }
        if( nTest == 1 )
            return true;

        if(seqDlg != null) {
            if (seqDlg.mResult == 0)
                return true;
            if (seqDlg.mResult == -1 || ++seqCount >= 10) {
                mApp.finish();
                return false;
            }
            nTest= jni.seqGetTest();
            seqDlg.dismiss();
            seqDlg = null;


        }

        if( nTest == 2 ) {
            seqDlg = new SeqDlg( this );
            seqDlg.show();
            return true;
        }

        mApp.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return false;
    }

    public String getAppDataDir()  {
        Context context = mApp.getApplicationContext();
        String path = "";
        try {
            path = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return path;
    }

    public ImageProc getImageProc()
    {
        return mProc;
    }

    public FragmentVI getWndVI()
    {
        return mVi;
    }


    public void StateCheckFn(String cmd)
    {
        if(bPaused)
            return;

        if( mProgress != null )
            mProgress.check();

        if(cmd != null && !cmd.isEmpty()) {
            List<String> larr = Arrays.asList(cmd.split("\n"));
            if(larr.size() > 0) {
                String arr[] = new String[larr.size()];
                larr.toArray(arr);

                if (arr.length == 4 && arr[0].equals("xml2xls_onZipReady")) {
                    jni.onZipResultFn(arr[1],arr[2],arr[3],0);
                } else
                if (arr.length == 4 && arr[0].equals("xml2xls_export_file")) {
                    jni.xml2html_export_file(arr[1],arr[2],arr[3],1);
                }
            }
        }
    }


    public void onResume()
    {
        bPaused = false;
        jni.SkipSet(0);
        jni.EnginePutIt("VI_FILTER_PAUSE",0);
        jni.EnginePutIt("VI_VAR_RESET",1);
        mApp.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(mVoice != null)
            mVoice.listen();

        jni.EnginePutStrt("VI_VAR_SEQ_CODE","username");
        jni.EnginePutStrt("VI_VAR_SEQ_PASSWORD","password12345");
        mVi.onResumeApp();
    }

    public void onPause()
    {
        jni.SkipSet(1);
        if(mProc != null)
            mProc.onPause();
        bPaused = true;
        jni.measureStop();
        jni.EnginePutIt("VI_VAR_RESET",1);
        if(mVoice != null)
            mVoice.cancel();

        mVi.onPauseApp();
    }

    public void onDestroy() {
        mJni.onDestroy();
    }

    public String getStr(String id)
    {
        return getStr(id,"");
    }

    public String getStr(String id,String def)
    {
        SharedPreferences sharedPref = mApp.getPreferences(Context.MODE_PRIVATE);
        String v = new String(def);
        try {
            v = sharedPref.getString(id, def);
        } catch(ClassCastException e)
        {

        }
        return v;
    }

    public void putStr(String id,String v)
    {
        SharedPreferences sharedPref = mApp.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(id,v);
        editor.commit();
    }

    public int getInt(String id)
    {
        return getInt(id,0);
    }
    public int getInt(String id,int def)
    {
        SharedPreferences sharedPref = mApp.getPreferences(Context.MODE_PRIVATE);
        int v = def;
        try {
            v = sharedPref.getInt(id, def);
        } catch(ClassCastException e)
        {

        }
        return v;
    }

    public void putInt(String id,int v)
    {
        SharedPreferences sharedPref = mApp.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(id,v);
        editor.commit();
    }

    public float getFlt(String id)
    {
        return getFlt(id,0.0f);
    }
    public float getFlt(String id,float def)
    {
        SharedPreferences sharedPref = mApp.getPreferences(Context.MODE_PRIVATE);
        float v = def;
        try {
            v = sharedPref.getFloat(id, def);
        } catch(ClassCastException e)
        {

        }
        return v;
    }

    public void putFlt(String id,float v)
    {
        SharedPreferences sharedPref = mApp.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(id,v);
        editor.commit();
    }

    public void invalidateOptionsMenu()
    {
        mApp.invalidateOptionsMenu();
    }
    public Resources getResources()
    {
        return mApp.getResources();
    }

    public String getPackageName()
    {
        return mApp.getPackageName();
    }
    public void finish()
    {
        mApp.finish();
    }
    public  void runOnUiThread(Runnable action)
    {
        mApp.runOnUiThread(action);
    }
    public  String getString(int resId) {
        return mApp.getString(resId);
    }
    public AppCompatActivity getApp()
    {
        return mApp;
    }

    public void onPrefChanged(String key)
    {
        boolean camRestart = false;
        boolean camConfigure = false;

        if( key.equals("VI_FACE_MODE") )
            camRestart = true;

        for (String tag : FragmentCfgCamera.tags) {
            if( key.equals(tag) )
                camConfigure = true;
        }

        if(camRestart)
        {
            if(mVi != null)
                mVi.cameraRestart();
        } else
        if(camConfigure) {
            mVi.cameraConfigure();

        }
    }

    public void updateCameraSettings() {

        FragmentTransaction ft = mApp.getSupportFragmentManager().beginTransaction();

        FragmentCfgCamera fr;

        fr = (FragmentCfgCamera) mApp.getSupportFragmentManager().findFragmentById(R.id.vi_fragment_cfg_camera);
        if (fr != null) {
          fr.reload();
        }

    }


    void onVoice(String cmd) {
        if( cmd.equals("start") )
            jni.measureStart();
        else
        if( cmd.equals("stop") )
            jni.measureStop();
        else
        if( cmd.equals("reset") ) {
            jni.EnginePutIt("VI_FILTER_PAUSE",0);
            jni.EnginePutIt("VI_VAR_RESET",1);
        }
    }

    public void makeExportHT() {
        AssetManager assetManager = mApp.getAssets();
        Locale current = getResources().getConfiguration().locale;
        String lang = current.getLanguage();
        String filename = null, filename0 = null;
        String localPath = mApp.getFilesDir().getAbsolutePath();
        try {
            String[] filelist = assetManager.list("");
            for(String name:filelist) {
                if( name.endsWith("_"+lang+".html") )
                    filename = name;
                else
                if( name.endsWith("M.html"))
                    filename0 = name;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(!localPath.endsWith("/"))
            localPath += "/";

        if(filename == null)
            filename = filename0;

        if(filename == null)
            return;

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            String newFileName = localPath + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;

            jni.EnginePutStrt("VI_INFO_PATH_M_TEMPLATE_HTML",newFileName );
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    public void openResults() {
        if(canOpenResults()) {
            ResultWriter html = new ResultWriter(this);
            html.openResults();
        }
    }

    public boolean canOpenResults() {
        return ResultWriter.canOpenResults();
    }


}
