package com.psymaker.vibraimage.vibrama;


import java.util.ArrayList;
import java.util.List;

public class Reg {
    private VibraimageActivityBase mApp;
    private static List<String> mIds;
    private void init()
    {
        if( mIds != null )
            return;
        mIds = new ArrayList<String>();

        mIds.add("VI_INFO_M_PERIOD");
        mIds.add("VI_INFO_M_DELAY");

        mIds.add("VI_VAR_FPSMAXF");
        mIds.add("VI_VAR_FPSMAXR");
        mIds.add("VI_VAR_FPS_PERIOD");
        mIds.add("VI_VAR_N1_RQST");
        mIds.add("VI_VAR_N0_RQST");
        mIds.add("VI_VAR_N2_RQST");
        mIds.add("VI_VAR_TH");
        mIds.add("VI_FILTER_SP");
        mIds.add("VI_FILTER_DELTA_STRETCH");
        mIds.add("VI_FILTER_AM");
        mIds.add("VI_FILTER_CT");
        mIds.add("VI_FILTER_DELTA_LO");
        mIds.add("VI_FILTER_DELTA_LOF");
        mIds.add("VI_VAR_STAT_RES_P7_LEVEL");
        mIds.add("VI_VAR_STAT_RES_P6_LEVEL");
        mIds.add("VI_VAR_STAT_RES_F5X_LEVEL");
        mIds.add("VI_FILTER_DISABLE_A");
        mIds.add("VI_FILTER_DISABLE_B");
        mIds.add("VI_FILTER_DISABLE_2X");
        mIds.add("VI_FILTER_DISABLE_VI1");
        mIds.add("VI_FILTER_DISABLE_VI2");
        mIds.add("VI_FILTER_DISABLE_FFT_UNUSED");
        mIds.add("VI_FILTER_DISABLE_ENTR");
        mIds.add("VI_FILTER_MOTION");
        mIds.add("VI_FILTER_MOTION_LEVEL");
        mIds.add("VI_FILTER_NSKIP");
        mIds.add("VI_FILTER_CONTOUR");
        mIds.add("VI_FILTER_HISTNW");
        mIds.add("VI_INFO_AVG#VI_VAR_STAT_RES_P7");
        mIds.add("VI_INFO_AVG#VI_VAR_STAT_RES_P6");
        mIds.add("VI_INFO_AVG#VI_VAR_STAT_RES_F5X");
        mIds.add("VI_INFO_AVG#VI_VAR_STATE_VAR_SRC");

        mIds.add("VI_FILTER_BLUR");

        mIds.add("VI_MODE_B");
        mIds.add("VI_FILTER_CONTOUR");
        mIds.add("VI_FACE_ENABLE");
        mIds.add("VI_FACE_DRAW");
        mIds.add("VI_FACE_MODE");

        mIds.add("VI_VAR_STATE_CRITICAL_LEV");
        mIds.add("VI_VAR_LD_MODE");
        mIds.add("VI_MODE_RESULT");
        mIds.add("VI_MODE_AURA");

        mIds.add("VI_INFO_CHQ_SET_ENABLE");
        mIds.add("VI_INFO_CHQ_SET_FPS_MAX");
        mIds.add("VI_INFO_CHQ_SET_FPS_MIN");
        mIds.add("VI_INFO_CHQ_SET_FACE_DT");
        mIds.add("VI_INFO_CHQ_SET_LEVEL_LIGHT");


        mIds.add("VI_INFO_VOICE_ENABLE");
        mIds.add("VI_INFO_VOICE_STR1");
        mIds.add("VI_INFO_VOICE_STR2");
        mIds.add("VI_INFO_VOICE_STR3");
        mIds.add("VI_INFO_VOICE_STR4");
        mIds.add("VI_INFO_VOICE_STR5");
        mIds.add("VI_INFO_VOICE_STR6");
        mIds.add("VI_INFO_VOICE_STR7");
        mIds.add("VI_INFO_VOICE_STR8");
        mIds.add("VI_INFO_VOICE_STR9");


        mIds.add("VI_INFO_M_PERIOD");
        mIds.add("VI_INFO_M_DELAY");

        mIds.add("VI_INFO_XLS_OPEN");

        mIds.add("CAM_EDGE_MODE");
        mIds.add("CAM_NOISE_REDUCTION_MODE");
        mIds.add("CAM_LENS_OPTICAL_STABILIZATION_MODE");
        mIds.add("CAM_TONEMAP_MODE");
        mIds.add("CAM_CONTROL_AF_MODE");
        mIds.add("CAM_CONTROL_AWB_MODE");
        mIds.add("CAM_CONTROL_AE_MODE");
        mIds.add("CAM_CONTROL_WB_KELVIN");


        mIds.add("CAM_EXP_TIME");
        mIds.add("CAM_LENS_FOCUS_DISTANCE");
        mIds.add("CAM_LENS_FOCUS_DISTANCE_AUTO");
        mIds.add("CAM_FLASH_MODE");
        mIds.add("CAM_CONTROL_SCENE_MODE");
        mIds.add("CAM_CONTROL_AE_EXPOSURE_COMPENSATION");
        mIds.add("CAM_CONTROL_ISO");
        mIds.add("CAM_CONTROL_AE_ANTIBANDING_MODE");
        mIds.add("CAM_CONTROL_AE_LOCK");
        mIds.add("CAM_CONTROL_AWB_LOCK");
        mIds.add("CAM_CONTROL_EFFECT_MODE");

    }

    public Reg(VibraimageActivityBase mApp)
    {
        this.mApp = mApp;
        init();
    }

    private void load(String id)
    {
        if(mApp == null)
            return;
        int type = jni.EngineGetType( jni.Tag2Id(id) );
        switch( type )
        {
            case Const.VT_INT:
                jni.EnginePutIt(id, mApp.getInt(id, jni.EngineGetIt(id)));
                break;
            case Const.VT_FLOAT:
                jni.EnginePutFt(id, mApp.getFlt(id, jni.EngineGetFt(id)));
                break;
            case Const.VT_STR:
                jni.EnginePutStrt(id, mApp.getStr(id, jni.EngineGetStrt(id)));
                break;
            default: break;
        }
    }

    private void save(String id)
    {
        if(mApp == null)
            return;
        int type = jni.EngineGetType( jni.Tag2Id(id) );
        switch( type )
        {
            case Const.VT_INT:
                mApp.putInt(id, jni.EngineGetIt(id));
                break;
            case Const.VT_FLOAT:
                mApp.putFlt(id, jni.EngineGetFt(id));
                break;
            case Const.VT_STR:
                mApp.putStr(id, jni.EngineGetStrt(id));
                break;
            default: break;
        }
    }
    public void load()
    {
        int pause = jni.EngineGetIt("VI_FILTER_PAUSE");
        jni.EnginePutIt("VI_FILTER_PAUSE",1);

        init();
        for(String id : mIds){
            load(id);
        }
        jni.EnginePutIt("VI_FILTER_PAUSE",pause);
    }

    public void save()
    {
        init();
        for(String id : mIds){
            save(id);
        }
    }
}
