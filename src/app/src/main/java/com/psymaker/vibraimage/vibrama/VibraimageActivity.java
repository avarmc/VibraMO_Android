package com.psymaker.vibraimage.vibrama;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.psymaker.vibraimage.vibrama.ui.FragmentVI;

import java.io.File;
import java.util.Map;

/**
 * com.psymaker.vibraimage.vibrama.VibraimageActivity
 * Activity displaying a fragment that implements RAW photo captures.
 */
public class VibraimageActivity extends AppCompatActivity
{

    public VibraimageActivityBase base = new VibraimageActivityBase(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        String ABI = Build.SUPPORTED_ABIS[0];
        Log.i("com.psymaker.vibraimage.vibrama","ABI="+ABI);
        base.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return base.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int nRet = base.onOptionsItemSelected(item);
        if(nRet == 0)
            return super.onOptionsItemSelected(item);
        return (nRet == 1);
    }

    @Override
    public void onBackPressed() {
        if(!base.onBackPressed())
            super.onBackPressed();
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if( base.onPrepareOptionsMenu(menu) )
            return true;
        return super.onPrepareOptionsMenu(menu);
    }



    public String getAppDataDir()  {
        return base.getAppDataDir();
    }

    public ImageProc getImageProc()
    {
        return base.getImageProc();
    }

    public FragmentVI getWndVI()
    {
        return base.getWndVI();
    }


    @Override
    public void onResume()
    {
        super.onResume();


        base.onResume();
    }

    @Override
    public void onPause()
    {
        base.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        base.onDestroy();
        super.onDestroy();
    }





}
