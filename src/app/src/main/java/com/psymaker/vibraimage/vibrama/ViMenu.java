package com.psymaker.vibraimage.vibrama;


import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.psymaker.vibraimage.vibrama.ui.FragmentCfg;
import com.psymaker.vibraimage.vibrama.ui.FragmentV4;
import com.psymaker.vibraimage.vibrama.ui.FragmentVI;
import com.psymaker.vibraimage.vibrama.med.R;

public class ViMenu {
    private static final String TAG = "ViMenu";

    private VibraimageActivityBase mApp;
    public ViMenu(VibraimageActivityBase mApp)
    {
        this.mApp = mApp;
    }

    public int onOptionsItemSelected(MenuItem item) {
        int ok = 1;

        int idCam = -1;
        int id = item.getItemId();
        Log.i(TAG,"onOptionsItemSelected");

        if(mApp.mVi != null) {

            switch (id) {
                case   R.id.menu_preset_idt:
                    if(!mApp.isFragmentVI()) {
                        mApp.showFragment(R.id.vi_fragment);
                        return -1;
                    }
                    break;
                case R.id.menu_mode_vi_1:
                case R.id.menu_mode_vi:
                    if (mApp.mVi != null) mApp.mVi.mProc.setModePreset(Const.VI_PRESET_VI);
                    mApp.showFragment(R.id.vi_fragment);
                    mApp.invalidateOptionsMenu();
                    break;
                case R.id.menu_mode_av_1:
                case R.id.menu_mode_av:
                    if (mApp.mVi != null) mApp.mVi.mProc.setModePreset(Const.VI_PRESET_AV);
                    mApp.showFragment(R.id.vi_fragment);
                    mApp.invalidateOptionsMenu();
                    break;
                case R.id.menu_mode_ar_1:
                case R.id.menu_mode_ar:
                    if (mApp.mVi != null) mApp.mVi.mProc.setModePreset(Const.VI_PRESET_AR);
                    mApp.showFragment(R.id.vi_fragment);
                    mApp.invalidateOptionsMenu();
                    break;
                case R.id.menu_mode_ld_1:
                case R.id.menu_mode_ld:
                    if (mApp.mVi != null) mApp.mVi.mProc.setModePreset(Const.VI_PRESET_LD);
                    mApp.showFragment(R.id.vi_fragment);
                    mApp.invalidateOptionsMenu();
                    break;
                case R.id.menu_view_v4_1:
                case R.id.menu_view_v4:
                    mApp.showFragment(R.id.vi_fragment_v4);
                    break;
                case R.id.menu_view_cfg_1:
                case R.id.menu_view_cfg:
                    mApp.showFragment(R.id.vi_fragment_cfg);
                    break;
                case R.id.menu_view_vi_1:
                case R.id.menu_view_vi:
                    mApp.showFragment(R.id.vi_fragment);
                    break;
                case R.id.menu_camera_settings:
                    mApp.showFragment(R.id.vi_fragment_cfg_camera);
                    mApp.updateCameraSettings();
                    break;
                case R.id.menu_action_measure_1:
                case R.id.menu_action_measure:
                    mApp.mProc.InitDB();
                    mApp.makeExportHT();
                    jni.measureStart();
                    SeqMeasure.onMeasure(mApp);
                    break;
                case R.id.menu_action_reset_1:
                case R.id.menu_action_reset:
                    if (mApp.mVi != null)
                    {
                        jni.EnginePutIt("VI_FILTER_PAUSE",0);
                        jni.EnginePutIt("VI_VAR_RESET",1);

                    }
                    break;
                case R.id.menu_view_web_1:
                case R.id.menu_view_web:
                    mApp.openResults();
                    break;
                case R.id.menu_defaults_micro:
                    jni.Defaults(0);
                    break;
                case R.id.menu_camera0: idCam = 0; break;
                case R.id.menu_camera1: idCam = 1; break;
                case R.id.menu_camera2: idCam = 2; break;
                case R.id.menu_camera3: idCam = 3; break;
                case R.id.menu_camera4: idCam = 4; break;
                case R.id.menu_camera5: idCam = 5; break;
                case R.id.menu_camera6: idCam = 6; break;
                case R.id.menu_camera7: idCam = 7; break;
                case R.id.menu_camera8: idCam = 8; break;
                case R.id.menu_camera9: idCam = 9; break;

                case R.id.menu_unregister: {
                    SeqMeasure  mSeq = new SeqMeasure(mApp);
                    mSeq.Unregister();
                    break;
                }

                default:
                 //   ok = false;
                    break;
            }
        }

        if(item.hasSubMenu())
        {
            mApp.onPrepareOptionsMenu(item.getSubMenu());
            ok = 1;
        }

        if( idCam >= 0 && mApp.mVi.mVideo != null) {
            mApp.mVi.onCamera(idCam);
            mApp.showFragment(R.id.vi_fragment);
        }

        return ok;
    }

