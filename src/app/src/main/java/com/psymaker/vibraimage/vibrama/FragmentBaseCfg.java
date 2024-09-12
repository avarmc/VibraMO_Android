package com.psymaker.vibraimage.vibrama;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import com.psymaker.vibraimage.vibrama.pref.PreferenceSync;
import com.psymaker.vibraimage.vibrama.pref.PreferenceSyncBase;


abstract public class FragmentBaseCfg extends PreferenceFragmentCompat  {

    public PreferenceSyncBase mSync;

    abstract public int getResourceID();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        reload();
    }

    public void reload() {
        mSync = new PreferenceSync((VibraimageActivity) getActivity(), this);

        PreferenceScreen ps = getPreferenceScreen();
        if(ps != null)
            ps.removeAll();

        // Load the preferences from an XML resource
        addPreferencesFromResource(getResourceID());
        loadState();

        mSync.initSummary(getPreferenceScreen());
    }

    protected void loadState() {}

    @Override
    public void onHiddenChanged (boolean hidden)
    {
        super.onHiddenChanged(hidden);
        if(hidden)
            mSync.stopUpdate();
        else
            mSync.startUpdate();

        Log.i("FragmentBaseCfg","onHiddenChanged "+getResourceID()+" "+hidden);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSync.startUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSync.stopUpdate();
    }



}
