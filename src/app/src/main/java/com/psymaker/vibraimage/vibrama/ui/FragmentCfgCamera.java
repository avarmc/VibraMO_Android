package com.psymaker.vibraimage.vibrama.ui;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.*;
import com.psymaker.vibraimage.vibrama.ImageProc;
import com.psymaker.vibraimage.vibrama.VibraimageActivity;
import com.psymaker.vibraimage.vibrama.med.R;
import com.psymaker.vibraimage.vibrama.FragmentBaseCfg;
import com.psymaker.vibraimage.vibrama.pref.PreferenceSync;


import java.util.Properties;


public class FragmentCfgCamera extends FragmentBaseCfg {

    public static final  String tags[] = {
            "VI_FACE_MODE",
            "CAM_EDGE_MODE",
            "CAM_NOISE_REDUCTION_MODE",
            "CAM_TONEMAP_MODE",
            "CAM_CONTROL_AF_MODE",
            "CAM_CONTROL_AWB_MODE",
            "CAM_CONTROL_AE_MODE",
            "CAM_CONTROL_WB_KELVIN",
            "CAM_EXP_TIME",
            "CAM_LENS_FOCUS_DISTANCE",
            "CAM_LENS_FOCUS_DISTANCE_AUTO",
            "CAM_FLASH_MODE",
            "CAM_CONTROL_SCENE_MODE",
            "CAM_CONTROL_AE_EXPOSURE_COMPENSATION",
            "CAM_CONTROL_ISO",
            "CAM_CONTROL_AE_ANTIBANDING_MODE",
            "CAM_CONTROL_AE_LOCK",
            "CAM_CONTROL_AWB_LOCK",
            "CAM_CONTROL_EFFECT_MODE",
            "CAM_LENS_OPTICAL_STABILIZATION_MODE"
    };

    @Override
    public int getResourceID() {  return R.xml.fragment_cfg_camera;  }

    @Override
    public void onHiddenChanged (boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden)
            reload();
    }

    @Override
    protected void loadState() {
        VibraimageActivity app = (VibraimageActivity) getActivity();
        FragmentVI vi = app.getWndVI();
        Preference p;
        PreferenceScreen ps = getPreferenceScreen();
        if(vi == null || ps == null || vi.mVideo == null)
            return;



        for(int i = 0; i < tags.length; ++i) {
            String tag = tags[i];
            p = ps.findPreference(tag);
            if(p == null)
                continue;

            if( !vi.mVideo.isOption(tag) ) {
                ps.removePreference(p);
            } else
            if(p instanceof ListPreference) {
                if(!loadList(vi,(ListPreference)p,tag))
                    ps.removePreference(p);
            }
        }

    }

    protected boolean loadList(FragmentVI vi,ListPreference p,String tag) {
        CharSequence[] aEntries = p.getEntries();
        CharSequence[] aValues = p.getEntryValues();

        int[] aSupp = vi.mVideo.getSupported(tag);
        if(aSupp == null || aSupp.length == 0) {
            return false;
        }

        CharSequence[] rEntries = new CharSequence[aSupp.length];
        CharSequence[] rValues = new CharSequence[aSupp.length];


        for(int i = 0; i < aSupp.length; ++i) {
            int x = aSupp[i];
            if(aValues != null && x >=0 && x < aValues.length) {
                rValues[i] = aValues[x];
                rEntries[i] = aEntries[x];
            } else {
                rValues[i] = rEntries[i] = String.format("%d",x);
            }
        }

        p.setEntries(rEntries);
        p.setEntryValues(rValues);
        return true;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {

    }
}