    public boolean onPrepareOptionsMenu( Menu menu) {

        if(mApp.mVi != null) {
            int size = menu.size();
            for (int i = 0; i < size; ++i) {
                MenuItem itm = menu.getItem(i);
                if(itm.hasSubMenu()) {
                    SubMenu sub = itm.getSubMenu();
                    onPrepareOptionsMenu(sub);
                }
                onPrepareOptionsMenuItem( itm );
            }
        }
        return true;
    }

    protected int getPresetIcon()
    {
        switch(mApp.mVi.mProc.getModePerset())
        {
            case Const.VI_PRESET_VI:
                return R.drawable.vi;
            case Const.VI_PRESET_AV:
                return R.drawable.av;
            case Const.VI_PRESET_AR:
                return R.drawable.ar;
            case Const.VI_PRESET_LD:
                return R.drawable.ld;
            default: break;
        }
        return R.drawable.vi;
    }

    protected void onPrepareOptionsMenuItem(MenuItem itm)
    {
        int id = itm.getItemId();
        int idCam = -1;

        switch( id )
        {
            case R.id.menu_preset_idt:
                itm.setIcon( getPresetIcon() );
                break;
            case R.id.menu_mode_vi_1:
            case R.id.menu_mode_vi:
                itm.setChecked(mApp.mVi.mProc.getModePerset() == Const.VI_PRESET_VI);
                break;
            case R.id.menu_mode_av_1:
            case R.id.menu_mode_av:
                itm.setChecked(mApp.mVi.mProc.getModePerset() == Const.VI_PRESET_AV);
                break;
            case R.id.menu_mode_ar_1:
            case R.id.menu_mode_ar:
                itm.setChecked(mApp.mVi.mProc.getModePerset() == Const.VI_PRESET_AR);
                break;
            case R.id.menu_mode_ld_1:
            case R.id.menu_mode_ld:
                itm.setChecked(mApp.mVi.mProc.getModePerset() == Const.VI_PRESET_LD);
                break;
            case R.id.menu_view_v4_1:
            case R.id.menu_view_v4:
                itm.setChecked(mApp.mCurrentFragment != null && mApp.mCurrentFragment instanceof FragmentV4);
                break;
            case R.id.menu_view_cfg_1:
            case R.id.menu_view_cfg:
                itm.setChecked(mApp.mCurrentFragment != null && mApp.mCurrentFragment instanceof FragmentCfg);
                break;
            case R.id.menu_view_vi_1:
            case R.id.menu_view_vi:
                itm.setChecked(mApp.mCurrentFragment != null && mApp.mCurrentFragment instanceof FragmentVI);
                break;
            case R.id.menu_action_measure_1:
            case R.id.menu_action_measure:
            {
                float progress = jni.EngineGetFt("VI_INFO_M_PROGRESS");
                itm.setChecked(progress > 0);
            }
            break;
            case R.id.menu_view_web_1:
            case R.id.menu_view_web:
            {
                itm.setEnabled( mApp.canOpenResults() );
            }
            break;

            case R.id.menu_camera0: idCam = 0; break;
            case R.id.menu_camera1: idCam = 1; break;
            case R.id.menu_camera2: idCam = 2; break;
            case R.id.menu_camera3: idCam = 3; break;
            case R.id.menu_camera4: idCam = 4; break;
            case R.id.menu_camera5: idCam = 5; break;
            case R.id.menu_camera6: idCam = 6; break;
            case R.id.menu_camera7: idCam = 7; break;
            case R.id.menu_camera8: idCam = 8; break;
            case R.id.menu_camera9: idCam = 9; break;


            default: break;
        }

        if(  mApp.mVi != null && mApp.mVi.mVideo != null && mApp.mVi.mVideo.mCameras != null && idCam >= 0)
        {

            if( idCam < mApp.mVi.mVideo.mCameras.length ) {
                String sid = mApp.mVi.getCameraID(idCam);
                itm.setChecked(mApp.mVi.getCameraID().equals(sid));
                if(  sid.equals("0") )
                    itm.setTitle(R.string.camera_back);
                else
                if(  sid.equals("1") )
                    itm.setTitle(R.string.camera_front);
                else
                    itm.setTitle(sid);
                itm.setVisible(true);
            } else
                itm.setVisible(false);
        }

    }


}
