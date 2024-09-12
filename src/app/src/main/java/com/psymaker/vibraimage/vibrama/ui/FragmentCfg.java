package com.psymaker.vibraimage.vibrama.ui;


import android.os.Bundle;

import androidx.annotation.Nullable;

import com.psymaker.vibraimage.vibrama.FragmentBaseCfg;
import com.psymaker.vibraimage.vibrama.med.R;

public class FragmentCfg extends FragmentBaseCfg {
    @Override
    public int getResourceID() { return R.xml.fragment_cfg; }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {

    }
}
