package com.psymaker.vibraimage.vibrama.pref;

import android.content.res.TypedArray;
import android.os.Handler;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.*;

import com.psymaker.vibraimage.vibrama.ImageProc;
import com.psymaker.vibraimage.vibrama.VibraimageActivity;
import com.psymaker.vibraimage.vibrama.jni;
import com.psymaker.vibraimage.vibrama.med.R;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by user on 28.06.2017.
 */

public class PreferenceSync extends PreferenceSyncBase {

    VibraimageActivity mApp;
    ImageProc mProc;
    PreferenceFragmentCompat mFragment;


    private TreeMap<String,Integer> mVer = new TreeMap<String,Integer>();


    Handler hUpdate = new Handler();
    Runnable runUpdate = new Runnable() {

        @Override
        public void run() {
            updateVer();
            hUpdate.postDelayed(this, 40);
        }
    };


    public PreferenceSync(VibraimageActivity mApp,PreferenceFragmentCompat mFragment)
    {
        this.mApp = mApp; // (VibraimageActivity)getActivity();
        this.mFragment = mFragment;
        mProc = mApp.getImageProc();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        updatePrefSummary(preference,true,newValue);
        return true;
    }

    @Override
    public void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            String tag = p.getKey();

            mProc.AddTag(tag);

            mVer.put(tag,-1);
            p.setOnPreferenceChangeListener(this);;
            updatePrefSummary(p,false,null);
        }
    }

    private String prefGetDst(Preference p) {

        if (p instanceof PreferenceList) {
            PreferenceList pref = (PreferenceList) p;
            return pref.mDst;
        }else
        if (p instanceof PreferenceInt) {
            PreferenceInt pref = (PreferenceInt) p;
            return pref.mDst;
        }else
        if (p instanceof PreferenceFloat) {
            PreferenceFloat pref = (PreferenceFloat) p;
            return pref.mDst;
        } else
         if (p instanceof PreferenceString) {
             PreferenceString pref = (PreferenceString) p;
            return pref.mDst;
        }  if (p instanceof PreferenceBool) {
            PreferenceBool pref = (PreferenceBool) p;
            return pref.mDst;
        }

        return "";
    }

    private boolean isEngine(Preference p) {
        return !prefGetDst(p).equals("cam");
    }

    private boolean isCam(Preference p) {
        return prefGetDst(p).equals("cam");
    }



    private boolean updatePrefSummary(Preference p, boolean bSet2Engine, Object newValue) {

        boolean bCam = isCam(p);

        String ret = null;
        String key = p.getKey();

        if (p instanceof PreferenceList) {
            PreferenceList pref = (PreferenceList) p;

            if(bSet2Engine) {
                ret = (newValue == null) ? pref.getValue() : newValue.toString();
                if(ret != null && !ret.equals(""))
                    jni.EnginePutIt(key, Integer.parseInt(ret));
                mApp.base.onPrefChanged(key);
            } else
            {
                pref.setValue( ""+jni.EngineGetIt(key) );
                ret = pref.getValue();
                p.setSummary( pref.getEntry() );
            }
        } else
        if (p instanceof PreferenceInt) {
            PreferenceInt pref = (PreferenceInt) p;

            if(bSet2Engine) {
                ret = (newValue == null) ? pref.getText() : newValue.toString();
                int v = (ret == null || ret.equals("")) ? 0:Integer.parseInt(ret);
                jni.EnginePutIt(key, v);
                mApp.base.onPrefChanged(key);
            } else
            {
                int v = 0;
                v = jni.EngineGetIt(key);
                pref.setText(""+v);
                ret = pref.getText();
                    pref.setSummaryValue(v);
            }
        } else
        if (p instanceof PreferenceFloat) {
            PreferenceFloat pref = (PreferenceFloat) p;

            if(bSet2Engine) {
                ret = (newValue == null) ? pref.getText() : newValue.toString();
                float v = (ret ==null || ret.equals("")) ? 0 : Float.parseFloat(ret);
                jni.EnginePutFt(key, v);
                mApp.base.onPrefChanged(key);
            } else
            {
                float v = 0;
                v = jni.EngineGetFt(key);
                pref.setText(""+v);
                ret = pref.getText();
                pref.setSummaryValue(v);
            }
        } else
        if (p instanceof PreferenceString) {
            PreferenceString pref = (PreferenceString) p;

            if(bSet2Engine) {
                ret = (newValue == null) ? pref.getText() : newValue.toString();
                jni.EnginePutStrt(key, ret);
                mApp.base.onPrefChanged(key);
            } else
            {
                String v = "";
                v = jni.EngineGetStrt(key);
                pref.setText(""+v);
                ret = pref.getText();
                pref.setSummaryValue(v);
            }
        } else
        if (p instanceof PreferenceBool) {
            PreferenceBool pref = (PreferenceBool) p;

            ret = (newValue == null) ? ""+pref.isChecked() : newValue.toString();

            if(bSet2Engine) {
                jni.EnginePutIt(key, (ret != null && ret.equals("true")) ? 1 : 0);
                mApp.base.onPrefChanged(key);
            } else
            {
                pref.setChecked( jni.EngineGetIt(key) != 0 );
            }
        }
        return true;
    }

    @Override
    public void startUpdate()
    {
        hUpdate.postDelayed(runUpdate, 1);
    }

    @Override
    public void stopUpdate()
    {
        hUpdate.removeCallbacks(runUpdate);
    }

    public void updateVer() {
        Set set = mVer.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry a = (Map.Entry) iterator.next();
            String tag = (String)a.getKey();
            int verC = (Integer) a.getValue();
            int verE = jni.EngineGetVert(tag);
            if(verC != verE)
            {
                Preference pref = mFragment.findPreference(tag);
                if(pref != null)
                    updatePrefSummary(pref,false,null);
                mVer.put(tag,verE);
            }
        }
    }

}
